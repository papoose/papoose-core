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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.AdminPermission;
import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
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
import org.osgi.framework.Version;

import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.spi.BundleStore;
import org.papoose.core.util.AttributeUtils;
import org.papoose.core.util.I18nUtils;
import org.papoose.core.util.SecurityUtils;
import org.papoose.core.util.SerialExecutor;
import org.papoose.core.util.Util;


/**
 * @version $Revision$ $Date$
 */
public class BundleController implements Bundle
{
    private final static String CLASS_NAME = BundleController.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Papoose framework;
    private final BundleStore bundleStore;
    private final Map<Integer, Generation> generations = new HashMap<Integer, Generation>();
    private Executor serialExecutor;
    private volatile Set<BundleListener> bundleListeners;
    private volatile Set<SynchronousBundleListener> syncBundleListeners;
    private volatile Set<FrameworkListener> frameworkListeners;
    private volatile Set<ServiceListener> serviceListeners;
    private volatile Set<ServiceListener> allServiceListeners;
    private volatile BundleContextProxy bundleContext;
    private volatile Generation currentGeneration;
    private volatile BundleActivator bundleActivator;


    public BundleController(Papoose framework, BundleStore bundleStore)
    {
        assert framework != null;
        assert bundleStore != null;

        this.framework = framework;
        this.bundleStore = bundleStore;
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
        lock.writeLock().lock();

        try
        {
            if (serialExecutor == null) serialExecutor = new SerialExecutor(framework.getExecutorService());

            return serialExecutor;
        }
        finally
        {
            lock.writeLock().unlock();
        }
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

    BundleActivator getBundleActivator()
    {
        return bundleActivator;
    }

    void setBundleActivator(BundleActivator bundleActivator)
    {
        this.bundleActivator = bundleActivator;
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
    public void start() throws BundleException
    {
        start(0);
    }

    /**
     * {@inheritDoc}
     */
    public void start(int options) throws BundleException
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.EXECUTE);

        try
        {
            lock.writeLock().lockInterruptibly();
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Request for start interrupted", ie);
        }

        try
        {
            if (!(currentGeneration instanceof BundleGeneration)) throw new BundleException("Cannot start fragment or extension bundle");

            if (currentGeneration.getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Papoose framework = getFramework();
            BundleGeneration bundleGeneration = (BundleGeneration) currentGeneration;

            framework.requestStart(bundleGeneration, options);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop(int options) throws BundleException
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.EXECUTE);

        try
        {
            lock.writeLock().lockInterruptibly();
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Request for stop interrupted", ie);
        }

        try
        {
            if (currentGeneration.getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Papoose framework = getFramework();
            BundleGeneration bundleGeneration = (BundleGeneration) getCurrentGeneration();

            framework.requestStop(bundleGeneration, options);
        }
        finally
        {
            lock.writeLock().unlock();
        }
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
        SecurityUtils.checkAdminPermission(this, AdminPermission.LIFECYCLE);

        try
        {
            lock.writeLock().lockInterruptibly();
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Request for update interrupted", ie);
        }

        try
        {
            if (currentGeneration.getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Papoose framework = getFramework();
            BundleGeneration bundleGeneration = (BundleGeneration) getCurrentGeneration();

            //Todo change body of implemented methods use File | Settings | File Templates.
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void update(InputStream inputStream) throws BundleException
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.LIFECYCLE);

        try
        {
            lock.writeLock().lockInterruptibly();
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Request for update interrupted", ie);
        }

        try
        {
            if (currentGeneration.getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Papoose framework = getFramework();
            BundleGeneration bundleGeneration = (BundleGeneration) getCurrentGeneration();

            //Todo change body of implemented methods use File | Settings | File Templates.
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void uninstall() throws BundleException
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.LIFECYCLE);

        try
        {
            lock.writeLock().lockInterruptibly();
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Request for update interrupted", ie);
        }

        try
        {
            if (currentGeneration.getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Papoose framework = getFramework();

            framework.getServiceRegistry().ungetService(this);
            framework.getServiceRegistry().unregister(this);

            BundleManager bundleManager = framework.getBundleManager();

            bundleManager.uninstall(this);
        }
        finally
        {
            lock.writeLock().unlock();
        }
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

        return new ServiceReference[0];  //Todo change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public ServiceReference[] getServicesInUse()
    {
        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        return new ServiceReference[0];  //Todo change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPermission(Object object)
    {
        if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

        return getCurrentGeneration().hasPermission(object);
    }

    /**
     * {@inheritDoc}
     */
    public URL getResource(String name)
    {
        try
        {
            SecurityUtils.checkAdminPermission(this, AdminPermission.RESOURCE);
        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Insufficient permission", se);
            return null;
        }

        lock.readLock().lock();

        try
        {
            if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Generation currentGeneration = getCurrentGeneration();

            if (currentGeneration instanceof FragmentGeneration) return null;

            if (getState() == Bundle.INSTALLED)
            {
                getFramework().getBundleManager().resolve(this);
            }

            if (getState() == Bundle.INSTALLED)
            {
                return currentGeneration.getResource(name);
            }
            else
            {
                BundleGeneration bundleGeneration;

                if (currentGeneration instanceof BundleGeneration)
                {
                    bundleGeneration = (BundleGeneration) currentGeneration;
                }
                else
                {
                    bundleGeneration = (BundleGeneration) getFramework().getBundleManager().getBundle(0).getCurrentGeneration();
                }

                return bundleGeneration.getClassLoader().getResource(name);
            }
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Dictionary getHeaders(String locale)
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.METADATA);

        lock.readLock().lock();

        try
        {
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
        finally
        {
            lock.readLock().unlock();
        }
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
            SecurityUtils.checkAdminPermission(this, AdminPermission.RESOURCE);
        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Insufficient permission", se);
            throw new ClassNotFoundException("Insufficient permission", se);
        }

        lock.readLock().lock();

        try
        {
            if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            if (getState() == Bundle.INSTALLED)
            {
                getFramework().getBundleManager().resolve(this);
            }

            Generation currentGeneration = getCurrentGeneration();

            if (getState() == Bundle.INSTALLED)
            {
                //noinspection ThrowableInstanceNeverThrown
                getFramework().getBundleManager().fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, this, new BundleException("Unable to resolve bundle")));
                throw new ClassNotFoundException("Unable to resolve bundle");
            }
            else
            {
                BundleGeneration bundleGeneration;

                if (currentGeneration instanceof BundleGeneration)
                {
                    bundleGeneration = (BundleGeneration) currentGeneration;
                }
                else if (currentGeneration instanceof FragmentGeneration)
                {
                    FragmentGeneration fragmentGeneration = (FragmentGeneration) currentGeneration;
                    bundleGeneration = fragmentGeneration.getHost();
                }
                else
                {
                    bundleGeneration = (BundleGeneration) getFramework().getBundleManager().getBundle(0).getCurrentGeneration();
                }

                return bundleGeneration.getClassLoader().loadClass(name);
            }
        }
        finally
        {
            lock.readLock().unlock();
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

        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Insufficient permission", se);
            return null;
        }

        lock.readLock().lock();

        try
        {
            if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Generation currentGeneration = getCurrentGeneration();

            if (currentGeneration instanceof FragmentGeneration) return null;

            if (getState() == Bundle.INSTALLED)
            {
                getFramework().getBundleManager().resolve(this);
            }

            if (getState() == Bundle.INSTALLED)
            {
                return currentGeneration.getResources(name);
            }
            else
            {
                BundleGeneration bundleGeneration;

                if (currentGeneration instanceof BundleGeneration)
                {
                    bundleGeneration = (BundleGeneration) currentGeneration;
                }
                else
                {
                    bundleGeneration = (BundleGeneration) getFramework().getBundleManager().getBundle(0).getCurrentGeneration();
                }

                return bundleGeneration.getClassLoader().findResources(name);
            }
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration getEntryPaths(String path)
    {
        lock.readLock().lock();

        try
        {
            SecurityUtils.checkAdminPermission(this, AdminPermission.RESOURCE);

            if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            Enumeration<URL> result = getCurrentGeneration().getArchiveStore().findEntries(path, "*", true, false);
            if (result == null) result = Collections.enumeration(Collections.<URL>emptySet());
            final Enumeration<URL> enumeration = result;
            return new Enumeration<String>()
            {
                public boolean hasMoreElements()
                {
                    return enumeration.hasMoreElements();
                }

                public String nextElement()
                {
                    URL url = enumeration.nextElement();
                    String path = url.getPath();

                    return path.substring(1, path.length());
                }
            };
        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Insufficient permission", se);
            return null;
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public URL getEntry(String name)
    {
        lock.readLock().lock();

        try
        {
            SecurityUtils.checkAdminPermission(this, AdminPermission.RESOURCE);

            if (getState() == UNINSTALLED) throw new IllegalStateException("This bundle is uninstalled");

            String path = "";
            String file = name;

            if (name.contains("/"))
            {
                path = name.substring(0, name.lastIndexOf('/'));
                file = name.substring(name.lastIndexOf('/') + 1, name.length());
            }

            Enumeration<URL> entries = getCurrentGeneration().getArchiveStore().findEntries(path, file, true, false);

            if (entries != null && entries.hasMoreElements())
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
        finally
        {
            lock.readLock().unlock();
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
        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Insufficient permission", se);
            return null;
        }

        lock.readLock().lock();

        try
        {
            if (getState() == Bundle.INSTALLED)
            {
                getFramework().getBundleManager().resolve(this);
            }

            Generation currentGeneration = getCurrentGeneration();
            List<URL> entries = new ArrayList<URL>();

            Enumeration<URL> enumeration = currentGeneration.getArchiveStore().findEntries(path, filePattern, true, recurse);

            if (enumeration != null) while (enumeration.hasMoreElements()) entries.add(enumeration.nextElement());

            if (currentGeneration instanceof BundleGeneration)
            {
                BundleGeneration bundleGeneration = (BundleGeneration) currentGeneration;

                for (FragmentGeneration fragment : bundleGeneration.getFragments())
                {
                    enumeration = fragment.getArchiveStore().findEntries(path, filePattern, true, recurse);

                    if (enumeration != null) while (enumeration.hasMoreElements()) entries.add(enumeration.nextElement());
                }
            }

            return entries.isEmpty() ? null : Collections.enumeration(entries);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public BundleContext getBundleContext()
    {
        SecurityUtils.checkAdminPermission(this, AdminPermission.CONTEXT);

        lock.readLock().lock();

        try
        {
            if (bundleContext == null) bundleContext = new BundleContextProxy(this);

            return bundleContext;
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map getSignerCertificates(int signersType)
    {
        lock.readLock().lock();

        try
        {
            return null;  //Todo change body of implemented methods use File | Settings | File Templates.
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Version getVersion()
    {
        return getCurrentGeneration().getVersion();
    }

    void setAutostart(AutostartSetting setting)
    {
        bundleStore.setAutoStart(setting);
    }

    AutostartSetting getAutostartSetting()
    {
        return bundleStore.getAutostart();
    }

    ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties)
    {

        return framework.getServiceRegistry().registerService(this, clazzes, service, properties);
    }

    ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {

        return framework.getServiceRegistry().getServiceReferences(this, clazz, filter);
    }

    ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {

        return framework.getServiceRegistry().getAllServiceReferences(this, clazz, filter);
    }

    ServiceReference getServiceReference(String clazz)
    {

        return framework.getServiceRegistry().getServiceReference(this, clazz);
    }

    Object getService(ServiceReference serviceReference)
    {

        return framework.getServiceRegistry().getService(this, serviceReference);
    }

    boolean ungetService(ServiceReference serviceReference)
    {

        return framework.getServiceRegistry().ungetService(this, serviceReference);
    }

    @Override
    public String toString()
    {
        return "[" + getBundleId() + "]:" + Util.bundleStateToString(getState()) + " " + getSymbolicName() + " - " + getCurrentGeneration().getVersion() + "/" + getGenerations().size();
    }

    void addBundleListener(BundleListener bundleListener)
    {
        lock.writeLock().lock();

        try
        {
            if (bundleListener instanceof SynchronousBundleListener)
            {
                if (syncBundleListeners == null) syncBundleListeners = new CopyOnWriteArraySet<SynchronousBundleListener>();

                syncBundleListeners.add((SynchronousBundleListener) bundleListener);
            }
            else
            {
                if (bundleListeners == null) bundleListeners = new CopyOnWriteArraySet<BundleListener>();

                bundleListeners.add(bundleListener);
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    void removeBundleListener(BundleListener bundleListener)
    {
        lock.writeLock().lock();

        try
        {
            if (bundleListener instanceof SynchronousBundleListener)
            {
                if (syncBundleListeners != null) syncBundleListeners.remove(bundleListener);
            }
            else
            {
                if (bundleListeners != null) bundleListeners.remove(bundleListener);
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    void addFrameworkListener(FrameworkListener frameworkListener)
    {
        lock.writeLock().lock();

        try
        {
            if (frameworkListeners == null) frameworkListeners = new CopyOnWriteArraySet<FrameworkListener>();

            frameworkListeners.add(frameworkListener);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    void removeFrameworkListener(FrameworkListener frameworkListener)
    {
        lock.writeLock().lock();

        try
        {
            if (frameworkListeners != null) frameworkListeners.remove(frameworkListener);
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    void addServiceListener(ServiceListener serviceListener)
    {
        addServiceListener(serviceListener, DefaultFilter.TRUE);
    }

    void addServiceListener(ServiceListener serviceListener, Filter filter)
    {
        lock.writeLock().lock();

        try
        {
            if (serviceListener instanceof AllServiceListener)
            {
                if (allServiceListeners == null) allServiceListeners = new CopyOnWriteArraySet<ServiceListener>();

                if (!allServiceListeners.add(new ServiceListenerWithFilter(serviceListener, filter)))
                {
                    LOGGER.warning("Listener collided with previously registered listener with filter " + filter);
                }
            }
            else
            {
                if (serviceListeners == null) serviceListeners = new CopyOnWriteArraySet<ServiceListener>();

                if (!serviceListeners.add(new ServiceListenerWithFilter(serviceListener, filter)))
                {
                    LOGGER.warning("Listener collided with previously registered listener with filter " + filter);
                }
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    void removeServiceListener(ServiceListener serviceListener)
    {
        lock.writeLock().lock();

        try
        {
            if (serviceListener instanceof AllServiceListener)
            {
                if (allServiceListeners != null) allServiceListeners.remove(new ServiceListenerWithFilter(serviceListener));
            }
            else
            {
                if (serviceListeners != null) serviceListeners.remove(new ServiceListenerWithFilter(serviceListener));
            }

            if (serviceListeners != null) serviceListeners.remove(new ServiceListenerWithFilter(serviceListener, DefaultFilter.TRUE));
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    void clearListeners()
    {
        lock.writeLock().lock();

        try
        {
            if (syncBundleListeners != null) syncBundleListeners.clear();
            if (bundleListeners != null) bundleListeners.clear();

            if (frameworkListeners != null) frameworkListeners.clear();

            if (serviceListeners != null) serviceListeners.clear();
            if (allServiceListeners != null) allServiceListeners.clear();
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    private static class ServiceListenerWithFilter implements AllServiceListener
    {
        private final ServiceListener delegate;
        private final Filter filter;

        public ServiceListenerWithFilter(ServiceListener delegate)
        {
            this(delegate, DefaultFilter.TRUE);
        }

        public ServiceListenerWithFilter(ServiceListener delegate, Filter filter)
        {
            assert delegate != null;
            assert filter != null;

            this.delegate = delegate;
            this.filter = filter;
        }

        public void serviceChanged(ServiceEvent event)
        {
            if (filter.match(event.getServiceReference())) delegate.serviceChanged(event);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ServiceListenerWithFilter that = (ServiceListenerWithFilter) o;

            return delegate == that.delegate;
        }

        @Override
        public int hashCode()
        {
            return delegate.hashCode();
        }
    }
}
