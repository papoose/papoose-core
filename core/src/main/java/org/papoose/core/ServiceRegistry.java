/**
 *
 * Copyright 2008-2009 (C) The original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.papoose.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.papoose.core.filter.Expr;
import org.papoose.core.util.ClassUtils;
import org.papoose.core.util.SecurityUtils;


/**
 * @version $Revision$ $Date$
 */
class ServiceRegistry
{
    private final static String CLASS_NAME = ServiceRegistry.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Object lock = new Object();
    private final Map<Long, ServiceEntry> serviceEntries = new HashMap<Long, ServiceEntry>();
    private final ThreadLocal<ServiceEntry> removing = new ThreadLocal<ServiceEntry>();
    private final Papoose framework;
    private long lastServiceId;

    ServiceRegistry(Papoose framework)
    {
        assert framework != null;

        this.framework = framework;
    }

    public void start()
    {
        lastServiceId = 0;
    }

    public void stop()
    {
    }

    public void validate(long serviceId)
    {
        synchronized (lock)
        {
            if (!serviceEntries.containsKey(serviceId)) throw new IllegalStateException("Service removed");
        }
    }

    public Bundle getBundle(long serviceId)
    {
        synchronized (lock)
        {
            ServiceEntry entry = serviceEntries.get(serviceId);

            if (entry != null)
            {
                return entry.getBundle();
            }
            else
            {
                return null;
            }
        }
    }

    public Bundle[] getUsingBundles(long serviceId)
    {
        synchronized (lock)
        {
            Map<BundleController, BundleServiceReference> map = serviceEntries.get(serviceId).getUsingBundles();

            if (map.isEmpty())
            {
                return null;
            }
            else
            {
                return map.keySet().toArray(new Bundle[map.keySet().size()]);
            }
        }
    }

    public boolean isAssignableTo(long serviceId, Bundle thatBundle, String className)
    {
        if (!(thatBundle instanceof BundleController)) return false;

        BundleController requestingBundleController = (BundleController) thatBundle;

        if (requestingBundleController.getFramework() != framework) return false;

        String packageName = className.substring(0, Math.max(0, className.lastIndexOf('.')));

        if (packageName.startsWith("java.")) return true;

        for (String delegate : framework.getBootDelegates())
        {
            if ((delegate.endsWith(".") && packageName.regionMatches(0, delegate, 0, delegate.length() - 1)) || packageName.equals(delegate))
            {
                return true;
            }
        }

        synchronized (lock)
        {
            ServiceEntry serviceEntry = serviceEntries.get(serviceId);
            if (serviceEntry == null) serviceEntry = removing.get();
            BundleController registeringBundleController = serviceEntry.getBundle();

            if (registeringBundleController == requestingBundleController) return true;

            BundleClassLoader requestingClassLoader = obtainSource(requestingBundleController, packageName);

            BundleClassLoader registeringClassLoader = obtainSource(registeringBundleController, packageName);

            if (requestingClassLoader == null && registeringClassLoader == null)
            {
                try
                {
                    BundleClassLoader reqbcl = obtainClassLoader(requestingBundleController);
                    if (reqbcl == null) return true;
                    Class reqc = reqbcl.delegateLoadClass(className);

                    BundleClassLoader regbcl = obtainClassLoader(registeringBundleController);
                    if (regbcl == null) return true;
                    Class regc = regbcl.delegateLoadClass(className);

                    return reqc == regc;
                }
                catch (ClassNotFoundException e)
                {
                    return true;
                }
            }
            else if (requestingClassLoader == null)
            {
                if (obtainClassLoader(requestingBundleController) == registeringClassLoader)
                {
                    return true;
                }
                else
                {
                    try
                    {
                        BundleClassLoader reqbcl = obtainClassLoader(requestingBundleController);
                        Class reqc = reqbcl.delegateLoadClass(className);
                        try
                        {
                            BundleClassLoader regbcl = obtainClassLoader(registeringBundleController);
                            Class regc = regbcl.delegateLoadClass(className);

                            return reqc == regc;
                        }
                        catch (ClassNotFoundException e)
                        {
                            return false;
                        }
                    }
                    catch (ClassNotFoundException e)
                    {
                        return true;
                    }
                }
            }
            else if (registeringClassLoader == null)
            {
                if (requestingClassLoader == obtainClassLoader(registeringBundleController))
                {
                    return true;
                }
                else
                {
                    try
                    {
                        BundleClassLoader regbcl = obtainClassLoader(registeringBundleController);
                        Class regc = regbcl.delegateLoadClass(className);
                        try
                        {
                            Class reqc = requestingClassLoader.delegateLoadClass(className);

                            return reqc == regc;
                        }
                        catch (ClassNotFoundException e)
                        {
                            return false;
                        }
                    }
                    catch (ClassNotFoundException e)
                    {
                        return true;
                    }
                }
            }
            else
            {
                return requestingClassLoader == registeringClassLoader;
            }
        }
    }

