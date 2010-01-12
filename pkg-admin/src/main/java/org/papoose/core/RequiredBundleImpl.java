/**
 *
 * Copyright 2008 (C) The original author or authors
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

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.RequiredBundle;

/**
 * {@inheritDoc}
 * <p/>
 * Developer's notes: This class' thread safety is protected by an internal
 * monitor instead of "this" so that bundles that obtain an instance of this
 * class cannot inadvertently, or maliciously, lock this object and hence lock
 * the framework.
 *
 * @version $Revision$ $Date$
 */
public class RequiredBundleImpl implements RequiredBundle
{
    private final Object monitor = new Object();
    private final String symbolicName;
    private final Version version;
    private Bundle bundle;
    private Bundle[] requiringBundles;

    public RequiredBundleImpl(String symbolicName, Version version, Bundle bundle, Bundle[] requiringBundles)
    {
        assert symbolicName != null;
        assert version != null;
        assert bundle != null;
        assert requiringBundles != null;

        this.symbolicName = symbolicName;
        this.version = version;
        this.bundle = bundle;
        this.requiringBundles = requiringBundles;
    }

    /**
     * {@inheritDoc}
     */
    public String getSymbolicName()
    {
        return symbolicName;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle getBundle()
    {
        synchronized (monitor)
        {
            return bundle;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Bundle[] getRequiringBundles()
    {
        synchronized (monitor)
        {
            Bundle[] result = null;
            if (requiringBundles != null)
            {
                result = new Bundle[requiringBundles.length];
                System.arraycopy(requiringBundles, 0, result, 0, result.length);
            }
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Version getVersion()
    {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRemovalPending()
    {
        synchronized (monitor)
        {
            return bundle == null;
        }
    }

    /**
     * Method used by <code>PackageAdminImpl</code> to clear this object to
     * indicate that the bundle reference has become stale.
     */
    void clear()
    {
        synchronized (monitor)
        {
            bundle = null;
            requiringBundles = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequiredBundleImpl that = (RequiredBundleImpl) o;

        return symbolicName.equals(that.symbolicName) && version.equals(that.version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int result;
        result = symbolicName.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        synchronized (monitor)
        {
            return "(RequiredPackageImpl name: " + symbolicName + " version: " + version + (bundle != null ? bundle + " " + requiringBundles.length + " importing bundles" : " stale") + ")";
        }
    }
}
