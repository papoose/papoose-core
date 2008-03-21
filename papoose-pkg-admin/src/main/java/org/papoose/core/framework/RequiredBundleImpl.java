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
package org.papoose.core.framework;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.RequiredBundle;

/**
 * @version $Revision$ $Date$
 */
public class RequiredBundleImpl implements RequiredBundle
{
    private final Object monitor = new Object();
    private final String symbolicName;
    private final Version version;
    private volatile Bundle bundle;
    private volatile Bundle[] requiringBundles;

    public RequiredBundleImpl(String symbolicName, Version version, Bundle bundle, Bundle[] requiringBundles)
    {
        this.symbolicName = symbolicName;
        this.version = version;
        this.bundle = bundle;
        this.requiringBundles = requiringBundles;
    }

    public String getSymbolicName()
    {
        return symbolicName;
    }

    public Bundle getBundle()
    {
        return bundle;
    }

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

    public Version getVersion()
    {
        return version;
    }

    public boolean isRemovalPending()
    {
        return bundle == null;
    }

    void clear()
    {
        synchronized (monitor)
        {
            bundle = null;
            requiringBundles = null;
        }
    }
}
