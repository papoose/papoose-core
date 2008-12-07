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
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.jar.Attributes;
import java.util.logging.Logger;

import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.util.SerialExecutor;


/**
 * @version $Revision$ $Date$
 */
public class BundleImpl extends AbstractBundle implements Comparable<BundleImpl>
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Executor serialExecutor;
    private final Set<FragmentBundleImpl> fragments = new TreeSet<FragmentBundleImpl>();
    private final Set<ExportDescription> exports = new TreeSet<ExportDescription>();
    private final BundleStore bundleStore;
    private BundleContextImpl bundleContext = null;
    private BundleClassLoader classLoader;
    private volatile State state;


    BundleImpl(Papoose framework, long bundleId, String location, BundleStore bundleStore, ArchiveStore archiveStore)
    {
        super(framework, bundleId, location, archiveStore);

        assert bundleStore != null;

        this.bundleStore = bundleStore;
        this.serialExecutor = new SerialExecutor(framework.getExecutorService());
        this.state = INSTALLED_STATE;
    }

    Executor getSerialExecutor()
    {
        return serialExecutor;
    }

    Set<FragmentBundleImpl> getFragments()
    {
        return fragments;
    }

    BundleStore getBundleStore()
    {
        return bundleStore;
    }

    BundleClassLoader getClassLoader()
    {
        return classLoader;
    }

    void setClassLoader(BundleClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    public int getState()
    {
        return state.getState();
    }

    public void start(int options) throws BundleException
    {
        state.start(options);
    }

    public void start() throws BundleException
    {
        start(0);
    }

    public void stop(int options) throws BundleException
    {
        state.stop(options);
    }

    public void stop() throws BundleException
    {
        stop(0);
    }

    public void update() throws BundleException
    {
        state.update();
    }

    public void update(InputStream inputStream) throws BundleException
    {
        state.update(inputStream);
    }

    public void uninstall() throws BundleException
    {
        state.uninstall();
    }

    public Dictionary getHeaders()
    {
        return state.getHeaders();
    }

    public ServiceReference[] getRegisteredServices()
    {
        return state.getRegisteredServices();
    }

    public ServiceReference[] getServicesInUse()
    {
        return state.getServicesInUse();
    }

    public boolean hasPermission(Object o)
    {
        return state.hasPermission(o);
    }

    public URL getResource(String name)
    {
        return state.getResource(name);
    }

    public Dictionary getHeaders(String locale)
    {
        return state.getHeaders(locale);
    }

    public Class loadClass(String name) throws ClassNotFoundException
    {
        return state.loadClass(name);
    }

    public Enumeration getResources(String name) throws IOException
    {
        return state.getResources(name);
    }

    public Enumeration getEntryPaths(String path)
    {
        return state.getEntryPaths(path);
    }

    public URL getEntry(String name)
    {
        return state.getEntry(name);
    }

    public long getLastModified()
    {
        return state.getLastModified();
    }

    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        if (getState() == Bundle.INSTALLED) getFramework().getBundleManager().resolve(this);
        return state.findEntries(path, filePattern, recurse);
    }

    public BundleContext getBundleContext()
    {
        return state.getBundleContext();
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
        if (this.getState() != o.getState()) return -1 * (int) (this.getBundleId() - o.getBundleId());
        if (this.getBundleId() != o.getBundleId()) return (int) (this.getBundleId() - o.getBundleId());
        return 0;
    }

    public String toString()
    {
        return "[bundle:" + getSymbolicName() + "]";
    }

    void setUninstalledState()
    {
        state = UNINSTALLED_STATE;
    }

    void setInstalledState()
    {
        state = INSTALLED_STATE;
    }

    void setResolvedState()
    {
        state = RESOLVED_STATE;
    }

    void setStartingState()
    {
        state = STARTING_STATE;
    }

    void setStopingState()
    {
        state = STOPPING_STATE;
    }

    void setActiveState() 
    {
        state = ACTIVE_STATE;
    }

    class UninstalledState extends State
    {
        public int getState()
        {
            return Bundle.UNINSTALLED;
        }

        public void start(int options) throws BundleException
        {
            throw new IllegalStateException();
        }

        public void start() throws BundleException
        {
            throw new IllegalStateException();
        }

        public void stop(int options) throws BundleException
        {
            //todo: consider this autogenerated code
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

        public BundleContext getBundleContext()
        {
            return null;
        }
    }

    class InstalledState extends State
    {
        public int getState()
        {
            return Bundle.INSTALLED;
        }

        public void start(int options) throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public void start() throws BundleException
        {
            logger.entering(getClass().getName(), "start");

            getFramework().getBundleManager().recordBundleHasStarted(this);

            getFramework().getBundleManager().fireBundleEvent(new BundleEvent(Bundle.RESOLVED, BundleImpl.this));

            getFramework().getBundleManager().resolve(this);

            state = STARTING_STATE;

            getFramework().getBundleManager().fireBundleEvent(new BundleEvent(Bundle.STARTING, BundleImpl.this));

            try
            {
                Class clazz = classLoader.loadClass(getCurrentStore().getBundleActivatorClass());
                BundleActivator bundleActivator = (BundleActivator) clazz.newInstance();
                if (bundleActivator != null) bundleActivator.start(null);
            }
            catch (Exception e)
            {
                state = RESOLVED_STATE;

                getFramework().getBundleManager().unregisterServices(BundleImpl.this);
                getFramework().getBundleManager().releaseServices(BundleImpl.this);

                bundleListeners.clear();

                throw new BundleException("", e);
            }

            logger.exiting(getClass().getName(), "start");
        }

        public void stop(int options) throws BundleException
        {
            //todo: consider this autogenerated code
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

            L18nResourceBundle parent = loadResourceBundle(getStores(), null, null);

            for (Locale intermediate : generateLocaleList(Locale.getDefault()))
            {
                parent = loadResourceBundle(getStores(), parent, intermediate);
            }

            if (locale != null)
            {
                Locale target = generateLocale(locale);
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
            return BundleImpl.super.getBundleId();
        }

        public String getLocation()
        {
            return BundleImpl.super.getLocation();
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
            return BundleImpl.this.getCurrentStore().getEntryPaths(path);
        }

        public URL getEntry(String name)
        {
            ResourceHandle handle = BundleImpl.this.getCurrentStore().getEntry(name);

            if (handle != null)
            {
                return handle.getUrl();
            }
            else
            {
                return null;
            }
        }

        public long getLastModified()
        {
            return accesstLastModified();
        }

        public Enumeration findEntries(String path, String filePattern, boolean recurse)
        {
            return BundleImpl.this.getCurrentStore().findEntries(path, filePattern, recurse);
        }

        public BundleContext getBundleContext()
        {
            return null;
        }
    }

    class ResolvedState extends InstalledState
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

            getFramework().getBundleManager().fireBundleEvent(new BundleEvent(Bundle.STARTING, BundleImpl.this));

            try
            {
                Class clazz = classLoader.loadClass(getCurrentStore().getBundleActivatorClass());
                BundleActivator bundleActivator = (BundleActivator) clazz.newInstance();
                if (bundleActivator != null) bundleActivator.start(null);
            }
            catch (Exception e)
            {
                state = RESOLVED_STATE;

                getFramework().getBundleManager().unregisterServices(BundleImpl.this);
                getFramework().getBundleManager().releaseServices(BundleImpl.this);

                bundleListeners.clear();

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

        public Class loadClass(String string) throws ClassNotFoundException
        {
            return null;  //todo: consider this autogenerated code
        }

        public Enumeration getResources(String name) throws IOException
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
            //todo: consider this autogenerated code
        }

        public BundleContext getBundleContext()
        {
            if (bundleContext == null) bundleContext = new BundleContextImpl(BundleImpl.this);
            return bundleContext;
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
            //todo: consider this autogenerated code
        }

        public BundleContext getBundleContext()
        {
            if (bundleContext == null) bundleContext = new BundleContextImpl(BundleImpl.this);
            return bundleContext;
        }
    }

    class ActiveState extends ResolvedState
    {
        public void start() throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public BundleContext getBundleContext()
        {
            if (bundleContext == null) bundleContext = new BundleContextImpl(BundleImpl.this);
            return bundleContext;
        }
    }

    private final State UNINSTALLED_STATE = new UninstalledState();
    private final State INSTALLED_STATE = new InstalledState();
    private final State RESOLVED_STATE = new ResolvedState();
    private final State STARTING_STATE = new StartingState();
    private final State STOPPING_STATE = new StopingState();
    private final State ACTIVE_STATE = new ActiveState();

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

    private static L18nResourceBundle loadResourceBundle(List<ArchiveStore> stores, L18nResourceBundle parent, Locale locale)
    {
        L18nResourceBundle resourceBundle;
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

            public Object put(Object key, Object value) { throw new UnsupportedOperationException("Read-only dictionary"); }

            public Object remove(Object key) { throw new UnsupportedOperationException("Read-only dictionary"); }
        };
    }

    private static Locale generateLocale(String locale)
    {
        String[] tokens = locale.split("_");
        if (tokens.length == 3) return new Locale(tokens[0], tokens[1], tokens[3]);
        if (tokens.length == 2) return new Locale(tokens[0], tokens[1]);
        return new Locale(tokens[0]);
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

                    public boolean hasMoreElements() { return enumeration.hasMoreElements(); }

                    public Object nextElement() { return localize(enumeration.nextElement().toString()); }
                };
            }

            public Object get(Object key) { return localize(attributes.getValue((String) key)); }

            @SuppressWarnings({ "EmptyCatchBlock" })
            protected Object localize(String result)
            {
                if (result != null && result.length() > 0 && result.charAt(0) == '%')
                {
                    result = result.substring(1);

                    if (resourceBundle != null) try { result = resourceBundle.getString(result); } catch (MissingResourceException ignore) { }
                }
                return result;
            }

            public Object put(Object key, Object value) { throw new UnsupportedOperationException("Read-only dictionary"); }

            public Object remove(Object key) { throw new UnsupportedOperationException("Read-only dictionary"); }
        };
    }
}
