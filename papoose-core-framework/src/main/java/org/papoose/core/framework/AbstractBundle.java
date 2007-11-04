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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.SynchronousBundleListener;

import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.util.Listeners;


/**
 * @version $Revision$ $Date$
 */
abstract class AbstractBundle implements Bundle
{
    protected final Listeners<BundleListener, BundleEvent> bundleListeners = new Listeners<BundleListener, BundleEvent>(new Listeners.Functor<BundleListener, BundleEvent>()
    {
        public void fire(BundleListener listener, BundleEvent event) { listener.bundleChanged(event); }
    });
    protected final Listeners<SynchronousBundleListener, BundleEvent> syncListeners = new Listeners<SynchronousBundleListener, BundleEvent>(new Listeners.Functor<SynchronousBundleListener, BundleEvent>()
    {
        public void fire(SynchronousBundleListener listener, BundleEvent event) { listener.bundleChanged(event); }
    });
    protected final Listeners<FrameworkListener, FrameworkEvent> frameworkListeners = new Listeners<FrameworkListener, FrameworkEvent>(new Listeners.Functor<FrameworkListener, FrameworkEvent>()
    {
        public void fire(FrameworkListener listener, FrameworkEvent event) { listener.frameworkEvent(event); }
    });
    protected final Listeners<ServiceListener, ServiceEvent> serviceListeners = new Listeners<ServiceListener, ServiceEvent>(new Listeners.Functor<ServiceListener, ServiceEvent>()
    {
        public void fire(ServiceListener listener, ServiceEvent event) { listener.serviceChanged(event); }
    });
    private final Papoose framework;
    private final long bundleId;
    private final String location;
    private final BundleStore bundleStore;
    private ArchiveStore currentStore;
    private ArchiveStore nextStore;
    private final List<ArchiveStore> stores = new ArrayList<ArchiveStore>();
    private volatile long lastModified;

    protected AbstractBundle(Papoose framework, long bundleId, String location, BundleStore bundleStore, ArchiveStore currentStore)
    {
        this.framework = framework;
        this.bundleId = bundleId;
        this.location = location;
        this.bundleStore = bundleStore;
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

    BundleStore getBundleStore()
    {
        return bundleStore;
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
        bundleStore.setStarted(true);
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

    abstract class State implements Bundle
    {
        public String getSymbolicName()
        {
            return AbstractBundle.this.getSymbolicName();
        }
    }

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
