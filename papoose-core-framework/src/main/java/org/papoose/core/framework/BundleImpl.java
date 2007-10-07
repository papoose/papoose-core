/**
 *
 * Copyright 2007 (C) The original author or authors
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
package org.papoose.core.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;

import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.util.Listeners;
import org.papoose.core.framework.util.ResetableLatch;


/**
 * @version $Revision$ $Date$
 */
public class BundleImpl extends AbstractBundle implements Comparable<BundleImpl>
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final ResetableLatch latch = new ResetableLatch();
    private final Listeners<BundleListener, BundleEvent> bundleListeners = new Listeners<BundleListener, BundleEvent>(new Listeners.Functor<BundleListener, BundleEvent>()
    {
        public void fire(BundleListener listener, BundleEvent event) { listener.bundleChanged(event); }
    });
    private final Listeners<SynchronousBundleListener, BundleEvent> syncListeners = new Listeners<SynchronousBundleListener, BundleEvent>(new Listeners.Functor<SynchronousBundleListener, BundleEvent>()
    {
        public void fire(SynchronousBundleListener listener, BundleEvent event) { listener.bundleChanged(event); }
    });
    private final Listeners<FrameworkListener, FrameworkEvent> frameworkListeners = new Listeners<FrameworkListener, FrameworkEvent>(new Listeners.Functor<FrameworkListener, FrameworkEvent>()
    {
        public void fire(FrameworkListener listener, FrameworkEvent event) { listener.frameworkEvent(event); }
    });
    private final Listeners<ServiceListener, ServiceEvent> serviceListeners = new Listeners<ServiceListener, ServiceEvent>(new Listeners.Functor<ServiceListener, ServiceEvent>()
    {
        public void fire(ServiceListener listener, ServiceEvent event) { listener.serviceChanged(event); }
    });
    private final Object LOCK = new Object();
    private final Papoose framework;
    private BundleClassLoader classLoader;
    private int startLevel;
    private volatile State state;


    BundleImpl(Papoose framework, long bundleId, BundleStore bundleStore, ArchiveStore archiveStore)
    {
        super(bundleId, bundleStore, archiveStore);

        this.framework = framework;
        this.state = INSTALLED_STATE;
    }

    Papoose getFramework()
    {
        return framework;
    }

    public BundleClassLoader getClassLoader()
    {
        return classLoader;
    }

    public int getState()
    {
        return state.getState();
    }

    public void start() throws BundleException
    {
        synchronized (LOCK)
        {
            state.start();
        }
    }

    public void stop() throws BundleException
    {
        synchronized (LOCK)
        {
            state.stop();
        }
    }

    public void update() throws BundleException
    {
        synchronized (LOCK)
        {
            state.update();
        }
    }

    public void update(InputStream inputStream) throws BundleException
    {
        synchronized (LOCK)
        {
            state.update(inputStream);
        }
    }

    public void uninstall() throws BundleException
    {
        synchronized (LOCK)
        {
            state.uninstall();
        }
    }

    public Dictionary getHeaders()
    {
        synchronized (LOCK)
        {
            return state.getHeaders();
        }
    }

    public long getBundleId()
    {
        synchronized (LOCK)
        {
            return state.getBundleId();
        }
    }

    public String getLocation()
    {
        synchronized (LOCK)
        {
            return state.getLocation();
        }
    }

    public ServiceReference[] getRegisteredServices()
    {
        synchronized (LOCK)
        {
            return state.getRegisteredServices();
        }
    }

    public ServiceReference[] getServicesInUse()
    {
        synchronized (LOCK)
        {
            return state.getServicesInUse();
        }
    }

    public boolean hasPermission(Object o)
    {
        synchronized (LOCK)
        {
            return state.hasPermission(o);
        }
    }

    public URL getResource(String name)
    {
        synchronized (LOCK)
        {
            return state.getResource(name);
        }
    }

    public Dictionary getHeaders(String locale)
    {
        synchronized (LOCK)
        {
            return state.getHeaders(locale);
        }
    }

    public String getSymbolicName()
    {
        synchronized (LOCK)
        {
            return state.getSymbolicName();
        }
    }

    public Class loadClass(String name) throws ClassNotFoundException
    {
        synchronized (LOCK)
        {
            return state.loadClass(name);
        }
    }

    public Enumeration getResources(String name) throws IOException
    {
        synchronized (LOCK)
        {
            return state.getResources(name);
        }
    }

    public Enumeration getEntryPaths(String path)
    {
        synchronized (LOCK)
        {
            return state.getEntryPaths(path);
        }
    }

    public URL getEntry(String name)
    {
        synchronized (LOCK)
        {
            return state.getEntry(name);
        }
    }

    public long getLastModified()
    {
        synchronized (LOCK)
        {
            return state.getLastModified();
        }
    }

    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        synchronized (LOCK)
        {
            return state.findEntries(path, filePattern, recurse);
        }
    }

    void addBundleListener(BundleListener bundleListener)
    {
        bundleListeners.addListener(bundleListener);
        if (bundleListener instanceof SynchronousBundleListener) syncListeners.addListener((SynchronousBundleListener) bundleListener);
    }

    void removeBundleListener(BundleListener bundleListener)
    {
        bundleListeners.removeListener(bundleListener);
        if (bundleListener instanceof SynchronousBundleListener) syncListeners.removeListener((SynchronousBundleListener) bundleListener);
    }

    void addFrameworkListener(FrameworkListener frameworkListener)
    {
        frameworkListeners.addListener(frameworkListener);
    }

    void removeFrameworkListener(FrameworkListener frameworkListener)
    {
        frameworkListeners.removeListener(frameworkListener);
    }

    void addServiceListener(ServiceListener serviceListener)
    {
        addServiceListener(serviceListener, FilterImpl.TRUE);
    }

    void addServiceListener(ServiceListener serviceListener, Filter filter)
    {
        serviceListeners.addListener(new ServiceListenerWithFilter(serviceListener, filter));
    }

    void removeServiceListener(ServiceListener serviceListener)
    {
        serviceListeners.removeListener(new ServiceListenerWithFilter(serviceListener, FilterImpl.TRUE));
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

    public int compareTo(BundleImpl o)
    {
        if (this.getState() != o.getState()) return -1 * (int) (this.bundleId - o.bundleId);
        if (this.bundleId != o.bundleId) return (int) (this.bundleId - o.bundleId);
        return 0;
    }

    class UninstalledState extends State
    {
        public int getState()
        {
            return Bundle.UNINSTALLED;
        }

        public void start() throws BundleException
        {
            throw new IllegalStateException();
        }

        public void stop() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void update() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void update(InputStream inputStream) throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void uninstall() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public Dictionary getHeaders()
        {
            return null;  //todo: consider this autogenerated code
        }

        public long getBundleId()
        {
            return 0;  //todo: consider this autogenerated code
        }

        public String getLocation()
        {
            return null;  //todo: consider this autogenerated code
        }

        public ServiceReference[] getRegisteredServices()
        {
            return new ServiceReference[0];  //todo: consider this autogenerated code
        }

        public ServiceReference[] getServicesInUse()
        {
            return new ServiceReference[0];  //todo: consider this autogenerated code
        }

        public boolean hasPermission(Object object)
        {
            return false;  //todo: consider this autogenerated code
        }

        public URL getResource(String name)
        {
            return null;  //todo: consider this autogenerated code
        }

        public Dictionary getHeaders(String locale)
        {
            return null;  //todo: consider this autogenerated code
        }

        public String getSymbolicName()
        {
            return null;  //todo: consider this autogenerated code
        }

        public Class loadClass(String string) throws ClassNotFoundException
        {
            return null;  //todo: consider this autogenerated code
        }

        public Enumeration getResources(String name) throws IOException
        {
            return null;  //todo: consider this autogenerated code
        }

        public Enumeration getEntryPaths(String path)
        {
            return null;  //todo: consider this autogenerated code
        }

        public URL getEntry(String name)
        {
            throw new IllegalStateException("Bundle is uninstalled");
        }

        public long getLastModified()
        {
            return 0;  //todo: consider this autogenerated code
        }

        public Enumeration findEntries(String path, String filePattern, boolean recurse)
        {
            return null;  //todo: consider this autogenerated code
        }
    }

    class InstalledState extends State
    {
        public int getState()
        {
            return Bundle.INSTALLED;
        }

        public void start() throws BundleException
        {
            logger.entering(getClass().getName(), "start");

            getFramework().getBundleManager().recordBundleHasStarted(this);

            syncListeners.fireEvent(new BundleEvent(org.osgi.framework.Bundle.RESOLVED, BundleImpl.this));

            getFramework().getBundleManager().resolve(this);

            state = STARTING_STATE;

            latch.set();

            syncListeners.fireEvent(new BundleEvent(org.osgi.framework.Bundle.STARTING, BundleImpl.this));

            try
            {
                Class clazz = classLoader.loadClass(getCurrentStore().getBundleActivatorClass());
                BundleActivator bundleActivator = (BundleActivator) clazz.newInstance();
                if (bundleActivator != null) bundleActivator.start(null);
            }
            catch (Exception e)
            {
                state = RESOLVED_STATE;

                getFramework().unregisterServices(BundleImpl.this);
                getFramework().releaseServices(BundleImpl.this);

                bundleListeners.clear();
                syncListeners.clear();

                throw new BundleException("", e);
            }

            logger.exiting(getClass().getName(), "start");
        }

        public void stop() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void update() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void update(InputStream inputStream) throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void uninstall() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public Dictionary getHeaders()
        {
            return getHeaders(null);
        }

        public Dictionary getHeaders(String locale)
        {
            if (locale != null && locale.length() == 0) return allocateDictionary(getCurrentStore().getAttributes());

            L18nBundle parent = getCurrentStore().getResourceBundle(null);

            for (Locale intermediate : generateLocaleList(Locale.getDefault()))
            {
                parent = loadResourceBundle(getStores(), parent, intermediate);
            }

            if (locale != null)
            {
                Locale target = new Locale(locale);
                if (!target.equals(Locale.getDefault()))
                {
                    for (Locale intermediate : generateLocaleList(target))
                    {
                        parent = loadResourceBundle(getStores(), parent, intermediate);
                    }
                }
            }

            return allocateDictionary(getCurrentStore().getAttributes(), parent);
        }

        public long getBundleId()
        {
            return 0;  //todo: consider this autogenerated code
        }

        public String getLocation()
        {
            return null;  //todo: consider this autogenerated code
        }

        public ServiceReference[] getRegisteredServices()
        {
            return new ServiceReference[0];  //todo: consider this autogenerated code
        }

        public ServiceReference[] getServicesInUse()
        {
            return new ServiceReference[0];  //todo: consider this autogenerated code
        }

        public boolean hasPermission(Object object)
        {
            return false;  //todo: consider this autogenerated code
        }

        public URL getResource(String name)
        {
            return null;  //todo: consider this autogenerated code
        }

        public String getSymbolicName()
        {
            return null;  //todo: consider this autogenerated code
        }

        public Class loadClass(String string) throws ClassNotFoundException
        {
            return null;  //todo: consider this autogenerated code
        }

        public Enumeration getResources(String name) throws IOException
        {
            return null;  //todo: consider this autogenerated code
        }

        public Enumeration getEntryPaths(String path)
        {
            return null;  //todo: consider this autogenerated code
        }

        public URL getEntry(String name)
        {
            return null;  //todo: consider this autogenerated code
        }

        public long getLastModified()
        {
            return 0;  //todo: consider this autogenerated code
        }

        public Enumeration findEntries(String path, String filePattern, boolean recurse)
        {
            return null;  //todo: consider this autogenerated code
        }
    }

    class ResolvedState extends State
    {
        public int getState()
        {
            return Bundle.RESOLVED;
        }

        public void start() throws BundleException
        {
            logger.entering(getClass().getName(), "start");

            getFramework().getBundleManager().recordBundleHasStarted(this);

            state = STARTING_STATE;

            latch.set();

            syncListeners.fireEvent(new BundleEvent(org.osgi.framework.Bundle.STARTING, BundleImpl.this));

            try
            {
                Class clazz = classLoader.loadClass(getCurrentStore().getBundleActivatorClass());
                BundleActivator bundleActivator = (BundleActivator) clazz.newInstance();
                if (bundleActivator != null) bundleActivator.start(null);
            }
            catch (Exception e)
            {
                state = RESOLVED_STATE;

                getFramework().unregisterServices(BundleImpl.this);
                getFramework().releaseServices(BundleImpl.this);

                bundleListeners.clear();
                syncListeners.clear();

                throw new BundleException("", e);
            }

            logger.exiting(getClass().getName(), "start");
        }

        public void stop() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void update() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void update(InputStream inputStream) throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void uninstall() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public Dictionary getHeaders()
        {
            return null;  //todo: consider this autogenerated code
        }

        public long getBundleId()
        {
            return 0;  //todo: consider this autogenerated code
        }

        public String getLocation()
        {
            return null;  //todo: consider this autogenerated code
        }

        public ServiceReference[] getRegisteredServices()
        {
            return new ServiceReference[0];  //todo: consider this autogenerated code
        }

        public ServiceReference[] getServicesInUse()
        {
            return new ServiceReference[0];  //todo: consider this autogenerated code
        }

        public boolean hasPermission(Object object)
        {
            return false;  //todo: consider this autogenerated code
        }

        public URL getResource(String name)
        {
            return null;  //todo: consider this autogenerated code
        }

        public Dictionary getHeaders(String locale)
        {
            return null;  //todo: consider this autogenerated code
        }

        public String getSymbolicName()
        {
            return null;  //todo: consider this autogenerated code
        }

        public Class loadClass(String string) throws ClassNotFoundException
        {
            return null;  //todo: consider this autogenerated code
        }

        public Enumeration getResources(String name) throws IOException
        {
            return null;  //todo: consider this autogenerated code
        }

        public Enumeration getEntryPaths(String path)
        {
            return null;  //todo: consider this autogenerated code
        }

        public URL getEntry(String name)
        {
            return null;  //todo: consider this autogenerated code
        }

        public long getLastModified()
        {
            return 0;  //todo: consider this autogenerated code
        }

        public Enumeration findEntries(String path, String filePatter, boolean recurse)
        {
            return null;  //todo: consider this autogenerated code
        }
    }

    class StartingState extends ResolvedState
    {
        public int getState()
        {
            return Bundle.STARTING;
        }

        public void start() throws BundleException
        {
            try
            {
                latch.await(getFramework().getWaitPeriod());
            }
            catch (InterruptedException ie)
            {
                throw new BundleException("", ie);
            }
        }
    }

    class StopingState extends ResolvedState
    {
        public int getState()
        {
            return Bundle.STOPPING;
        }

        public void start() throws BundleException
        {
            try
            {
                latch.await(getFramework().getWaitPeriod());
                RESOLVED_STATE.start();
            }
            catch (InterruptedException ie)
            {
                throw new BundleException("", ie);
            }
        }
    }

    class ActiveState extends ResolvedState
    {
        public void start() throws BundleException
        {
        }
    }

    private final State UNINSTALLED_STATE = new UninstalledState();
    private final State INSTALLED_STATE = new InstalledState();
    private final State RESOLVED_STATE = new ResolvedState();
    private final State STARTING_STATE = new StartingState();
    private final State STOPPING_STATE = new StopingState();
    private final State ACTIVE_STATE = new ActiveState();

    private static class ServiceListenerWithFilter implements ServiceListener
    {
        private final ServiceListener delegate;
        private final Filter filter;

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
            delegate.serviceChanged(event);
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

    private static List<Locale> generateLocaleList(Locale locale)
    {
        List<Locale> result = new Vector<Locale>(3);
        String language = locale.getLanguage();
        int languageLength = language.length();
        String country = locale.getCountry();
        int countryLength = country.length();
        String variant = locale.getVariant();
        int variantLength = variant.length();

        if (languageLength + countryLength + variantLength == 0) return result;

        if (languageLength > 0) result.add(new Locale(language));

        if (countryLength + variantLength == 0) return result;

        if (countryLength > 0) result.add(new Locale(language, country));

        if (variantLength == 0) return result;

        result.add(new Locale(language, country, variant));

        return result;
    }

    private static L18nBundle loadResourceBundle(List<ArchiveStore> stores, L18nBundle parent, Locale locale)
    {
        L18nBundle resourceBundle;
        for (ArchiveStore store : stores)
        {
            resourceBundle = store.getResourceBundle(locale);
            if (resourceBundle != null)
            {
                resourceBundle.setParent(parent);
                return resourceBundle;
            }
        }
        return parent;
    }

    private static Dictionary allocateDictionary(final Attributes attributes)
    {
        return new Dictionary()
        {
            public int size() { return attributes.size(); }

            public boolean isEmpty() { return attributes.isEmpty(); }

            public Enumeration keys() { return Collections.enumeration(attributes.keySet()); }

            public Enumeration elements() { return Collections.enumeration(attributes.values()); }

            public Object get(Object key) { return attributes.getValue((String) key); }

            public Object put(Object key, Object value) { return attributes.put(key, value); }

            public Object remove(Object key) { return attributes.remove(new Attributes.Name((String) key)); }
        };
    }

    private static Dictionary allocateDictionary(final Attributes attributes, final ResourceBundle resourceBundle)
    {
        return new Dictionary()
        {
            public int size() { return attributes.size(); }

            public boolean isEmpty() { return attributes.isEmpty(); }

            public Enumeration keys() { return Collections.enumeration(attributes.keySet()); }

            public Enumeration elements()
            {
                return new Enumeration()
                {
                    Enumeration enumeration = Collections.enumeration(attributes.values());

                    public boolean hasMoreElements()
                    {
                        return enumeration.hasMoreElements();
                    }

                    public Object nextElement()
                    {
                        return enumeration.nextElement().toString();
                    }
                };
            }

            @SuppressWarnings({ "EmptyCatchBlock" })
            public Object get(Object key)
            {
                String result = attributes.getValue((String) key);
                if (result != null && result.length() > 0 && result.charAt(0) == '%')
                {
                    result = result.substring(1);
                    try
                    {
                        result = resourceBundle.getString(result);
                    }
                    catch (MissingResourceException ignore)
                    {
                    }
                }
                return result;
            }

            public Object put(Object key, Object value) { return attributes.put(key, value); }

            public Object remove(Object key) { return attributes.remove(new Attributes.Name((String) key)); }
        };
    }
}
