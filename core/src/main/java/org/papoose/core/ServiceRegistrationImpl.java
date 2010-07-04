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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * @version $Revision$ $Date$
 */
public final class ServiceRegistrationImpl implements ServiceRegistration
{
    private final Dictionary<String, Object> properties;
    private final long serviceId;
    private final WeakReference<ServiceRegistry> serviceRegistry;

    public ServiceRegistrationImpl(Dictionary<String, Object> properties, ServiceRegistry serviceRegistry)
    {
        assert properties != null;
        assert properties.get(Constants.SERVICE_RANKING) instanceof Integer;
        assert serviceRegistry != null;

        this.properties = properties;
        this.serviceId = (Long) properties.get(Constants.SERVICE_ID);
        this.serviceRegistry = new WeakReference<ServiceRegistry>(serviceRegistry);
    }

    /**
     * {@inheritDoc}
     */
    public ServiceReference getReference()
    {
        return new ServiceReferenceImpl();
    }

    /**
     * {@inheritDoc}
     */
    public void setProperties(Dictionary properties)
    {
        ServiceRegistry pinnedRegistry = serviceRegistry.get();
        if (pinnedRegistry == null) throw new IllegalStateException("Papoose framework already removed");

        pinnedRegistry.lock(serviceId);

        try
        {
            pinnedRegistry.validate(serviceId);

            Enumeration<String> enumeration = this.properties.keys();
            while (enumeration.hasMoreElements())
            {
                String key = enumeration.nextElement();
                if (!Constants.OBJECTCLASS.equals(key) || !Constants.SERVICE_ID.equals(key))
                {
                    this.properties.remove(key);
                }
            }

            //noinspection unchecked
            enumeration = properties.keys();
            while (enumeration.hasMoreElements())
            {
                String key = enumeration.nextElement();
                if (!Constants.OBJECTCLASS.equals(key) || !Constants.SERVICE_ID.equals(key))
                {
                    this.properties.put(key, properties.get(key));
                }
            }

            pinnedRegistry.setProperties(serviceId, this.properties);
        }
        finally
        {
            pinnedRegistry.unlock(serviceId);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregister()
    {
        ServiceRegistry pinnedRegistry = serviceRegistry.get();
        if (pinnedRegistry == null) throw new IllegalStateException("Papoose framework already removed");

        pinnedRegistry.unregister(serviceId);
    }

    @Override
    public String toString()
    {
        return Arrays.toString((Object[]) properties.get(Constants.OBJECTCLASS)) + "/" + serviceId;
    }

    public final class ServiceReferenceImpl implements ServiceReference
    {
        /**
         * {@inheritDoc}
         */
        public Object getProperty(String key)
        {
            return properties.get(key);
        }

        /**
         * {@inheritDoc}
         */
        public String[] getPropertyKeys()
        {
            List<String> keys = new ArrayList<String>();
            Enumeration<String> enumeration = properties.keys();

            while (enumeration.hasMoreElements()) keys.add(enumeration.nextElement());

            return keys.toArray(new String[keys.size()]);
        }

        /**
         * {@inheritDoc}
         */
        public Bundle getBundle()
        {
            ServiceRegistry pinnedRegistry = serviceRegistry.get();
            if (pinnedRegistry == null) throw new IllegalStateException("Papoose framework already removed");

            return pinnedRegistry.getBundle(serviceId);
        }

        /**
         * {@inheritDoc}
         */
        public Bundle[] getUsingBundles()
        {
            ServiceRegistry pinnedRegistry = serviceRegistry.get();
            if (pinnedRegistry == null) throw new IllegalStateException("Papoose framework already removed");

            return pinnedRegistry.getUsingBundles(serviceId);
        }

        /**
         * {@inheritDoc}
         */
        public boolean isAssignableTo(Bundle bundle, String className)
        {
            ServiceRegistry pinnedRegistry = serviceRegistry.get();
            if (pinnedRegistry == null) throw new IllegalStateException("Papoose framework already removed");

            return pinnedRegistry.isAssignableTo(serviceId, bundle, className);
        }

        Dictionary<String, Object> getProperties()
        {
            return properties;
        }

        long getServiceId()
        {
            return serviceId;
        }

        private WeakReference<ServiceRegistry> getServiceRegistry()
        {
            return serviceRegistry;
        }

        public int compareTo(Object o)
        {
            ServiceReferenceImpl their = (ServiceReferenceImpl) o;

            if (serviceId == their.getServiceId()) return 0;

            Integer myRank = (Integer) getProperty(Constants.SERVICE_RANKING);
            Integer theirRank = (Integer) their.getProperty(Constants.SERVICE_RANKING);
            int result = myRank.compareTo(theirRank);

            if (result != 0) return result;

            return -(Long.valueOf(serviceId).compareTo(their.getServiceId()));
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof ServiceReferenceImpl)) return false;

            ServiceReferenceImpl that = (ServiceReferenceImpl) o;

            if (serviceId != that.getServiceId()) return false;

            ServiceRegistry myRegistry = serviceRegistry.get();
            ServiceRegistry theirRegistry = that.getServiceRegistry().get();

            if (myRegistry != theirRegistry) return false;

            return true;
        }

        @Override
        public int hashCode()
        {
            ServiceRegistry registry = serviceRegistry.get();
            int result = registry != null ? registry.hashCode() : 0;
            result = 31 * result + Long.valueOf(serviceId).hashCode();
            return result;
        }

        @Override
        public String toString()
        {
            return Arrays.toString((Object[]) properties.get(Constants.OBJECTCLASS)) + "/" + serviceId;
        }
    }
}
