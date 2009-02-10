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

import org.papoose.core.descriptions.ExportDescription;


/**
 * @version $Revision$ $Date$
 */
public class PackageAdminImpl implements PackageAdmin, SynchronousBundleListener
{
    private final static String CLASSNAME = PackageAdminImpl.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);
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

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "start");
    }

    void stop() throws InterruptedException
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "stop");

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

        Set<BundleController> collectedBundles = new HashSet<BundleController>();
        Set<ExportedPackageImpl> collectedExports = new HashSet<ExportedPackageImpl>();

        try
        {
            readLock();

            if (bundle == null)
            {
                for (Bundle b : framework.getBundleManager().getBundles())
                {
                    collectedBundles.add((BundleController) b);
                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + b);
                }
            }
            else if (bundle instanceof BundleController)
            {
                BundleController bundleController = (BundleController) bundle;
                if (bundleController.getFramework() == framework)
                {
                    collectedBundles.add(bundleController);
                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + bundleController);
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

            for (BundleController bundleController : collectedBundles)
            {
                for (Generation generation : bundleController.getGenerations().values())
                {
                    if (generation instanceof BundleGeneration)
                    {
                        BundleGeneration bundleGeneration = (BundleGeneration) generation;
                        BundleClassLoader classLoader = bundleGeneration.getClassLoader();

                        if (classLoader != null)
                        {
                            if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Searching bundle " + bundleController);

                            for (ExportDescription export : bundleGeneration.getArchiveStore().getBundleExportList())
                            {
                                Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                                for (String packageName : export.getPackageNames())
                                {
                                    Set<BundleController> importers = new HashSet<BundleController>();

                                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Searching package " + packageName);

                                    for (Wire wire : classLoader.getWires())
                                    {
                                        if (packageName.equals(wire.getPackageName()))
                                        {
                                            importers.add(wire.getBundleGeneration().getBundleController());
                                            if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + wire.getBundleGeneration().getBundleController());
                                        }
                                    }

                                    ExportedPackageImpl exportedPackage = new ExportedPackageImpl(packageName, version, bundleController, importers.toArray(new Bundle[importers.size()]));

                                    collectedExports.add(exportedPackage);
                                }
                            }
                        }
                        else
                        {
                            if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Bundle generation " + bundleGeneration + " seems to be unresolved");
                        }
                    }
                }
            }
        }
        catch (InterruptedException ie)
        {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for a read lock on bundle manager", ie);
            Thread.currentThread().interrupt();
        }
        finally
        {
            readUnlock();
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

        Set<ExportedPackageImpl> collectedExports = new HashSet<ExportedPackageImpl>();

        try
        {
            readLock();

            for (Bundle bundle : framework.getBundleManager().getBundles())
            {
                BundleController bundleController = (BundleController) bundle;

                for (Generation generation : bundleController.getGenerations().values())
                {
                    if (generation instanceof BundleGeneration)
                    {
                        BundleGeneration bundleGeneration = (BundleGeneration) generation;
                        BundleClassLoader classLoader = bundleGeneration.getClassLoader();

                        if (classLoader != null)
                        {
                            if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Searching bundle " + bundle);

                            for (ExportDescription export : bundleGeneration.getArchiveStore().getBundleExportList())
                            {
                                Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                                for (String packageName : export.getPackageNames())
                                {
                                    if (targetPackageName.equals(packageName))
                                    {
                                        Set<BundleController> importers = new HashSet<BundleController>();
                                        for (Wire wire : classLoader.getWires())
                                        {
                                            if (packageName.equals(wire.getPackageName()))
                                            {
                                                importers.add(wire.getBundleGeneration().getBundleController());
                                                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + wire.getBundleGeneration().getBundleController());
                                            }
                                        }

                                        ExportedPackageImpl exportedPackage = new ExportedPackageImpl(packageName, version, bundle, importers.toArray(new Bundle[importers.size()]));

                                        collectedExports.add(exportedPackage);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (InterruptedException ie)
        {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for a read lock on bundle manager", ie);
            Thread.currentThread().interrupt();
        }
        finally
        {
            readUnlock();
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

        ExportedPackageImpl result = null;

        try
        {
            readLock();

            for (Bundle bundle : framework.getBundleManager().getBundles())
            {
                BundleController bundleController = (BundleController) bundle;
                for (Generation generation : bundleController.getGenerations().values())
                {
                    if (generation instanceof BundleGeneration)
                    {
                        BundleGeneration bundleGeneration = (BundleGeneration) generation;
                        BundleClassLoader classLoader = bundleGeneration.getClassLoader();

                        if (classLoader != null)
                        {
                            if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Searching bundle " + bundleController);

                            for (ExportDescription export : bundleGeneration.getArchiveStore().getBundleExportList())
                            {
                                Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                                for (String packageName : export.getPackageNames())
                                {
                                    if (targetPackageName.equals(packageName))
                                    {
                                        Set<BundleController> importers = new HashSet<BundleController>();
                                        for (Wire wire : classLoader.getWires())
                                        {
                                            if (packageName.equals(wire.getPackageName()))
                                            {
                                                importers.add(wire.getBundleGeneration().getBundleController());
                                                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + wire.getBundleGeneration().getBundleController());
                                            }
                                        }

                                        ExportedPackageImpl candidate = new ExportedPackageImpl(packageName, version, bundleController, importers.toArray(new Bundle[importers.size()]));
                                        if (result == null || result.compareTo(candidate) < 0) result = candidate;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (InterruptedException ie)
        {
            LOGGER.log(Level.WARNING, "Interrupted while waiting for a read lock on bundle manager", ie);
            Thread.currentThread().interrupt();
        }
        finally
        {
            readUnlock();
        }

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

        framework.getExecutorService().submit(new RefreshPackagesRunnable(framework, bundles));

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
        Set<BundleController> bundleControllers = new HashSet<BundleController>();
        boolean result = true;

        if (bundles == null) bundles = manager.getBundles();

        for (Bundle bundle : bundles)
        {
            if (bundle instanceof BundleController)
            {
                BundleController bundleController = (BundleController) bundle;
                if (bundleController.getFramework() == framework)
                {
                    bundleControllers.add(bundleController);
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

        for (BundleController bundleController : bundleControllers)
        {
            if (bundleController.getState() == Bundle.INSTALLED)
            {
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Resolving " + bundleController);
                manager.resolve(bundleController);
            }
            else
            {
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Already resolved or uninstalled " + bundleController);
            }
        }

        for (BundleController bundleController : bundleControllers)
        {
            if (bundleController.getState() == Bundle.INSTALLED)
            {
                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Not resolved " + bundleController);
                result = false;
                break;
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

        Map<BundleGeneration, Set<BundleGeneration>> requiring = new HashMap<BundleGeneration, Set<BundleGeneration>>();

        for (Bundle bundle : bundles)
        {
            BundleController bundleController = (BundleController) bundle;

            for (Generation generation : bundleController.getGenerations().values())
            {
                if (generation instanceof BundleGeneration)
                {
                    BundleGeneration requiringGeneration = (BundleGeneration) generation;

                    if (LOGGER.isLoggable(Level.FINEST) && !requiringGeneration.getRequiredBundles().isEmpty()) LOGGER.finest("Found bundle with required wirings " + generation);

                    for (BundleGeneration requiredGeneration : requiringGeneration.getRequiredBundles())
                    {
                        if (symbolicName == null || requiredGeneration.getSymbolicName().equals(symbolicName))
                        {
                            Set<BundleGeneration> set = requiring.get(requiredGeneration);
                            if (set == null) requiring.put(requiredGeneration, set = new HashSet<BundleGeneration>());
                            set.add(requiringGeneration);
                        }
                    }
                }
            }
        }

        List<RequiredBundle> list = new ArrayList<RequiredBundle>();
        for (BundleGeneration requiredGeneration : requiring.keySet())
        {
            Set<BundleGeneration> set = requiring.get(requiredGeneration);
            Set<BundleController> bundleControllers = new HashSet<BundleController>();

            for (BundleGeneration bundleGeneration : set) bundleControllers.add(bundleGeneration.getBundleController());

            RequiredBundleImpl rb = new RequiredBundleImpl(requiredGeneration.getSymbolicName(), requiredGeneration.getVersion(),
                                                           requiredGeneration.getBundleController(),
                                                           bundleControllers.toArray(new Bundle[bundleControllers.size()]));

            list.add(rb);
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

        SortedSet<Generation> sortedSet = new TreeSet<Generation>(new Comparator<Generation>()
        {
            public int compare(Generation o1, Generation o2)
            {
                return -o1.getVersion().compareTo(o2.getVersion());
            }
        });

        for (Bundle bundle : bundles)
        {
            BundleController bundleController = (BundleController) bundle;

            for (Generation generation : bundleController.getGenerations().values())
            {
                if (generation.getSymbolicName().equals(symbolicName) && range.includes(generation.getVersion()))
                {
                    sortedSet.add(generation);
                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle generation " + generation);
                }
            }
        }

        List<Bundle> list = new ArrayList<Bundle>();
        for (Generation generation : sortedSet)
        {
            BundleController bundleController = generation.getBundleController();
            if (!list.contains(bundleController)) list.add(bundleController);
        }

        Bundle[] result = list.isEmpty() ? null : list.toArray(new Bundle[list.size()]);

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
        if (bundle instanceof BundleController)
        {
            BundleController bundleController = (BundleController) bundle;
            if (bundleController.getFramework() == framework)
            {
                Generation generation = bundleController.getCurrentGeneration();
                if (generation instanceof BundleGeneration)
                {
                    List<FragmentGeneration> fragmentList = ((BundleGeneration) generation).getFragments();
                    if (fragmentList != null && fragmentList.size() > 0)
                    {
                        Set<BundleController> fragmentBundles = new HashSet<BundleController>();
                        for (FragmentGeneration fragment : fragmentList)
                        {
                            fragmentBundles.add(fragment.getBundleController());
                        }

                        result = fragmentBundles.toArray(new Bundle[fragmentBundles.size()]);
                    }
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
        if (bundle instanceof BundleController)
        {
            BundleController fragmentBundle = (BundleController) bundle;
            if (fragmentBundle.getFramework() == framework)
            {
                Set<BundleController> hosts = new HashSet<BundleController>();

                for (Generation generation : fragmentBundle.getGenerations().values())
                {
                    if (generation instanceof FragmentGeneration)
                    {
                        hosts.add(((FragmentGeneration) generation).getHost().getBundleController());
                    }
                }
                if (!hosts.isEmpty()) result = hosts.toArray(new Bundle[hosts.size()]);
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
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getBundleGeneration", clazz);

        Bundle result = null;
        ClassLoader classLoader = clazz.getClassLoader();
        if (classLoader instanceof BundleClassLoader)
        {
            BundleClassLoader bundleClassLoader = (BundleClassLoader) classLoader;
            BundleGeneration generation = bundleClassLoader.getBundle();
            BundleController controller = generation.getBundleController();
            if (controller.getFramework() == framework)
            {
                result = controller;
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

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "getBundleGeneration", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int getBundleType(Bundle bundle)
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "getBundleType", bundle);

        int result = 0;

        if (bundle instanceof BundleController)
        {
            if (((BundleController) bundle).getCurrentGeneration() instanceof FragmentGeneration)
            {
                result = BUNDLE_TYPE_FRAGMENT;
            }
        }

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

    private void readLock() throws InterruptedException
    {
        framework.getBundleManager().readLock();
    }

    private void readUnlock()
    {
        framework.getBundleManager().readUnlock();
    }
}
