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

    public BundleContextImpl(BundleImpl bundle)
    {
        assert bundle != null;

        this.bundle = bundle;
    }

    void invalidateContext()
    {
        bundle = null;
    }

    Papoose getFramework()
    {
        return getBundle().getFramework();
    }

    public String getProperty(String key)
    {
        return (String) getFramework().getProperty(key);
    }

    public BundleImpl getBundle()
    {
        BundleImpl bundle = this.bundle;
        if (bundle == null) throw new IllegalStateException("This bundle is no longer valid");
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
        return getFramework().getBundleManager().installBundle(location, inputStream);
    }

    public Bundle getBundle(long bundleId)
    {
        return getFramework().getBundleManager().getBundle(bundleId);
    }

    public Bundle[] getBundles()
    {
        return getFramework().getBundleManager().getBundles();
    }

    public void addServiceListener(ServiceListener serviceListener, String filter) throws InvalidSyntaxException
    {
        getBundle().addServiceListener(serviceListener, new FilterImpl(getFramework().getParser().parse(filter)));
    }

    public void addServiceListener(ServiceListener serviceListener)
    {
        getBundle().addServiceListener(serviceListener);
    }

    public void removeServiceListener(ServiceListener serviceListener)
    {
        getBundle().removeServiceListener(serviceListener);
    }

    public void addBundleListener(BundleListener bundleListener)
    {
        getBundle().addBundleListener(bundleListener);
    }

    public void removeBundleListener(BundleListener bundleListener)
    {
        getBundle().removeBundleListener(bundleListener);
    }

    public void addFrameworkListener(FrameworkListener frameworkListener)
    {
        getBundle().addFrameworkListener(frameworkListener);
    }

    public void removeFrameworkListener(FrameworkListener frameworkListener)
    {
        getBundle().removeFrameworkListener(frameworkListener);
    }

    public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties)
    {
        return getBundle().registerService(clazzes, service, properties);
    }

    public ServiceRegistration registerService(String clazz, Object service, Dictionary properties)
    {
        return registerService(new String[]{ clazz }, service, properties);
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        return getBundle().getServiceReferences(clazz, filter);
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException
    {
        return getBundle().getAllServiceReferences(clazz, filter);
    }

    public ServiceReference getServiceReference(String clazz)
    {
        return getBundle().getServiceReference(clazz);
    }

    public Object getService(ServiceReference serviceReference)
    {
        return getBundle().getService(serviceReference);
    }

    public boolean ungetService(ServiceReference serviceReference)
    {
        return getBundle().ungetService(serviceReference);
    }

    public File getDataFile(String filename)
    {
        return new File(getBundle().getBundleStore().getDataRoot(), filename);
    }

    public Filter createFilter(String filter) throws InvalidSyntaxException
    {
        return new FilterImpl(getFramework().getParser().parse(filter));
    }
}
