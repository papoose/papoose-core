/**
 *
 * Copyright 2009 (C) The original author or authors
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.AdminPermission;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;

import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.spi.BundleStore;
import org.papoose.core.util.AttributeUtils;
import org.papoose.core.util.I18nUtils;
import org.papoose.core.util.SecurityUtils;
import org.papoose.core.util.SerialExecutor;
import org.papoose.core.util.ToStringCreator;


/**
 * @version $Revision$ $Date$
 */
public class BundleController implements Bundle
{
    private final static String CLASS_NAME = BundleController.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Set<BundleListener> bundleListeners = new CopyOnWriteArraySet<BundleListener>();
    private final Set<SynchronousBundleListener> syncBundleListeners = new CopyOnWriteArraySet<SynchronousBundleListener>();
    private final Set<FrameworkListener> frameworkListeners = new CopyOnWriteArraySet<FrameworkListener>();
    private final Set<ServiceListener> serviceListeners = new CopyOnWriteArraySet<ServiceListener>();
    private final Set<ServiceListener> allServiceListeners = new CopyOnWriteArraySet<ServiceListener>();
    private final Papoose framework;
    private final BundleStore bundleStore;
    private final Executor serialExecutor;
    private final Map<Integer, Generation> generations = new HashMap<Integer, Generation>();
    private volatile BundleContextImpl bundleContext;
    private volatile Generation currentGeneration;

    public BundleController(Papoose framework, BundleStore bundleStore)
    {
        assert framework != null;
        assert bundleStore != null;

        this.framework = framework;
        this.bundleStore = bundleStore;
        this.serialExecutor = new SerialExecutor(framework.getExecutorService());
    }

    Set<BundleListener> getBundleListeners()
    {
        return bundleListeners;
    }

    Set<SynchronousBundleListener> getSyncBundleListeners()
    {
        return syncBundleListeners;
    }

    Set<FrameworkListener> getFrameworkListeners()
    {
        return frameworkListeners;
    }

    Set<ServiceListener> getServiceListeners()
    {
        return serviceListeners;
    }

    Set<ServiceListener> getAllServiceListeners()
    {
        return allServiceListeners;
    }

    Papoose getFramework()
    {
        return framework;
    }

    BundleStore getBundleStore()
    {
        return bundleStore;
    }

    Executor getSerialExecutor()
    {
        return serialExecutor;
    }

    Map<Integer, Generation> getGenerations()
    {
        return generations;
    }

    Generation getCurrentGeneration()
    {
        return currentGeneration;
    }

    void setCurrentGeneration(Generation currentGeneration)
    {
        this.currentGeneration = currentGeneration;
    }

    /**
     * {@inheritDoc}
     */
    public int getState()
    {
        return getCurrentGeneration().getState();
    }

