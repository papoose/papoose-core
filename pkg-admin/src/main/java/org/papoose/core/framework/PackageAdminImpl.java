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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.AdminPermission;
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
    private final static String CLASSNAME = PackageAdminImpl.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);
    private final Set<WeakReference<ExportedPackageImpl>> exportedPackages = new HashSet<WeakReference<ExportedPackageImpl>>();
    private final Set<WeakReference<RequiredBundleImpl>> requiredBundles = new HashSet<WeakReference<RequiredBundleImpl>>();
    private final Set<Bundle> oldBundles = new HashSet<Bundle>();
    private final Papoose framework;

    public PackageAdminImpl(Papoose framework)
    {
        assert framework != null;

        this.framework = framework;

        if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("framework: " + framework);
    }

    void start() throws InterruptedException
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "start");

        BundleManager manager = framework.getBundleManager();

        try
        {
            manager.writeLock();
        }
        catch (InterruptedException ie)
        {
            LOGGER.log(Level.WARNING, "Wait for write lock interrupted", ie);
            throw ie;
        }

        try
        {
            for (Bundle bundle : manager.getBundles())
            {
                BundleContext context = bundle.getBundleContext();
                if (context != null) context.addBundleListener(this);

                if (bundle.getState() == Bundle.UNINSTALLED || (bundle instanceof BundleImpl && ((BundleImpl) bundle).getNextStore() != null))
                {
                    oldBundles.add(bundle);
                }
            }
        }
        finally
        {
            manager.writeUnlock();
        }

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "start");
    }

    void stop() throws InterruptedException
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "stop");

        oldBundles.clear();

        BundleManager manager = framework.getBundleManager();

        try
        {
            manager.writeLock();
        }
        catch (InterruptedException ie)
        {
            LOGGER.log(Level.WARNING, "Wait for write lock interrupted", ie);
            throw ie;
        }

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
            manager.writeUnlock();
        }

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "stop");
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
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getExportedPackages", bundle);

        Set<BundleImpl> collectedBundles = new HashSet<BundleImpl>();
        Set<ExportedPackageImpl> collectedExports = new HashSet<ExportedPackageImpl>();

        if (bundle == null)
        {
            for (Bundle b : framework.getBundleManager().getBundles())
            {
                if (b instanceof BundleImpl)
                {
                    collectedBundles.add((BundleImpl) b);
                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + b);
                }
            }
        }
        else if (bundle instanceof BundleImpl)
        {
            BundleImpl bundleImpl = (BundleImpl) bundle;
            if (bundleImpl.getFramework() == framework)
            {
                collectedBundles.add(bundleImpl);
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + bundleImpl);
            }
            else
            {
                LOGGER.warning("Bundle does not belong to this framework instance");
            }
        }
        else
        {
            LOGGER.warning("Bundle does not belong to the Papoose framework");
        }

        for (BundleImpl bundleImpl : collectedBundles)
        {
            BundleClassLoader classLoader = bundleImpl.getClassLoader();

            if (classLoader != null)
            {
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Searching bundle " + bundleImpl);

                for (ExportDescription export : bundleImpl.getCurrentStore().getBundleExportList())
                {
                    Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                    for (String packageName : export.getPackages())
                    {
                        Set<BundleImpl> importers = new HashSet<BundleImpl>();

                        if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Searching package " + packageName);

                        for (Wire wire : classLoader.getWires())
                        {
                            if (packageName.equals(wire.getPackageName()))
                            {
                                importers.add(wire.getBundle());
                                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + wire.getBundle());
                            }
                        }

                        ExportedPackageImpl exportedPackage = new ExportedPackageImpl(packageName, version, bundleImpl, importers.toArray(new Bundle[importers.size()]));

                        exportedPackages.add(new WeakReference<ExportedPackageImpl>(exportedPackage));
                        collectedExports.add(exportedPackage);
                    }
                }
            }
            else
            {
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Bundle " + bundleImpl + " seems to be unresolved");
            }
        }

        int numExports = collectedExports.size();
        ExportedPackage[] result = (numExports == 0 ? null : collectedExports.toArray(new ExportedPackage[numExports]));

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getExportedPackages", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public ExportedPackage[] getExportedPackages(String targetPackageName)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getExportedPackages", targetPackageName);

        Set<BundleImpl> collectedBundles = new HashSet<BundleImpl>();
        Set<ExportedPackageImpl> collectedExports = new HashSet<ExportedPackageImpl>();

        for (Bundle b : framework.getBundleManager().getBundles())
        {
            if (b instanceof BundleImpl)
            {
                collectedBundles.add((BundleImpl) b);
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + b);
            }
        }

        for (BundleImpl bundleImpl : collectedBundles)
        {
            BundleClassLoader classLoader = bundleImpl.getClassLoader();

            if (classLoader != null)
            {
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Searching bundle " + bundleImpl);

                for (ExportDescription export : bundleImpl.getCurrentStore().getBundleExportList())
                {
                    Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                    for (String packageName : export.getPackages())
                    {
                        if (targetPackageName.equals(packageName))
                        {
                            Set<BundleImpl> importers = new HashSet<BundleImpl>();
                            for (Wire wire : classLoader.getWires())
                            {
                                if (packageName.equals(wire.getPackageName()))
                                {
                                    importers.add(wire.getBundle());
                                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + wire.getBundle());
                                }
                            }

                            ExportedPackageImpl exportedPackage = new ExportedPackageImpl(packageName, version, bundleImpl, importers.toArray(new Bundle[importers.size()]));

                            exportedPackages.add(new WeakReference<ExportedPackageImpl>(exportedPackage));
                            collectedExports.add(exportedPackage);
                        }
                    }
                }
            }
        }

        int numExports = collectedExports.size();
        ExportedPackage[] result = (numExports == 0 ? null : collectedExports.toArray(new ExportedPackage[numExports]));

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getExportedPackages", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public ExportedPackage getExportedPackage(String targetPackageName)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getExportedPackage", targetPackageName);

        Set<BundleImpl> collectedBundles = new HashSet<BundleImpl>();

        for (Bundle b : framework.getBundleManager().getBundles())
        {
            if (b instanceof BundleImpl)
            {
                collectedBundles.add((BundleImpl) b);
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + b);
            }
        }

        ExportedPackageImpl result = null;
        for (BundleImpl bundleImpl : collectedBundles)
        {
            BundleClassLoader classLoader = bundleImpl.getClassLoader();

            if (classLoader != null)
            {
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Searching bundle " + bundleImpl);

                for (ExportDescription export : bundleImpl.getCurrentStore().getBundleExportList())
                {
                    Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                    for (String packageName : export.getPackages())
                    {
                        if (targetPackageName.equals(packageName))
                        {
                            Set<BundleImpl> importers = new HashSet<BundleImpl>();
                            for (Wire wire : classLoader.getWires())
                            {
                                if (packageName.equals(wire.getPackageName()))
                                {
                                    importers.add(wire.getBundle());
                                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + wire.getBundle());
                                }
                            }

                            ExportedPackageImpl candidate = new ExportedPackageImpl(packageName, version, bundleImpl, importers.toArray(new Bundle[importers.size()]));
                            if (result == null || result.compareTo(candidate) < 0) result = candidate;
                        }
                    }
                }
            }
        }

        if (result != null) exportedPackages.add(new WeakReference<ExportedPackageImpl>(result));

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getExportedPackage", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void refreshPackages(Bundle[] bundles)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "refreshPackages", bundles);

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(framework.getBundleManager().getBundle(0), AdminPermission.RESOLVE));

        final Bundle[] checkedBundles = (bundles == null ? oldBundles.toArray(new Bundle[oldBundles.size()]) : bundles);

        oldBundles.clear();

        framework.getExecutorService().submit(new RefreshPackagesRunnable(framework, checkedBundles));

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "refreshPackages");
    }

    /**
     * {@inheritDoc}
     * Developer's note: This method will not attpempt to refresh any extra
     * bundles that are not specified by the caller.
     */
    public boolean resolveBundles(Bundle[] bundles)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "resolveBundles", bundles);

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(framework.getBundleManager().getBundle(0), AdminPermission.RESOLVE));

        BundleManager manager = framework.getBundleManager();
        boolean result = true;

        if (bundles == null) bundles = manager.getBundles();

        for (Bundle bundle : bundles)
        {
            if (bundle instanceof BundleImpl)
            {
                BundleImpl bundleImpl = (BundleImpl) bundle;
                if (bundleImpl.getFramework() == framework)
                {
                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Testing " + bundleImpl);
                    if (bundleImpl.getState() == Bundle.INSTALLED) result = result && manager.resolve(bundleImpl);
                }
                else
                {
                    LOGGER.warning("Bundle does not belong to this framework instance");
                }
            }
            else
            {
                LOGGER.warning("Bundle does not belong to the Papoose framework");
            }
        }

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "resolveBundles", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public RequiredBundle[] getRequiredBundles(String symbolicName)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getRequiredBundles", symbolicName);

        Bundle[] bundles = framework.getBundleManager().getBundles();

        Map<String, Set<BundleImpl>> requiring = new HashMap<String, Set<BundleImpl>>();
        Map<String, BundleImpl> required = new HashMap<String, BundleImpl>();
        for (Bundle bundle : bundles)
        {
            if (bundle instanceof BundleImpl)
            {
                BundleImpl bundleImpl = (BundleImpl) bundle;

                if (symbolicName == null || bundleImpl.getSymbolicName().equals(symbolicName))
                {
                    required.put(bundleImpl.getSymbolicName(), bundleImpl);
                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Found required bundle " + bundle);
                }

                for (RequireDescription requireDescription : bundleImpl.getCurrentStore().getBundleRequireBundle())
                {
                    if (symbolicName == null || requireDescription.getSymbolName().equals(symbolicName))
                    {
                        Set<BundleImpl> set = requiring.get(requireDescription.getSymbolName());
                        if (set == null)
                        {
                            set = new HashSet<BundleImpl>();
                            requiring.put(requireDescription.getSymbolName(), set);
                        }
                        set.add(bundleImpl);
                        if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added requiring bundle " + bundleImpl);
                    }
                }
            }
        }

        List<RequiredBundle> list = new ArrayList<RequiredBundle>();
        for (String name : requiring.keySet())
        {
            BundleImpl requiredBundle = required.get(name);
            if (requiredBundle != null)
            {
                Set<BundleImpl> set = requiring.get(name);
                RequiredBundleImpl s = new RequiredBundleImpl(name, requiredBundle.getVersion(), requiredBundle, set.toArray(new Bundle[set.size()]));

                list.add(s);
                requiredBundles.add(new WeakReference<RequiredBundleImpl>(s));
            }
        }

        RequiredBundle[] result = list.isEmpty() ? null : list.toArray(new RequiredBundle[list.size()]);

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getRequiredBundles", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle[] getBundles(String symbolicName, String versionRange)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getBundles", new Object[]{ symbolicName, versionRange });

        if (symbolicName == null) throw new IllegalArgumentException("symbolicName cannot be null");

        VersionRange range;
        if (versionRange != null)
        {
            try
            {
                range = VersionRange.parseVersionRange(versionRange);
            }
            catch (Exception e)
            {
                range = VersionRange.DEFAULT_VERSION_RANGE;
                LOGGER.log(Level.WARNING, "Error parsing version range ", e);
            }
        }
        else
        {
            range = VersionRange.DEFAULT_VERSION_RANGE;
        }

        Bundle[] bundles = framework.getBundleManager().getBundles();

        SortedSet<AbstractBundle> sortedSet = new TreeSet<AbstractBundle>(new Comparator<AbstractBundle>()
        {
            public int compare(AbstractBundle o1, AbstractBundle o2)
            {
                return -o1.getVersion().compareTo(o2.getVersion());
            }
        });

        for (Bundle bundle : bundles)
        {
            AbstractBundle abstractBundle = (AbstractBundle) bundle;

            if (abstractBundle.getSymbolicName().equals(symbolicName) && range.includes(abstractBundle.getVersion()))
            {
                sortedSet.add(abstractBundle);
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + abstractBundle);
            }
        }

        Bundle[] result = sortedSet.isEmpty() ? null : sortedSet.toArray(new Bundle[sortedSet.size()]);

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getBundles", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle[] getFragments(Bundle bundle)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getFragments", bundle);

        Bundle[] result = null;
        if (bundle instanceof BundleImpl)
        {
            BundleImpl bundleImpl = (BundleImpl) bundle;
            if (bundleImpl.getFramework() == framework)
            {
                Set<FragmentBundleImpl> fragmentSet = bundleImpl.getFragments();
                if (fragmentSet != null && fragmentSet.size() > 0)
                {
                    result = fragmentSet.toArray(new Bundle[fragmentSet.size()]);
                }
            }
            else
            {
                LOGGER.warning("Bundle does not belong to this framework instance");
            }
        }
        else
        {
            LOGGER.warning("Bundle does not belong to the Papoose framework");
        }

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getFragments", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle[] getHosts(Bundle bundle)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getHosts", bundle);

        Bundle[] result = null;
        if (bundle instanceof FragmentBundleImpl)
        {
            FragmentBundleImpl fragmentBundle = (FragmentBundleImpl) bundle;
            if (fragmentBundle.getFramework() == framework)
            {
                Bundle host = fragmentBundle.getHost();
                if (host != null) result = new Bundle[]{ host };
            }
            else
            {
                LOGGER.warning("Bundle does not belong to this framework instance");
            }
        }
        else
        {
            LOGGER.warning("Bundle does not belong to the Papoose framework");
        }

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getHosts", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle getBundle(Class clazz)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getBundle", clazz);

        Bundle result = null;
        ClassLoader classLoader = clazz.getClassLoader();
        if (classLoader instanceof BundleClassLoader)
        {
            BundleClassLoader bundleClassLoader = (BundleClassLoader) classLoader;
            BundleImpl bundle = bundleClassLoader.getBundle();
            if (bundle.getFramework() == framework)
            {
                result = bundle;
            }
            else
            {
                LOGGER.warning("Class' class loader does not belong to this framework instance");
            }
        }
        else
        {
            LOGGER.warning("Class was not loaded by the Papoose framework");
        }

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getBundle", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int getBundleType(Bundle bundle)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getBundleType", bundle);

        int result = bundle instanceof FragmentBundleImpl ? BUNDLE_TYPE_FRAGMENT : 0;

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getBundleType", result);

        return result;
    }

    public void bundleChanged(BundleEvent event)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "bundleChanged", event);

        Bundle bundle = event.getBundle();
        //todo: consider this autogenerated code

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "bundleChanged");
    }
}
