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

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

import org.papoose.core.framework.spi.BundleManager;

/**
 * @version $Revision$ $Date$
 */
public class PackageAdminImpl implements PackageAdmin, SynchronousBundleListener
{
    private Set<WeakReference<ExportedPackage>> s = new HashSet<WeakReference<ExportedPackage>>();
    private final Papoose framework;
    private long lastRefreshed = 0;

    public PackageAdminImpl(Papoose framework)
    {
        this.framework = framework;
    }

    void start()
    {
        BundleManager manager = framework.getBundleManager();

        manager.lock();
        try
        {
            for (Bundle bundle : manager.getBundles())
            {
                BundleContext context = bundle.getBundleContext();
                if (context != null) context.addBundleListener(this);
            }
        }
        finally
        {
            manager.unlock();
        }
    }

    void stop()
    {
        BundleManager manager = framework.getBundleManager();

        manager.lock();
        try
        {
            for (Bundle bundle : manager.getBundles())
            {
                BundleContext context = bundle.getBundleContext();
                if (context != null) context.removeBundleListener(this);
            }
        }
        finally
        {
            manager.unlock();
        }
    }

    /**
     * Gets the exported packages for the specified bundle.
     * <p/>
     * The implementation checks to make sure that the bundle belongs to the
     * same framework instance as the one that this package admin service
     * belongs to.  If not, it returns null.
     *
     * @param bundle The bundle whose exported packages are to be returned, or
     *               null if all exported packages are to be returned. If the
     *               specified bundle is the system bundle (that is, the bundle
     *               with id zero), this method returns all the packages known
     *               to be exported by the system bundle. This will include the
     *               package specified by the
     *               <code>org.osgi.framework.system.packages</code> system
     *               property as well as any other package exported by the
     *               framework implementation.
     * @return An array of exported packages, null if the specified bundle has
     *         no exported packages, or null if the bundle does not belong to
     *         the same framework instance.
     */
    public ExportedPackage[] getExportedPackages(Bundle bundle)
    {
        Set<BundleImpl> collectedBundles = new HashSet<BundleImpl>();
        Set<ExportedPackageImpl> collectedExports = new HashSet<ExportedPackageImpl>();

        if (bundle == null)
        {
            for (Bundle b : framework.getBundleManager().getBundles())
            {
                if (b instanceof BundleImpl)
                {
                    collectedBundles.add((BundleImpl) b);
                }
            }
        }
        else if (bundle instanceof BundleImpl)
        {
            BundleImpl bundleImpl = (BundleImpl) bundle;
            if (framework.getFrameworkId() == bundleImpl.getFramework().getFrameworkId())
            {
                collectedBundles.add(bundleImpl);
            }
        }

        for (BundleImpl bundleImpl : collectedBundles)
        {
            for (ExportDescription export : bundleImpl.getCurrentStore().getBundleExportList())
            {
                Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                for (String packageName : export.getPackages())
                {
                    Set<BundleImpl> importers = new HashSet<BundleImpl>();
                    BundleClassLoader classLoader = bundleImpl.getClassLoader();

                    if (classLoader != null)
                    {
                        for (Wire wire : classLoader.getWires())
                        {
                            if (packageName.equals(wire.getPackageName())) importers.add(wire.getBundle());
                        }
                    }

                    collectedExports.add(new ExportedPackageImpl(packageName, version, bundleImpl, importers.toArray(new Bundle[importers.size()])));
                }
            }
        }

        int numExports = collectedExports.size();
        return (numExports == 0 ? null : collectedExports.toArray(new ExportedPackage[numExports]));
    }

