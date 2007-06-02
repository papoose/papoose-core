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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * @version $Revision$ $Date$
 */
class BundleContextImpl implements BundleContext
{
    private volatile BundleImpl bundle;
    private final Papoose framework;
    private final Object LOCK = new Object();
    private volatile BundleContext state;

    public BundleContextImpl(BundleImpl bundle)
    {
        assert bundle != null;

        this.bundle = bundle;
        this.framework = bundle.getFramework();
        this.state = VALID_STATE;
    }

    public String getProperty(String key)
    {
        synchronized (LOCK)
        {
            return state.getProperty(key);
        }
    }

    public Bundle getBundle()
    {
        synchronized (LOCK)
        {
            return state.getBundle();
        }
    }

    public Bundle installBundle(String location) throws BundleException
    {
        synchronized (LOCK)
        {
            return state.installBundle(location);
        }
    }

    public Bundle installBundle(String location, InputStream inputStream) throws BundleException
    {
        synchronized (LOCK)
        {
            return state.installBundle(location, inputStream);
        }
    }

    public Bundle getBundle(long bundleId)
    {
        synchronized (LOCK)
        {
            return state.getBundle(bundleId);
        }
    }

    public Bundle[] getBundles()
    {
        synchronized (LOCK)
        {
            return state.getBundles();
        }
    }

    public void addServiceListener(ServiceListener serviceListener, String filter) throws InvalidSyntaxException
    {
        synchronized (LOCK)
        {
            state.addServiceListener(serviceListener, filter);
        }
    }

    public void addServiceListener(ServiceListener serviceListener)
    {
        synchronized (LOCK)
        {
            state.addServiceListener(serviceListener);
        }
    }

    public void removeServiceListener(ServiceListener serviceListener)
    {
        synchronized (LOCK)
        {
            state.removeServiceListener(serviceListener);
        }
    }

    public void addBundleListener(BundleListener bundleListener)
    {
        synchronized (LOCK)
        {
            state.addBundleListener(bundleListener);
        }
    }

    public void removeBundleListener(BundleListener bundleListener)
    {
        synchronized (LOCK)
        {
            state.removeBundleListener(bundleListener);
        }
    }

    public void addFrameworkListener(FrameworkListener frameworkListener)
    {
        synchronized (LOCK)
        {
            state.addFrameworkListener(frameworkListener);
        }
    }

    public void removeFrameworkListener(FrameworkListener frameworkListener)
    {
        synchronized (LOCK)
        {
            state.removeFrameworkListener(frameworkListener);
        }
    }

