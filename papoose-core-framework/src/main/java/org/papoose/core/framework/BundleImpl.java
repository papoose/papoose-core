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
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;

import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.util.Listeners;
import org.papoose.core.framework.util.ResetableLatch;


/**
 * @version $Revision$ $Date$
 */
class BundleImpl extends AbstractBundle implements org.osgi.framework.Bundle
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final ResetableLatch latch = new ResetableLatch();
    private final Listeners<BundleListener, BundleEvent> bundleListeners = new Listeners<BundleListener, BundleEvent>(new Listeners.Functor<BundleListener, BundleEvent>()
    {
        public void fire(BundleListener listener, BundleEvent event)
        {
            listener.bundleChanged(event);
        }
    });
    private final Listeners<SynchronousBundleListener, BundleEvent> syncListeners = new Listeners<SynchronousBundleListener, BundleEvent>(new Listeners.Functor<SynchronousBundleListener, BundleEvent>()
    {
        public void fire(SynchronousBundleListener listener, BundleEvent event)
        {
            listener.bundleChanged(event);
        }
    });
    private final Listeners<FrameworkListener, FrameworkEvent> frameworkListeners = new Listeners<FrameworkListener, FrameworkEvent>(new Listeners.Functor<FrameworkListener, FrameworkEvent>()
    {
        public void fire(FrameworkListener listener, FrameworkEvent event)
        {
            listener.frameworkEvent(event);
        }
    });
    private final Listeners<ServiceListener, ServiceEvent> serviceListeners = new Listeners<ServiceListener, ServiceEvent>(new Listeners.Functor<ServiceListener, ServiceEvent>()
    {
        public void fire(ServiceListener listener, ServiceEvent event)
        {
            listener.serviceChanged(event);
        }
    });
    private final ClassLoader classLoader;
    private final Papoose framework;
    private final BundleStore bundleStore;
    private final Object LOCK = new Object();
    private int startLevel;
    private volatile State state;

    /**
     * Manifest
     */
    private final String bundleActivatorClass;
    private final List<String> bundleCategories;
    private final List<String> bundleClassPath;
    private final String bundleContactAddress;
    private final String bundleCopyright;
    private final String bundleDescription;
    private final String bundleDocUrl;
    private final String bundleLocalization;
    private final short bundleManifestVersion;
    private final String bundleName;
    private final List<NativeCodeDescription> bundleNativeCodeList;
    private final boolean bundleNativeCodeListOptional;
    private final List<String> bundleExecutionEnvironment;
    private final String bundleSymbolicName;
    private final URL bundleUpdateLocation;
    private final String bundleVendor;
    private final Version bundleVersion;
    private final List<DynamicDescription> bundleDynamicImportList;
    private final List<ExportDescription> bundleExportList;
    private final List<String> bundleExportService;
    private final FragmentDescription bundleFragmentHost;
    private final List<ImportDescription> bundleImportList;
    private final List<String> bundleImportService;
    private final List<RequireDescription> bundleRequireBundle;


    public BundleImpl(ClassLoader classLoader, Papoose framework, BundleStore bundleStore, long bundleId,
                      String bundleActivatorClass, List<String> bundleCategories, List<String> bundleClassPath, String bundleContactAddress, String bundleCopyright, String bundleDescription, String bundleDocUrl, String bundleLocalization, short bundleManifestVersion, String bundleName, List<NativeCodeDescription> bundleNativeCodeList, boolean bundleNativeCodeListOptional, List<String> bundleExecutionEnvironment, String bundleSymbolicName, URL bundleUpdateLocation, String bundleVendor, Version bundleVersion, List<DynamicDescription> bundleDynamicImportList, List<ExportDescription> bundleExportList, List<String> bundleExportService, FragmentDescription bundleFragmentHost, List<ImportDescription> bundleImportList, List<String> bundleImportService, List<RequireDescription> bundleRequireBundle) throws BundleException
    {
        super(bundleId);

        this.classLoader = classLoader;
        this.framework = framework;
        this.bundleStore = bundleStore;
        this.state = UNINSTALLED_STATE;

        this.bundleActivatorClass = bundleActivatorClass;
        this.bundleCategories = bundleCategories;
        this.bundleClassPath = bundleClassPath;
        this.bundleContactAddress = bundleContactAddress;
        this.bundleCopyright = bundleCopyright;
        this.bundleDescription = bundleDescription;
        this.bundleDocUrl = bundleDocUrl;
        this.bundleLocalization = bundleLocalization;
        this.bundleManifestVersion = bundleManifestVersion;
        this.bundleName = bundleName;
        this.bundleNativeCodeList = bundleNativeCodeList;
        this.bundleNativeCodeListOptional = bundleNativeCodeListOptional;
        this.bundleExecutionEnvironment = bundleExecutionEnvironment;
        this.bundleSymbolicName = bundleSymbolicName;
        this.bundleUpdateLocation = bundleUpdateLocation;
        this.bundleVendor = bundleVendor;
        this.bundleVersion = bundleVersion;
        this.bundleDynamicImportList = bundleDynamicImportList;
        this.bundleExportList = bundleExportList;
        this.bundleExportService = bundleExportService;
        this.bundleFragmentHost = bundleFragmentHost;
        this.bundleImportList = bundleImportList;
        this.bundleImportService = bundleImportService;
        this.bundleRequireBundle = bundleRequireBundle;

        if (bundleManifestVersion != 2) throw new BundleException("Bundle-ManifestVersion must be 2");
    }

    List<String> getBundleExecutionEnvironment()
    {
        return Collections.unmodifiableList(bundleExecutionEnvironment);
    }

    /**
     * Make sure that at least one native code description is valid.
     * <p/>
     * This could be done during the <code>BundleImpl</code> constructor but
     * it seems to be more transparent to have the bundle manager call this
     * method.
     *
     * @return a list of resolvable native code descriptions
     * @throws BundleException if the method is unable to find at least one valid native code description
     */
    SortedSet<NativeCodeDescription> resolveNativeCodeDependencies() throws BundleException
    {
        SortedSet<NativeCodeDescription> set = new TreeSet<NativeCodeDescription>();

        if (!bundleNativeCodeList.isEmpty())
        {
            VersionRange osVersionRange = VersionRange.parseVersionRange((String) framework.getProperty(Constants.FRAMEWORK_OS_VERSION));

            nextDescription:
            for (NativeCodeDescription description : bundleNativeCodeList)
            {
                Map<String, Object> parameters = description.getParameters();
                for (String key : parameters.keySet())
                {
                    if ("osname".equals(key) && !framework.getProperty(Constants.FRAMEWORK_OS_NAME).equals(parameters.get(key))) continue nextDescription;
                    else if ("processor".equals(key) && !framework.getProperty(Constants.FRAMEWORK_PROCESSOR).equals(parameters.get(key))) continue nextDescription;
                    else if ("osversion".equals(key))
                    {
                        if (!osVersionRange.includes(description.getOsVersion())) continue nextDescription;
                    }
                    else if ("language".equals(key) && !framework.getProperty(Constants.FRAMEWORK_LANGUAGE).equals(description.getLanguage())) continue nextDescription;
                    else if ("selection-filter".equals(key))
                    {
                        try
                        {
                            Filter selectionFilter = new FilterImpl(framework.getParser().parse((String) parameters.get(key)));
                            if (!selectionFilter.match(framework.getProperties())) continue nextDescription;
                        }
                        catch (InvalidSyntaxException ise)
                        {
                            throw new BundleException("Invalid selection filter", ise);
                        }
                    }
                }

                set.add(description);
            }
        }

        return set;
    }

    public void zstart() throws BundleException
    {
        logger.entering(getClass().getName(), "start");

        if (state == UNINSTALLED_STATE) throw new IllegalStateException();

        if (state == STARTING_STATE || state == STOPPING_STATE)
        {
            try
            {
                latch.await(framework.getWaitPeriod());
            }
            catch (InterruptedException ie)
            {
                throw new BundleException("", ie);
            }
        }

        if (state == ACTIVE_STATE) return;

        framework.getBundleManager().recordBundleHasStarted(this);

        if (state != RESOLVED_STATE) framework.getBundleManager().resolve(this);

        state = STARTING_STATE;

        latch.set();

        syncListeners.fireEvent(new BundleEvent(org.osgi.framework.Bundle.STARTING, this));

        try
        {
            Class clazz = classLoader.loadClass(bundleActivatorClass);
            BundleActivator bundleActivator = (BundleActivator) clazz.newInstance();
            if (bundleActivator != null) bundleActivator.start(null);
        }
        catch (Exception e)
        {
            state = RESOLVED_STATE;

            framework.unregisterServices(this);
            framework.releaseServices(this);

            bundleListeners.clear();
            syncListeners.clear();

            throw new BundleException("", e);
        }
        //todo: consider this autogenerated code

        logger.exiting(getClass().getName(), "start");
    }

    void markInstalled()
    {
        state = INSTALLED_STATE;
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

    Papoose getFramework()
    {
        return framework;
    }

    BundleStore getBundleStore()
    {
        return bundleStore;
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

    abstract class State implements org.osgi.framework.Bundle
    {
    }

    class UninstalledState extends State
    {
        public int getState()
        {
            return Bundle.UNINSTALLED;
        }

        public void start() throws BundleException
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

    class InstalledState extends State
    {
        public int getState()
        {
            return 0;  //todo: consider this autogenerated code
        }

        public void start() throws BundleException
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
            return 0;  //todo: consider this autogenerated code
        }

        public void start() throws BundleException
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
    }

    class StopingState extends ResolvedState
    {
    }

    class ActiveState extends ResolvedState
    {
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
}