    /**
     * {@inheritDoc}
     */
    public void start(int options) throws BundleException
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.EXECUTE);

        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        //Todo change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws BundleException
    {
        start(0);
    }

    /**
     * {@inheritDoc}
     */
    public void stop(int options) throws BundleException
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.EXECUTE);

        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        //Todo change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws BundleException
    {
        stop(0);
    }

    /**
     * {@inheritDoc}
     */
    public void update() throws BundleException
    {
        //Todo change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public void update(InputStream inputStream) throws BundleException
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.LIFECYCLE);

        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        //Todo change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public void uninstall() throws BundleException
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.LIFECYCLE);

        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        Papoose framework = getFramework();
        BundleManager bundleManager = framework.getBundleManager();
    }

    /**
     * {@inheritDoc}
     */
    public Dictionary getHeaders()
    {
        return getHeaders(null);
    }

    /**
     * {@inheritDoc}
     */
    public long getBundleId()
    {
        return bundleStore.getBundleId();
    }

    /**
     * {@inheritDoc}
     */
    public String getLocation()
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.METADATA);

        return bundleStore.getLocation();
    }

    /**
     * {@inheritDoc}
     */
    public ServiceReference[] getRegisteredServices()
    {
        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        return new ServiceReference[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public ServiceReference[] getServicesInUse()
    {
        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        return new ServiceReference[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPermission(Object object)
    {
        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        Generation bundleGeneration = obtainLatestResolvedBundle();

        return bundleGeneration.hasPermission(object);
    }

    /**
     * {@inheritDoc}
     */
    public URL getResource(String name)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(this, AdminPermission.RESOURCE));

        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        Generation bundleGeneration = obtainLatestResolvedBundle();

        return bundleGeneration.getResource(name);
    }

    /**
     * {@inheritDoc}
     */
    public Dictionary getHeaders(String locale)
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(this, AdminPermission.METADATA));

        ArchiveStore archiveStore = getCurrentGeneration().getArchiveStore();

        if (locale != null && locale.length() == 0) return AttributeUtils.allocateReadOnlyDictionary(archiveStore.getAttributes());

        L18nResourceBundle parent = I18nUtils.loadResourceBundle(archiveStore, null, null);

        for (Locale intermediateLocale : I18nUtils.generateLocaleList(Locale.getDefault()))
        {
            parent = I18nUtils.loadResourceBundle(archiveStore, parent, intermediateLocale);
        }

        if (locale != null)
        {
            Locale target = I18nUtils.parseLocale(locale);
            if (!target.equals(Locale.getDefault()))
            {
                for (Locale intermediateLocale : I18nUtils.generateLocaleList(target))
                {
                    parent = I18nUtils.loadResourceBundle(archiveStore, parent, intermediateLocale);
                }
            }
        }

        return AttributeUtils.allocateReadOnlyI18nDictionary(archiveStore.getAttributes(), parent);
    }

    /**
     * {@inheritDoc}
     */
    public String getSymbolicName()
    {
        return getCurrentGeneration().getSymbolicName();
    }

    /**
     * {@inheritDoc}
     */
    public Class loadClass(String name) throws ClassNotFoundException
    {
        try
        {
            SecurityUtils.checkAdminPermission(this, AdminPermission.CLASS);

            if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Generation bundleGeneration = obtainLatestResolvedBundle();

            return bundleGeneration.loadClass(name);
        }
        catch (SecurityException se)
        {
            throw new ClassNotFoundException("Caller does not have permission to load class", se);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration getResources(String name) throws IOException
    {
        try
        {
            SecurityUtils.checkAdminPermission(this, AdminPermission.RESOURCE);

            if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Generation bundleGeneration = obtainLatestResolvedBundle();

            return bundleGeneration.getResources(name);
        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Insufficient permission", se);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration getEntryPaths(String path)
    {
        try
        {
            SecurityUtils.checkAdminPermission(this, AdminPermission.RESOURCE);

            if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            final Enumeration<URL> enumeration = getCurrentGeneration().getArchiveStore().findEntries(path, "*", true, false);
            return new Enumeration<String>()
            {
                public boolean hasMoreElements()
                {
                    return enumeration.hasMoreElements();
                }

                public String nextElement()
                {
                    URL url = enumeration.nextElement();
                    return url.getPath();
                }
            };
        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Insufficient permission", se);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public URL getEntry(String name)
    {
        try
        {
            SecurityUtils.checkAdminPermission(this, AdminPermission.RESOURCE);

            if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            String path = "";
            String file = name;
            String[] parts = name.split("/");
            if (parts.length > 1)
            {
                StringBuilder builder = new StringBuilder(parts[0]);
                for (int i = 1; i < parts.length - 1; i++) builder.append("/").append(parts[i]);
                path = builder.toString();
                file = parts[parts.length - 1];
            }

            Enumeration<URL> entries = getCurrentGeneration().getArchiveStore().findEntries(path, file, false, false);

            if (entries.hasMoreElements())
            {
                return entries.nextElement();
            }
            else
            {
                return null;
            }
        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Insufficient permission", se);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified()
    {
        return getBundleStore().getLastModified();
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        try
        {
            SecurityUtils.checkAdminPermission(this, AdminPermission.RESOURCE);

            try
            {
                getFramework().getBundleManager().readLock();

                if (getState() == Bundle.INSTALLED)
                {
                    getFramework().getBundleManager().resolve(this);
                }

                Generation currentGeneration = getCurrentGeneration();
                List<URL> entries = new ArrayList<URL>();

                Enumeration<URL> enumeration = currentGeneration.getArchiveStore().findEntries(path, filePattern, false, recurse);

                if (enumeration != null) while (enumeration.hasMoreElements()) entries.add(enumeration.nextElement());

                if (currentGeneration instanceof BundleGeneration)
                {
                    BundleGeneration bundleGeneration = (BundleGeneration) currentGeneration;

                    for (FragmentGeneration fragment : bundleGeneration.getFragments())
                    {
                        enumeration = fragment.getArchiveStore().findEntries(path, filePattern, false, recurse);
                        if (enumeration != null) while (enumeration.hasMoreElements()) entries.add(enumeration.nextElement());
                    }
                }

                return entries.isEmpty() ? null : Collections.enumeration(entries);
            }
            catch (InterruptedException ie)
            {
                LOGGER.log(Level.WARNING, "Aquisition of a read lock interrupted", ie);
                Thread.currentThread().interrupt();
                return null;
            }
            finally
            {
                getFramework().getBundleManager().readUnlock();
            }
        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Insufficient permission", se);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public BundleContext getBundleContext()
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.CONTEXT);

        if (bundleContext == null) bundleContext = new BundleContextImpl(this);

        return bundleContext;
    }

    public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties)
    {
        return null;  //todo: consider this autogenerated code
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        return new ServiceReference[0];  //todo: consider this autogenerated code
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        return new ServiceReference[0];  //todo: consider this autogenerated code
    }

    public ServiceReference getServiceReference(String clazz)
    {
        return null;  //todo: consider this autogenerated code
    }

    public Object getService(ServiceReference serviceReference)
    {
        return null;  //todo: consider this autogenerated code
    }

    public boolean ungetService(ServiceReference serviceReference)
    {
        return false;  //todo: consider this autogenerated code
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        Generation generation = getCurrentGeneration();
        creator.append("symbolicName", generation.getSymbolicName());
        creator.append("version", generation.getVersion());
        creator.append("bundleId", getBundleId());
        creator.append("generations", getGenerations().size());

        return creator.toString();
    }

    void addBundleListener(BundleListener bundleListener)
    {
        if (bundleListener instanceof SynchronousBundleListener)
        {
            syncBundleListeners.add((SynchronousBundleListener) bundleListener);
        }
        else
        {
            bundleListeners.add(bundleListener);
        }

    }

    void removeBundleListener(BundleListener bundleListener)
    {
        if (bundleListener instanceof SynchronousBundleListener)
        {
            syncBundleListeners.remove(bundleListener);
        }
        else
        {
            bundleListeners.remove(bundleListener);
        }
    }

    void addFrameworkListener(FrameworkListener frameworkListener)
    {
        frameworkListeners.add(frameworkListener);
    }

    void removeFrameworkListener(FrameworkListener frameworkListener)
    {
        frameworkListeners.remove(frameworkListener);
    }

    void addServiceListener(ServiceListener serviceListener)
    {
        addServiceListener(serviceListener, FilterImpl.TRUE);
    }

    void addServiceListener(ServiceListener serviceListener, Filter filter)
    {
        if (serviceListener instanceof AllServiceListener)
        {
            allServiceListeners.add(new ServiceListenerWithFilter(serviceListener, filter));
        }
        else
        {
            serviceListeners.add(new ServiceListenerWithFilter(serviceListener, filter));
        }
    }

    void removeServiceListener(ServiceListener serviceListener)
    {
        if (serviceListener instanceof AllServiceListener)
        {
            allServiceListeners.remove(new ServiceListenerWithFilter(serviceListener));
        }
        else
        {
            serviceListeners.remove(new ServiceListenerWithFilter(serviceListener));
        }
        serviceListeners.remove(new ServiceListenerWithFilter(serviceListener, FilterImpl.TRUE));
    }

    /**
     * Obtain the latest generation of the bundle.  Attempt to resolve the
     * bundle generation if it is not resolved.  If that generation did not
     * resolve attempt to obtain an earlier resolved generation.
     *
     * @return latest generation of the bundle
     */
    private Generation obtainLatestResolvedBundle()
    {
        Generation result = generations.get(latestGeneration());

        if ((result.getState() & (STARTING | ACTIVE | STOPPING)) == 0)
        {
            // the newest generation is not resolved
            Papoose framework = getFramework();
            BundleManager bundleManager = framework.getBundleManager();

            if (!bundleManager.resolve(result))
            {
                /*
                 * We cannot resolve the newest generation so we'll look for a
                 * resolved older generation if one exists.  If one does not
                 * exist we return the unresolved generation.
                 */
                SortedSet<Integer> keys = new TreeSet<Integer>(generations.keySet());
                SortedSet<Integer> headSet = keys.headSet(result.getGeneration());
                if (!headSet.isEmpty()) result = generations.get(headSet.last());
            }
        }

        return result;
    }

    /**
     * Obtain the generation number of the newest generation.
     * <p/>
     * We can't be sure of the order of the keys stored in the generations mapping.
     *
     * @return the generation number of the newest generation
     */
    private int latestGeneration()
    {
        int result = -1;
        for (Integer generation : generations.keySet()) result = Math.max(result, generation);
        return result;
    }

    public static class ServiceListenerWithFilter implements AllServiceListener
    {
        private final ServiceListener delegate;
        private final Filter filter;

        public ServiceListenerWithFilter(ServiceListener delegate)
        {
            this(delegate, FilterImpl.TRUE);
        }

        public ServiceListenerWithFilter(ServiceListener delegate, Filter filter)
        {
            assert delegate != null;
            assert filter != null;

            this.delegate = delegate;
            this.filter = filter;
        }

        public Filter getFilter()
        {
            return filter;
        }

        public void serviceChanged(ServiceEvent event)
        {
            if (filter.match(event.getServiceReference())) delegate.serviceChanged(event);
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ServiceListenerWithFilter that = (ServiceListenerWithFilter) o;

            return delegate.equals(that.delegate);
        }

        public int hashCode()
        {
            return delegate.hashCode();
        }
    }
}
