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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;

import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;


/**
 * @version $Revision$ $Date$
 */
public abstract class AbstractBundle implements Bundle
{
    protected final Set<BundleListener> bundleListeners = new CopyOnWriteArraySet<BundleListener>();
    protected final Set<SynchronousBundleListener> syncBundleListeners = new CopyOnWriteArraySet<SynchronousBundleListener>();
    protected final Set<FrameworkListener> frameworkListeners = new CopyOnWriteArraySet<FrameworkListener>();
    protected final Set<ServiceListener> serviceListeners = new CopyOnWriteArraySet<ServiceListener>();
    protected final Set<ServiceListener> allServiceListeners = new CopyOnWriteArraySet<ServiceListener>();
    private final Papoose framework;
    private final long bundleId;
    private final String location;
    private ArchiveStore currentStore;
    private ArchiveStore nextStore;
    private final List<ArchiveStore> stores = new ArrayList<ArchiveStore>();
    private volatile long lastModified;

    protected AbstractBundle(Papoose framework, long bundleId, String location, ArchiveStore currentStore)
    {
        assert framework != null;
        assert location != null;
        assert currentStore != null;

        this.framework = framework;
        this.bundleId = bundleId;
        this.location = location;
        this.currentStore = currentStore;

        this.stores.add(currentStore);
        this.lastModified = System.currentTimeMillis();
    }

    Papoose getFramework()
    {
        return framework;
    }

    public String getSymbolicName()
    {
        return currentStore.getBundleSymbolicName();
    }

    public Version getVersion()
    {
        return currentStore.getBundleVersion();
    }

    public long getBundleId()
    {
        return bundleId;
    }

    public String getLocation()
    {
        return location;
    }

    public List<ExportDescription> getBundleExportList()
    {
        return currentStore.getBundleExportList();
    }

    ArchiveStore getCurrentStore()
    {
        return currentStore;
    }

    void setCurrentStore(ArchiveStore currentStore)
    {
        this.currentStore = currentStore;
    }

    ArchiveStore getNextStore()
    {
        return nextStore;
    }

    void setNextStore(ArchiveStore nextStore)
    {
        this.nextStore = nextStore;
    }

    List<ArchiveStore> getStores()
    {
        return stores;
    }

    long accesstLastModified()
    {
        return lastModified;
    }

    void setLastModified(long lastModified)
    {
        this.lastModified = lastModified;
    }

    void markInstalled() throws BundleException
    {
        // TODO
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

    abstract class State implements Bundle
    {
        public String getSymbolicName()
        {
            return AbstractBundle.this.getSymbolicName();
        }
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