    private static BundleClassLoader obtainSource(BundleController bundle, String packageName)
    {
        BundleClassLoader bundleClassLoader = obtainClassLoader(bundle);

        if (bundleClassLoader == null) return null;

        for (Wire wire : bundleClassLoader.getWires())
        {
            for (String exportPackage : wire.getExportDescription().getPackageNames())
            {
                if (exportPackage.equals(packageName)) return wire.getBundleClassLoader();
            }
        }

        return null;
    }

    private static BundleClassLoader obtainClassLoader(BundleController bundle)
    {
        if (bundle.getState() == Bundle.INSTALLED) bundle.getFramework().getBundleManager().resolve(bundle);

        Generation generation = bundle.getCurrentGeneration();
        BundleGeneration bundleGeneration;

        if (generation instanceof BundleGeneration)
        {
            bundleGeneration = (BundleGeneration) generation;
        }
        else if (generation instanceof FragmentGeneration)
        {
            FragmentGeneration fragmentGeneration = (FragmentGeneration) generation;
            bundleGeneration = fragmentGeneration.getHost();
        }
        else
        {
            return null;
        }

        return bundleGeneration.getClassLoader();
    }

    @SuppressWarnings({ "unchecked" })
    public ServiceRegistration registerService(BundleController bundleController, String[] clazzes, Object service, Dictionary properties)
    {
        LOGGER.entering(CLASS_NAME, "registerService", new Object[]{ bundleController, clazzes, service, properties });

        for (String clazz : clazzes)
        {
            SecurityUtils.checkServicePermission(clazz, ServicePermission.REGISTER);
        }

        checkClasses(service, clazzes);

        ServiceRegistrationImpl serviceRegistration;

        synchronized (lock)
        {
            long serviceId = ++lastServiceId;

            String[] c = new String[clazzes.length];
            System.arraycopy(clazzes, 0, c, 0, c.length);

            Dictionary<String, Object> p = new Hashtable<String, Object>();
            if (properties != null)
            {
                Enumeration<String> enumeration = properties.keys();
                while (enumeration.hasMoreElements())
                {
                    String key = enumeration.nextElement();
                    p.put(key, properties.get(key));
                }
            }
            p.put(Constants.OBJECTCLASS, c);
            p.put(Constants.SERVICE_ID, serviceId);

            if (p.get(Constants.SERVICE_RANKING) == null)
            {
                p.put(Constants.SERVICE_RANKING, 0);
            }
            else
            {
                try
                {
                    p.put(Constants.SERVICE_RANKING, Integer.valueOf(p.get(Constants.SERVICE_RANKING).toString()));
                }
                catch (NumberFormatException e)
                {
                    LOGGER.warning("Bad value for service.ranking " + p.get(Constants.SERVICE_RANKING));
                    p.put(Constants.SERVICE_RANKING, 0);
                }
            }

            serviceRegistration = new ServiceRegistrationImpl(p, this);

            serviceEntries.put(serviceId, new ServiceEntry(serviceRegistration, bundleController, service));
        }

        framework.getBundleManager().fireServiceEvent(new ServiceEvent(ServiceEvent.REGISTERED, serviceRegistration.getReference()));

        LOGGER.exiting(CLASS_NAME, "registerService", serviceRegistration);

        return serviceRegistration;
    }