    public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties)
    {
        synchronized (LOCK)
        {
            return state.registerService(clazzes, service, properties);
        }
    }

    public ServiceRegistration registerService(String clazz, Object service, Dictionary properties)
    {
        synchronized (LOCK)
        {
            return state.registerService(clazz, service, properties);
        }
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        synchronized (LOCK)
        {
            return state.getServiceReferences(clazz, filter);
        }
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        synchronized (LOCK)
        {
            return state.getAllServiceReferences(clazz, filter);
        }
    }

    public ServiceReference getServiceReference(String clazz)
    {
        synchronized (LOCK)
        {
            return state.getServiceReference(clazz);
        }
    }

    public Object getService(ServiceReference serviceReference)
    {
        synchronized (LOCK)
        {
            return state.getService(serviceReference);
        }
    }

    public boolean ungetService(ServiceReference serviceReference)
    {
        synchronized (LOCK)
        {
            return state.ungetService(serviceReference);
        }
    }

    public File getDataFile(String filename)
    {
        synchronized (LOCK)
        {
            return state.getDataFile(filename);
        }
    }

    public Filter createFilter(String filter) throws InvalidSyntaxException
    {
        synchronized (LOCK)
        {
            return state.createFilter(filter);
        }
    }

    void invalidateContext()
    {
        synchronized (LOCK)
        {
            bundle = null;
            state = INVALID_STATE;
        }
    }

    private class ValidBundleContextState implements BundleContext
    {
        public String getProperty(String key)
        {
            return framework.getProperty(key);
        }

        public Bundle getBundle()
        {
            return bundle;
        }

        public Bundle installBundle(String location) throws BundleException
        {
            try
            {
                return installBundle(location, new URL(location).openStream());
            }
            catch (IOException ioe)
            {
                throw new BundleException("Unable to open a stream for location: " + location, ioe);
            }
        }

        public Bundle installBundle(String location, InputStream inputStream) throws BundleException
        {
            return framework.getBundleManager().installBundle(location, inputStream);
        }

        public Bundle getBundle(long bundleId)
        {
            return framework.getBundleManager().getBundle(bundleId);
        }

        public Bundle[] getBundles()
        {
            return framework.getBundleManager().getBundles();
        }

        public void addServiceListener(ServiceListener serviceListener, String filter) throws InvalidSyntaxException
        {
            bundle.addServiceListener(serviceListener, new FilterImpl(framework.getParser().parse(filter)));
        }

        public void addServiceListener(ServiceListener serviceListener)
        {
            bundle.addServiceListener(serviceListener);
        }

        public void removeServiceListener(ServiceListener serviceListener)
        {
            bundle.removeServiceListener(serviceListener);
        }

        public void addBundleListener(BundleListener bundleListener)
        {
            bundle.addBundleListener(bundleListener);
        }

        public void removeBundleListener(BundleListener bundleListener)
        {
            bundle.removeBundleListener(bundleListener);
        }

        public void addFrameworkListener(FrameworkListener frameworkListener)
        {
            bundle.addFrameworkListener(frameworkListener);
        }

        public void removeFrameworkListener(FrameworkListener frameworkListener)
        {
            bundle.removeFrameworkListener(frameworkListener);
        }

        public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties)
        {
            return bundle.registerService(clazzes, service, properties);
        }

        public ServiceRegistration registerService(String clazz, Object service, Dictionary properties)
        {
            return registerService(new String[]{clazz}, service, properties);
        }

        public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
        {
            return bundle.getServiceReferences(clazz, filter);
        }

        public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
        {
            return bundle.getAllServiceReferences(clazz, filter);
        }

        public ServiceReference getServiceReference(String clazz)
        {
            return bundle.getServiceReference(clazz);
        }

        public Object getService(ServiceReference serviceReference)
        {
            return bundle.getService(serviceReference);
        }

        public boolean ungetService(ServiceReference serviceReference)
        {
            return bundle.ungetService(serviceReference);
        }

        public File getDataFile(String filename)
        {
            return new File(bundle.getBundleStore().getDataRoot(), filename);
        }

        public Filter createFilter(String filter) throws InvalidSyntaxException
        {
            return new FilterImpl(framework.getParser().parse(filter));
        }
    }

    private class InvalidBundleContextState extends ValidBundleContextState
    {
        public String getProperty(String key)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public Bundle getBundle()
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public Bundle installBundle(String location) throws BundleException
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public Bundle installBundle(String location, InputStream inputStream) throws BundleException
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public void addServiceListener(ServiceListener serviceListener, String filter) throws InvalidSyntaxException
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public void addServiceListener(ServiceListener serviceListener)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public void removeServiceListener(ServiceListener serviceListener)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public void addBundleListener(BundleListener bundleListener)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public void removeBundleListener(BundleListener bundleListener)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public void addFrameworkListener(FrameworkListener frameworkListener)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public void removeFrameworkListener(FrameworkListener frameworkListener)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public ServiceRegistration registerService(String clazz, Object service, Dictionary properties)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public ServiceReference getServiceReference(String clazz)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public Object getService(ServiceReference serviceReference)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public boolean ungetService(ServiceReference serviceReference)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public File getDataFile(String filename)
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }

        public Filter createFilter(String filter) throws InvalidSyntaxException
        {
            throw new IllegalStateException("This bundle is no longer valid");
        }
    }

    private final BundleContext VALID_STATE = new ValidBundleContextState();
    private final BundleContext INVALID_STATE = new InvalidBundleContextState();
}
