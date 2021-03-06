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
import java.util.Arrays;
import java.util.Collections;
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
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;

import org.papoose.core.descriptions.ExportDescription;


/**
 *
 */
public class PackageAdminImpl implements PackageAdmin
{
    private final static String CLASS_NAME = PackageAdminImpl.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private volatile Papoose framework;
    private volatile ServiceRegistration serviceRegistration;

    public void start(Papoose framework)
    {
        LOGGER.entering(CLASS_NAME, "start", framework);

        this.framework = framework;

        BundleContext context = framework.getSystemBundleContext();

        this.serviceRegistration = context.registerService(PackageAdmin.class.getName(), this, null);

        LOGGER.exiting(CLASS_NAME, "start");
    }

    public void stop()
    {
        LOGGER.entering(CLASS_NAME, "stop");

        serviceRegistration.unregister();

        serviceRegistration = null;
        framework = null;

        LOGGER.exiting(CLASS_NAME, "stop");
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
        LOGGER.entering(CLASS_NAME, "getExportedPackages", bundle);

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

            final List<BundleController> bundles = Collections.unmodifiableList(Arrays.asList(framework.getBundleManager().getBundles()));

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

                            for (ExportDescription export : bundleGeneration.getArchiveStore().getExportDescriptions())
                            {
                                Version version = (Version) export.getParameters().get(Constants.VERSION_ATTRIBUTE);
                                for (String packageName : export.getPackageNames())
                                {
                                    Set<BundleController> importers = new HashSet<BundleController>();

                                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Searching package " + packageName);

                                    for (BundleController controller : bundles)
                                    {
                                        Generation g = controller.getCurrentGeneration();

                                        if (controller != bundleController && g instanceof BundleGeneration)
                                        {
                                            BundleGeneration bg = (BundleGeneration) g;
                                            BundleClassLoader cl = bg.getClassLoader();

                                            for (Wire wire : cl.getWires())
                                            {
                                                if (packageName.equals(wire.getPackageName()) && wire.getBundleGeneration() == bundleGeneration)
                                                {
                                                    importers.add(controller);
                                                    if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Added bundle " + controller);
                                                }
                                            }
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

        LOGGER.exiting(CLASS_NAME, "getExportedPackages", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public ExportedPackage[] getExportedPackages(String targetPackageName)
    {
        LOGGER.entering(CLASS_NAME, "getExportedPackages", targetPackageName);

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

                            for (ExportDescription export : bundleGeneration.getArchiveStore().getExportDescriptions())
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

        LOGGER.exiting(CLASS_NAME, "getExportedPackages", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public ExportedPackage getExportedPackage(String targetPackageName)
    {
        LOGGER.entering(CLASS_NAME, "getExportedPackage", targetPackageName);

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

                            for (ExportDescription export : bundleGeneration.getArchiveStore().getExportDescriptions())
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

        LOGGER.exiting(CLASS_NAME, "getExportedPackage", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void refreshPackages(Bundle[] bundles)
    {
        LOGGER.entering(CLASS_NAME, "refreshPackages", bundles);

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(framework.getBundleManager().getBundle(0), AdminPermission.RESOLVE));

        framework.getExecutorService().submit(new RefreshPackagesRunnable(framework, bundles));

        LOGGER.exiting(CLASS_NAME, "refreshPackages");
    }

    /**
     * {@inheritDoc}
     * Developer's note: This method will not attpempt to refresh any extra
     * bundles that are not specified by the caller.
     */
    public boolean resolveBundles(Bundle[] bundles)
    {
        LOGGER.entering(CLASS_NAME, "resolveBundles", bundles);

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

        LOGGER.exiting(CLASS_NAME, "resolveBundles", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public RequiredBundle[] getRequiredBundles(String symbolicName)
    {
        LOGGER.entering(CLASS_NAME, "getRequiredBundles", symbolicName);

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

        LOGGER.exiting(CLASS_NAME, "getRequiredBundles", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle[] getBundles(String symbolicName, String versionRange)
    {
        LOGGER.entering(CLASS_NAME, "getBundles", new Object[]{ symbolicName, versionRange });

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

        LOGGER.exiting(CLASS_NAME, "getBundles", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle[] getFragments(Bundle bundle)
    {
        LOGGER.entering(CLASS_NAME, "getFragments", bundle);

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

        LOGGER.exiting(CLASS_NAME, "getFragments", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle[] getHosts(Bundle bundle)
    {
        LOGGER.entering(CLASS_NAME, "getHosts", bundle);

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

        LOGGER.exiting(CLASS_NAME, "getHosts", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle getBundle(Class clazz)
    {
        LOGGER.entering(CLASS_NAME, "getBundleGeneration", clazz);

        Bundle result = null;
        ClassLoader classLoader = clazz.getClassLoader();
        if (classLoader instanceof BundleClassLoader)
        {
            BundleClassLoader bundleClassLoader = (BundleClassLoader) classLoader;
            BundleGeneration generation = bundleClassLoader.getBundleGeneration();
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

        LOGGER.exiting(CLASS_NAME, "getBundleGeneration", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public int getBundleType(Bundle bundle)
    {
        LOGGER.entering(CLASS_NAME, "getBundleType", bundle);

        int result = 0;

        if (bundle instanceof BundleController)
        {
            if (((BundleController) bundle).getCurrentGeneration() instanceof FragmentGeneration)
            {
                result = BUNDLE_TYPE_FRAGMENT;
            }
        }

        LOGGER.exiting(CLASS_NAME, "getBundleType", result);

        return result;
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