    public ServiceReference[] getServiceReferences(BundleController bundleController, String clazz, String filterString) throws InvalidSyntaxException
    {
        synchronized (lock)
        {
            DefaultFilter filter = (filterString == null ? null : new DefaultFilter(framework.getParser().parse(filterString)));
            List<ServiceReference> result = new ArrayList<ServiceReference>();

            for (ServiceEntry entry : serviceEntries.values())
            {
                ServiceRegistrationImpl registration = entry.getRegistration();
                ServiceRegistrationImpl.ServiceReferenceImpl reference = (ServiceRegistrationImpl.ServiceReferenceImpl) registration.getReference();
                Dictionary<String, Object> p = reference.getProperties();

                boolean found = true;
                if (clazz != null)
                {
                    found = false;
                    for (String objectClass : (String[]) p.get(Constants.OBJECTCLASS))
                    {
                        if (objectClass.equals(clazz))
                        {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) continue;

                boolean secure = true;
                for (String objectClass : (String[]) p.get(Constants.OBJECTCLASS))
                {
                    try
                    {
                        SecurityUtils.checkServicePermission(objectClass, ServicePermission.GET);
                    }
                    catch (SecurityException e)
                    {
                        secure = false;
                        break;
                    }
                }
                if (!secure) continue;

                boolean assignable = true;
                for (String objectClass : (String[]) p.get(Constants.OBJECTCLASS))
                {
                    if (!reference.isAssignableTo(bundleController, objectClass))
                    {
                        assignable = false;
                        break;
                    }
                }
                if (!assignable) continue;

                if (filter != null && !filter.match(p)) continue;

                result.add(reference);
            }

            return sortedReferences(result);
        }
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        synchronized (lock)
        {
            Expr expr = (filter == null ? null : framework.getParser().parse(filter));
            List<ServiceReference> result = new ArrayList<ServiceReference>();

            for (ServiceEntry entry : serviceEntries.values())
            {
                ServiceRegistrationImpl registration = entry.getRegistration();
                ServiceRegistrationImpl.ServiceReferenceImpl reference = (ServiceRegistrationImpl.ServiceReferenceImpl) registration.getReference();
                Dictionary<String, Object> p = reference.getProperties();

                boolean found = true;
                if (clazz != null)
                {
                    found = false;
                    for (String objectClass : (String[]) p.get(Constants.OBJECTCLASS))
                    {
                        if (objectClass.equals(clazz))
                        {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) continue;

                boolean secure = true;
                for (String objectClass : (String[]) p.get(Constants.OBJECTCLASS))
                {
                    try
                    {
                        SecurityUtils.checkServicePermission(objectClass, ServicePermission.GET);
                    }
                    catch (SecurityException e)
                    {
                        secure = false;
                        break;
                    }
                }
                if (!secure) continue;

                if (expr != null && !expr.match(p)) continue;

                result.add(reference);
            }

            return sortedReferences(result);
        }
    }

    private static ServiceReference[] sortedReferences(List<ServiceReference> references)
    {
        if (references.isEmpty()) return null;

        SortedSet<ServiceReference> sorted = new TreeSet<ServiceReference>(new Comparator<ServiceReference>()
        {
            public int compare(ServiceReference o1, ServiceReference o2)
            {
                Integer ranking1 = (Integer) o1.getProperty(Constants.SERVICE_RANKING);
                Integer ranking2 = (Integer) o2.getProperty(Constants.SERVICE_RANKING);

                int result = -ranking1.compareTo(ranking2);

                if (result != 0) return result;

                Long id1 = (Long) o1.getProperty(Constants.SERVICE_ID);
                Long id2 = (Long) o2.getProperty(Constants.SERVICE_ID);

                return id1.compareTo(id2);
            }
        });
        sorted.addAll(references);

        return sorted.toArray(new ServiceReference[sorted.size()]);
    }

    public ServiceReference getServiceReference(BundleController bundleController, String clazz)
    {
        synchronized (lock)
        {
            try
            {
                ServiceReference[] references = getServiceReferences(bundleController, clazz, null);

                if (references == null) return null;

                return references[0];
            }
            catch (InvalidSyntaxException neverHappens)
            {
                return null;
            }
        }
    }

    public Object getService(BundleController bundleController, ServiceReference sr)
    {
        if (!(sr instanceof ServiceRegistrationImpl.ServiceReferenceImpl)) return null;

        ServiceRegistrationImpl.ServiceReferenceImpl serviceReference = (ServiceRegistrationImpl.ServiceReferenceImpl) sr;
        SecurityException se = null;
        for (String objectClass : (String[]) serviceReference.getProperty(Constants.OBJECTCLASS))
        {
            try
            {
                SecurityUtils.checkServicePermission(objectClass, ServicePermission.GET);
                break;
            }
            catch (SecurityException e)
            {
                se = e;
            }
        }
        if (se != null) throw se;

        ServiceEntry entry;
        BundleServiceReference reference;
        synchronized (lock)
        {
            //noinspection SuspiciousMethodCalls
            entry = serviceEntries.get(serviceReference.getServiceId());

            if (entry == null) return null;

            reference = entry.getUsingBundles().get(bundleController);
            if (reference == null) entry.getUsingBundles().put(bundleController, reference = new BundleServiceReference());

            reference.increment();
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (reference)
        {
            Object service = entry.getService();
            if (service instanceof ServiceFactory)
            {
                if (reference.getCount() > 1) return reference.getService();

                try
                {
                    ServiceFactory factory = (ServiceFactory) service;
                    service = factory.getService(bundleController, entry.getRegistration());

                    checkClasses(service, (String[]) sr.getProperty(Constants.OBJECTCLASS));

                    reference.setService(service);
                }
                catch (Throwable t)
                {
                    framework.getBundleManager().fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundleController, t));
                    service = null;
                }
            }
            return service;
        }
    }

    public boolean ungetService(BundleController bundleController, ServiceReference serviceReference)
    {
        synchronized (lock)
        {
            @SuppressWarnings({ "SuspiciousMethodCalls" })
            ServiceEntry entry = serviceEntries.get(serviceReference.getProperty(Constants.SERVICE_ID));

            if (entry == null) return false;

            BundleServiceReference reference = entry.getUsingBundles().get(bundleController);

            if (reference == null) return false;

            reference.decrement();

            if (reference.getCount() == 0)
            {
                Object service = reference.getService();
                if (service != null)
                {
                    ServiceFactory factory = (ServiceFactory) entry.getService();
                    factory.ungetService(bundleController, entry.getRegistration(), service);
                }
                entry.getUsingBundles().remove(bundleController);
            }

            return true;
        }
    }

    public void ungetService(BundleController bundleController)
    {
        synchronized (lock)
        {
            for (ServiceEntry entry : serviceEntries.values())
            {
                BundleServiceReference reference = entry.getUsingBundles().get(bundleController);

                if (reference == null) continue;

                Object service = reference.getService();
                if (service != null)
                {
                    ServiceFactory factory = (ServiceFactory) entry.getService();
                    factory.ungetService(bundleController, entry.getRegistration(), service);
                }

                entry.getUsingBundles().remove(bundleController);
            }
        }
    }

    public void setProperties(long serviceId, Dictionary old)
    {
        ServiceEntry entry;
        synchronized (lock)
        {
            entry = serviceEntries.get(serviceId);

            if (entry == null) return;
        }

        ServiceRegistrationImpl serviceRegistration = entry.getRegistration();

        framework.getBundleManager().fireModifiedServiceEvent(serviceRegistration.getReference(), old);
    }

    public void unregister(long serviceId)
    {
        ServiceEntry oldEntry;
        ServiceEntry entry;

        synchronized (lock)
        {
            oldEntry = removing.get();
            removing.set(entry = serviceEntries.get(serviceId));

            if (entry == null) throw new IllegalStateException("Service has already be unregistered");

            serviceEntries.remove(serviceId);
        }

        try
        {
            Map<BundleController, BundleServiceReference> bundles = entry.getUsingBundles();
            ServiceRegistrationImpl serviceRegistration = entry.getRegistration();
            framework.getBundleManager().fireServiceEvent(new ServiceEvent(ServiceEvent.UNREGISTERING, serviceRegistration.getReference()));

            for (BundleController bundle : bundles.keySet())
            {
                BundleServiceReference bsr = bundles.get(bundle);
                Object service = bsr.getService();
                if (service != null)
                {
                    ServiceFactory factory = (ServiceFactory) entry.getService();
                    factory.ungetService(bundle, entry.getRegistration(), service);
                }
            }
        }
        finally
        {
            removing.set(oldEntry);
        }
    }


    public void unregister(BundleController bundleController)
    {
        Set<ServiceEntry> entries = new HashSet<ServiceEntry>();

        synchronized (lock)
        {
            for (ServiceEntry entry : serviceEntries.values())
            {
                if (entry.getBundle() == bundleController)
                {
                    entries.add(entry);
                }
            }

            for (ServiceEntry entry : entries)
            {
                unregister((Long) entry.getRegistration().getReference().getProperty(Constants.SERVICE_ID));
            }
        }
    }

    public void lock(long serviceId)
    {
        synchronized (lock)
        {
            //Todo: change body of created methods use File | Settings | File Templates.
        }
    }

    public void unlock(long serviceId)
    {
        synchronized (lock)
        {
            //Todo: change body of created methods use File | Settings | File Templates.
        }
    }

    public static void checkClasses(Object service, String[] clazzes)
    {
        Set<Class> classes = ClassUtils.getAllInterfacesAsSet(service);
        if (!classes.contains(ServiceFactory.class))
        {
            for (String clazz : clazzes)
            {
                boolean found = false;

                if (clazz.equals(service.getClass().getName()))
                {
                    found = true;
                }
                else
                {
                    for (Class cls : classes)
                    {
                        if (clazz.equals(cls.getName()))
                        {
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) throw new IllegalArgumentException();
            }
        }
    }

    private static class ServiceEntry
    {
        private final ServiceRegistrationImpl registration;
        private final BundleController bundle;
        private final Object service;
        private final Map<BundleController, BundleServiceReference> usingBundles = new HashMap<BundleController, BundleServiceReference>();

        private ServiceEntry(ServiceRegistrationImpl registration, BundleController bundle, Object service)
        {
            assert registration != null;
            assert bundle != null;
            assert service != null;

            this.registration = registration;
            this.bundle = bundle;
            this.service = service;
        }

        public ServiceRegistrationImpl getRegistration()
        {
            return registration;
        }

        public BundleController getBundle()
        {
            return bundle;
        }

        public Object getService()
        {
            return service;
        }

        public Map<BundleController, BundleServiceReference> getUsingBundles()
        {
            return usingBundles;
        }

        @Override
        public String toString()
        {
            return Arrays.toString((String[]) registration.getReference().getProperty(Constants.OBJECTCLASS));
        }
    }

    private static class BundleServiceReference
    {
        private int count;
        private Object service;

        public void increment()
        {
            count++;
        }

        public void decrement()
        {
            count--;
        }

        public int getCount()
        {
            return count;
        }

        public Object getService()
        {
            return service;
        }

        public void setService(Object service)
        {
            this.service = service;
        }
    }
}
