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

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;

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
@ThreadSafe
public class ExportedPackageImpl implements ExportedPackage, Comparable
{
    private final Object monitor = new Object();
    private final String name;
    private final Version version;
    @GuardedBy("monitor") private Bundle exportingBundle;
    @GuardedBy("monitor") private Bundle[] importingBundles;

    /**
     * {@inheritDoc}
     * <p/>
     * Developer's notes: The spec states that the version can be null.
     * However, I just don't see how this can happen since there's always a
     * default value which I proceed to set when the bundle is loaded.
     */
    public ExportedPackageImpl(String name, Version version, Bundle exportingBundle, Bundle[] importingBundles)
    {
        assert name != null;
        assert version != null;
        assert exportingBundle != null;
        assert importingBundles != null;

        this.name = name;
        this.version = version;
        this.exportingBundle = exportingBundle;
        this.importingBundles = importingBundles;
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle getExportingBundle()
    {
        synchronized (monitor)
        {
            return exportingBundle;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Bundle[] getImportingBundles()
    {
        synchronized (monitor)
        {
            Bundle[] result = null;
            if (importingBundles != null)
            {
                result = new Bundle[importingBundles.length];
                System.arraycopy(importingBundles, 0, result, 0, result.length);
            }
            return result;
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getSpecificationVersion()
    {
        return version.toString();
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
            return exportingBundle == null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo(Object o)
    {
        Version myVersion = version == null ? VersionRange.DEFAULT_VERSION : version;
        Version theirVersion = ((ExportedPackageImpl) o).version == null ? VersionRange.DEFAULT_VERSION : ((ExportedPackageImpl) o).version;
        return myVersion.compareTo(theirVersion);
    }

    /**
     * Method used by <code>PackageAdminImpl</code> to clear this object to
     * indicate that the bundle reference has become stale.
     */
    void clear()
    {
        synchronized (monitor)
        {
            exportingBundle = null;
            importingBundles = null;
        }
    }
}