    public ExportedPackage[] getExportedPackages(String targetPackageName)
    {
        Set<BundleImpl> collectedBundles = new HashSet<BundleImpl>();
        Set<ExportedPackageImpl> collectedExports = new HashSet<ExportedPackageImpl>();

        for (Bundle b : framework.getBundleManager().getBundles())
        {
            if (b instanceof BundleImpl)
            {
                collectedBundles.add((BundleImpl) b);
            }
        }

        for (BundleImpl bundleImpl : collectedBundles)
        {
            for (ExportDescription export : bundleImpl.getCurrentStore().getBundleExportList())
            {
                Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                for (String packageName : export.getPackages())
                {
                    if (targetPackageName.equals(packageName))
                    {
                        Set<BundleImpl> importers = new HashSet<BundleImpl>();
                        BundleClassLoader classLoader = bundleImpl.getClassLoader();

                        if (classLoader != null)
                        {
                            for (Wire wire : classLoader.getWires())
                            {
                                if (packageName.equals(wire.getPackageName())) importers.add(wire.getBundle());
                            }
                        }

                        collectedExports.add(new ExportedPackageImpl(packageName, version, bundleImpl, importers.toArray(new Bundle[importers.size()])));
                    }
                }
            }
        }

        int numExports = collectedExports.size();
        return (numExports == 0 ? null : collectedExports.toArray(new ExportedPackage[numExports]));
    }

    public ExportedPackage getExportedPackage(String targetPackageName)
    {
        Set<BundleImpl> collectedBundles = new HashSet<BundleImpl>();

        for (Bundle b : framework.getBundleManager().getBundles())
        {
            if (b instanceof BundleImpl)
            {
                collectedBundles.add((BundleImpl) b);
            }
        }

        ExportedPackageImpl result = null;
        for (BundleImpl bundleImpl : collectedBundles)
        {
            for (ExportDescription export : bundleImpl.getCurrentStore().getBundleExportList())
            {
                Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                for (String packageName : export.getPackages())
                {
                    if (targetPackageName.equals(packageName))
                    {
                        Set<BundleImpl> importers = new HashSet<BundleImpl>();
                        BundleClassLoader classLoader = bundleImpl.getClassLoader();

                        if (classLoader != null)
                        {
                            for (Wire wire : classLoader.getWires())
                            {
                                if (packageName.equals(wire.getPackageName())) importers.add(wire.getBundle());
                            }
                        }

                        ExportedPackageImpl candidate = new ExportedPackageImpl(packageName, version, bundleImpl, importers.toArray(new Bundle[importers.size()]));
                        if (result == null || result.compareTo(candidate) < 0) result = candidate;
                    }
                }
            }
        }

        return result;
    }

    public void refreshPackages(Bundle[] bundles)
    {
        //todo: consider this autogenerated code
        lastRefreshed = System.currentTimeMillis();
    }

    public boolean resolveBundles(Bundle[] bundles)
    {
        BundleManager manager = framework.getBundleManager();
        boolean result = true;

        manager.lock();
        try
        {
            if (bundles == null) bundles = manager.getBundles();

            for (Bundle bundle : bundles)
            {
                if (bundle instanceof BundleImpl)
                {
                    BundleImpl bundleImpl = (BundleImpl) bundle;
                    if (framework.getFrameworkId() == bundleImpl.getFramework().getFrameworkId() && bundleImpl.getState() == Bundle.INSTALLED)
                    {
                        result = result && manager.resolve(bundleImpl);
                    }
                }
            }
            return result;
        }
        finally
        {
            manager.unlock();
        }
    }

    public RequiredBundle[] getRequiredBundles(String symbolicName)
    {
        return new RequiredBundle[0];  //todo: consider this autogenerated code
    }

    public Bundle[] getBundles(String s, String s1)
    {
        return new Bundle[0];  //todo: consider this autogenerated code
    }

    public Bundle[] getFragments(Bundle bundle)
    {
        return new Bundle[0];  //todo: consider this autogenerated code
    }

    public Bundle[] getHosts(Bundle bundle)
    {
        return new Bundle[0];  //todo: consider this autogenerated code
    }

    public Bundle getBundle(Class aClass)
    {
        return null;  //todo: consider this autogenerated code
    }

    public int getBundleType(Bundle bundle)
    {
        return 0;  //todo: consider this autogenerated code
    }

    public void bundleChanged(BundleEvent event)
    {
        //todo: consider this autogenerated code
    }
}
