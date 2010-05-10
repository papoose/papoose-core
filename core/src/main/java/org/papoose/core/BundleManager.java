/**
 *
 * Copyright 2007-2009 (C) The original author or authors
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

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xbean.classloader.ResourceLocation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;

import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.Extension;
import org.papoose.core.descriptions.FragmentDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.descriptions.LazyActivationDescription;
import org.papoose.core.descriptions.NativeCodeDescription;
import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.spi.BootClasspathManager;
import org.papoose.core.spi.BundleStore;
import org.papoose.core.spi.ProtectionDomainFactory;
import org.papoose.core.spi.Solution;
import org.papoose.core.spi.Store;
import org.papoose.core.util.SecurityUtils;


/**
 * @version $Revision$ $Date$
 */
public class BundleManager
{
    private final static String CLASS_NAME = BundleManager.class.getName();
    private final Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Map<String, BundleController> locations = new HashMap<String, BundleController>();
    private final Map<NameVersionKey, BundleController> nameVersions = new HashMap<NameVersionKey, BundleController>();
    private final Map<Long, BundleController> installedbundles = new HashMap<Long, BundleController>();  // todo: handy but not sure it's consistently maintained
    private final Papoose framework;
    private final Store store;
    private volatile ProtectionDomainFactory protectionDomainFactory;
    private final AtomicLong bundleCounter = new AtomicLong(0);


    public BundleManager(Papoose framework, Store store)
    {
        this.framework = framework;
        this.store = store;
        this.protectionDomainFactory = new DefaultProtectionDomainFactory();
    }

    public Store getStore()
    {
        return store;
    }

    void setProtectionDomainFactory(ProtectionDomainFactory protectionDomainFactory)
    {
        this.protectionDomainFactory = protectionDomainFactory;
    }

    ProtectionDomainFactory getProtectionDomainFactory()
    {
        return protectionDomainFactory;
    }

    public InputStream getInputStreamForCodesource(long bundleId, int generationId) throws IOException
    {
        if (!installedbundles.containsKey(bundleId)) throw new IOException("Unable to find bundle " + bundleId);

        BundleController bundle = installedbundles.get(bundleId);
        Generation generation = (bundle != null ? bundle.getGenerations().get(generationId) : null);
        if (generation != null) return generation.getArchiveStore().getInputStreamForCodeSource();

        throw new IOException("Unable to find archive store generation " + generationId + " for bundle " + bundleId);
    }

    public InputStream getInputStreamForEntry(long bundleId, int generationId, String path) throws IOException
    {
        if (!installedbundles.containsKey(bundleId)) throw new IOException("Unable to find bundle " + bundleId);

        BundleController bundle = installedbundles.get(bundleId);
        Generation generation = (bundle != null ? bundle.getGenerations().get(generationId) : null);
        if (generation != null) return generation.getArchiveStore().getInputStreamForEntry(path);

        throw new IOException("Unable to find archive store generation " + generationId + " for bundle " + bundleId);
    }

    public InputStream getInputStreamForResource(long bundleId, int generationId, int location, String path) throws IOException
    {
        if (!installedbundles.containsKey(bundleId)) throw new IOException("Unable to find bundle " + bundleId);

        BundleController bundle = installedbundles.get(bundleId);
        Generation generation = (bundle != null ? bundle.getGenerations().get(generationId) : null);
        if (generation != null) return generation.getArchiveStore().getInputStreamForResource(location, path);

        throw new IOException("Unable to find archive store generation " + generationId + " for bundle " + bundleId);
    }

    public BundleController getBundle(long bundleId)
    {
        return installedbundles.get(bundleId);
    }

    public BundleController[] getBundles()
    {
        Collection<BundleController> bundles = installedbundles.values();
        return bundles.toArray(new BundleController[bundles.size()]);
    }

    public Bundle installSystemBundle(Version version) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "installSystemBundle");

        if (locations.containsKey(Constants.SYSTEM_BUNDLE_LOCATION)) throw new BundleException("System bundle already installed");

        final long systemBundleId = 0;
        try
        {
            BundleStore bundleStore = store.obtainSystemBundleStore();

            BundleController systemBundle = new SystemBundleController(framework, bundleStore, version);

            NameVersionKey key = new NameVersionKey(systemBundle.getSymbolicName(), version);

            if (nameVersions.containsKey(key)) throw new BundleException("Bundle already registered with name " + key.getSymbolicName() + " and version " + key.getVersion());

            nameVersions.put(key, systemBundle);
            locations.put(Constants.SYSTEM_BUNDLE_LOCATION, systemBundle);
            installedbundles.put(systemBundleId, systemBundle);

            framework.getResolver().added(systemBundle.getCurrentGeneration());

            insertSystemClassLoader((BundleGeneration) systemBundle.getCurrentGeneration());

            bundleStore.markModified();

            LOGGER.exiting(CLASS_NAME, "installSystemBundle", systemBundle);

            return systemBundle;
        }
        catch (BundleException be)
        {
            store.removeBundleStore(systemBundleId);
            throw be;
        }
        catch (Exception e)
        {
            store.removeBundleStore(systemBundleId);
            throw new BundleException("Error occured while loading location " + Constants.SYSTEM_BUNDLE_LOCATION, e);
        }
    }

    private void insertSystemClassLoader(BundleGeneration bundle) throws BundleException
    {
        ArchiveStore currentStore = bundle.getArchiveStore();

        Set<String> exportedPackages = new HashSet<String>();

        for (ExportDescription desc : currentStore.getExportDescriptions())
        {
            exportedPackages.addAll(desc.getPackageNames());
        }

        Set<ArchiveStore> archiveStores = new HashSet<ArchiveStore>();

        archiveStores.add(currentStore);
        for (FragmentGeneration fragment : bundle.getFragments()) archiveStores.add(fragment.getArchiveStore());

        List<ResourceLocation> resourceLocations = new ArrayList<ResourceLocation>();

        for (String element : currentStore.getBundleClassPath())
        {
            ResourceLocation resourceLocation = currentStore.registerClassPathElement(element);
            if (resourceLocation == null)
            {
                for (FragmentGeneration fragment : bundle.getFragments())
                {
                    resourceLocation = fragment.getArchiveStore().registerClassPathElement(element);

                    if (resourceLocation != null) break;
                }
            }
            if (resourceLocation != null) resourceLocations.add(resourceLocation);
        }

        for (FragmentGeneration fragment : bundle.getFragments())
        {
            ArchiveStore archiveStore = fragment.getArchiveStore();

            for (String element : archiveStore.getBundleClassPath())
            {
                ResourceLocation resourceLocation = archiveStore.registerClassPathElement(element);
                if (resourceLocation != null) resourceLocations.add(resourceLocation);
            }
        }

        /**
         * Empty for system bundle
         */
        List<Wire> requiredBundles = new ArrayList<Wire>();
        Set<Wire> wires = new HashSet<Wire>();

        BundleClassLoader classLoader = new BundleClassLoader(bundle.getBundleController().getLocation(),
                                                              framework.getClassLoader(),
                                                              framework,
                                                              bundle,
                                                              wires,
                                                              requiredBundles,
                                                              framework.getBootDelegates(),
                                                              exportedPackages.toArray(new String[exportedPackages.size()]),
                                                              currentStore.getDynamicDescriptions(),
                                                              resourceLocations,
                                                              archiveStores)
        {
            @Override
            protected Class<?> delegateLoadClass(String className) throws ClassNotFoundException
            {
                String packageName = className.substring(0, Math.max(0, className.lastIndexOf('.')));

                for (String exportedPackage : getExportedPackages())
                {
                    if (exportedPackage.equals(packageName)) return getParent().loadClass(className);
                }

                throw new ClassNotFoundException();
            }
        };

        bundle.setClassLoader(classLoader);

        bundle.setState(Bundle.RESOLVED);
    }

    public Bundle installBundle(String location, InputStream inputStream) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "installBundle", new Object[]{ location, inputStream });

        if (locations.containsKey(location)) return locations.get(location);

        long bundleId = bundleCounter.incrementAndGet();

        try
        {
            BundleStore bundleStore = store.allocateBundleStore(bundleId, location);

            ArchiveStore archiveStore = store.allocateArchiveStore(framework, bundleId, inputStream);

            if (!archiveStore.getBundleNativeCodeList().isEmpty()) archiveStore.assignNativeCodeDescriptions(resolveNativeCodeDependencies(archiveStore.getBundleNativeCodeList()));

            confirmRequiredExecutionEnvironment(archiveStore.getBundleRequiredExecutionEnvironment());

            BundleController bundle = new BundleController(framework, bundleStore);

            Generation generation = allocateGeneration(bundle, archiveStore);

            bundle.getGenerations().put(archiveStore.getGeneration(), generation);
            bundle.setCurrentGeneration(generation);

            NameVersionKey key = new NameVersionKey(archiveStore.getBundleSymbolicName(), archiveStore.getBundleVersion());

            if (nameVersions.containsKey(key)) throw new BundleException("Bundle already registered with name " + key.getSymbolicName() + " and version " + key.getVersion());

            SecurityUtils.checkAdminPermission(bundle, AdminPermission.LIFECYCLE);
            if (generation instanceof ExtensionGeneration) SecurityUtils.checkAdminPermission(bundle, AdminPermission.EXTENSIONLIFECYCLE);

            nameVersions.put(key, bundle);
            locations.put(location, bundle);
            installedbundles.put(bundleId, bundle);

            framework.getResolver().added(generation);

            bundleStore.markModified();

            generation.setState(Bundle.INSTALLED);

            fireBundleEvent(new BundleEvent(BundleEvent.INSTALLED, bundle));

            LOGGER.exiting(CLASS_NAME, "installBundle", bundle);

            return bundle;
        }
        catch (BundleException be)
        {
            try
            {
                store.removeBundleStore(bundleId);
            }
            catch (BundleException e)
            {
                throw new FatalError("Unable to remove bundle at location " + location, e);
            }
            throw be;
        }
        catch (SecurityException se)
        {
            try
            {
                store.removeBundleStore(bundleId);
            }
            catch (BundleException be)
            {
                throw new FatalError("Unable to remove bundle at location " + location, be);
            }
            throw se;
        }
        catch (Exception e)
        {
            try
            {
                store.removeBundleStore(bundleId);
            }
            catch (BundleException be)
            {
                throw new FatalError("Unable to remove bundle at location " + location, be);
            }
            throw new BundleException("Error occured while loading location " + location, e);
        }
    }

    /**
     * Make sure that at least one native code description is valid.
     *
     * @param bundleNativeCodeList the raw list of native code descriptions to be processed
     * @return a list of resolvable native code descriptions
     * @throws BundleException if the method is unable to find at least one valid native code description
     */
    // todo: util candidate
    private SortedSet<NativeCodeDescription> resolveNativeCodeDependencies(List<NativeCodeDescription> bundleNativeCodeList) throws BundleException
    {
        SortedSet<NativeCodeDescription> set = new TreeSet<NativeCodeDescription>();

        if (!bundleNativeCodeList.isEmpty())
        {
            VersionRange osVersionRange = VersionRange.parseVersionRange((String) framework.getProperty(Constants.FRAMEWORK_OS_VERSION));

            nextDescription:
            for (NativeCodeDescription description : bundleNativeCodeList)
            {
                Map<String, Object> parameters = description.getParameters();
                for (String key : parameters.keySet())
                {
                    if ("osname".equals(key) && !framework.getProperty(Constants.FRAMEWORK_OS_NAME).equals(parameters.get(key)))
                    {
                        continue nextDescription;
                    }
                    else if ("processor".equals(key) && !framework.getProperty(Constants.FRAMEWORK_PROCESSOR).equals(parameters.get(key)))
                    {
                        continue nextDescription;
                    }
                    else if ("osversion".equals(key))
                    {
                        if (!osVersionRange.includes(description.getOsVersion())) continue nextDescription;
                    }
                    else if ("language".equals(key) && !framework.getProperty(Constants.FRAMEWORK_LANGUAGE).equals(description.getLanguage()))
                    {
                        continue nextDescription;
                    }
                    else if ("selection-filter".equals(key))
                    {
                        try
                        {
                            Filter selectionFilter = new DefaultFilter(framework.getParser().parse((String) parameters.get(key)));
                            if (!selectionFilter.match(framework.getProperties())) continue nextDescription;
                        }
                        catch (InvalidSyntaxException ise)
                        {
                            throw new BundleException("Invalid selection filter", ise);
                        }
                    }
                }

                set.add(description);
            }
        }

        return set;
    }

    // todo: util candidate
    protected void confirmRequiredExecutionEnvironment(List<String> bundleExecutionEnvironment) throws BundleException
    {
        if (!bundleExecutionEnvironment.isEmpty())
        {
            String string = (String) framework.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
            if (string == null) throw new BundleException(Constants.FRAMEWORK_EXECUTIONENVIRONMENT + " not set");
            String[] environments = string.split(",");

            nextRequirement:
            for (String requirement : bundleExecutionEnvironment)
            {
                for (String environment : environments)
                {
                    if (requirement.equals(environment)) continue nextRequirement;
                }
                throw new BundleException("Missing required execution environment: " + requirement);
            }
        }
    }

    public boolean resolve(Bundle target)
    {
        if (target.getState() != Bundle.INSTALLED) return false;

        try
        {
            BundleController bundleController = (BundleController) target;
            Set<Solution> solutions = framework.getResolver().resolve(bundleController.getCurrentGeneration());

            if (solutions.isEmpty()) return false;

            for (Solution solution : solutions)
            {
                finishResolution(solution);
            }
        }
        catch (BundleException be)
        {
            LOGGER.log(Level.WARNING, "Unable to resolve bundle", be);
            return false;
        }
        return true;
    }

    public Wire resolve(Bundle target, ImportDescription importDescription)
    {
        assert target != null;
        assert importDescription != null;

        Wire result = null;

        try
        {
            BundleController targetController = (BundleController) target;
            BundleGeneration targetGeneration = (BundleGeneration) targetController.getCurrentGeneration();
            Set<Solution> solutions = framework.getResolver().resolve(targetGeneration, importDescription);

            if (solutions.isEmpty()) return null;

            for (Solution solution : solutions)
            {
                BundleGeneration solutionGeneration = solution.getBundle();

                if (solutionGeneration == targetGeneration)
                {
                    result = solution.getWires().iterator().next();
                }
                else
                {
                    finishResolution(solution);
                }
            }

            assert result != null;
        }
        catch (BundleException be)
        {
            LOGGER.log(Level.FINEST, "Unable to resolve bundle's dynamic import", be);
        }

        return result;
    }

    private void finishResolution(Solution solution) throws BundleException
    {
        BundleGeneration bundleGeneration = solution.getBundle();
        ArchiveStore currentStore = bundleGeneration.getArchiveStore();
        Set<Wire> wires = solution.getWires();

        for (Wire wire : wires)
        {
            wire.getExportDescription().incrementReferenceCount();
        }

        Set<String> exportedPackages = new HashSet<String>();
        for (ExportDescription desc : currentStore.getExportDescriptions())
        {
            exportedPackages.addAll(desc.getPackageNames());
        }

        List<Wire> requiredBundles = new ArrayList<Wire>();
        for (Solution.RequiredBundleWrapper wrapper : solution.getRequiredBundles())
        {
            Wire wire = wrapper.getWire();

            requiredBundles.add(wire);
            bundleGeneration.getRequiredBundles().add(wire.getBundleGeneration());

            if (wrapper.isReExport())
            {
                exportedPackages.add(wire.getPackageName());
            }
        }

        Set<ArchiveStore> archiveStores = new HashSet<ArchiveStore>();

        archiveStores.add(currentStore);
        for (FragmentGeneration fragment : solution.getFragments()) archiveStores.add(fragment.getArchiveStore());

        List<ResourceLocation> resourceLocations = new ArrayList<ResourceLocation>();

        for (String element : currentStore.getBundleClassPath())
        {
            ResourceLocation resourceLocation = currentStore.registerClassPathElement(element);
            if (resourceLocation == null)
            {
                for (FragmentGeneration fragment : solution.getFragments())
                {
                    resourceLocation = fragment.getArchiveStore().registerClassPathElement(element);

                    if (resourceLocation != null) break;
                }
            }
            if (resourceLocation != null) resourceLocations.add(resourceLocation);
        }

        for (FragmentGeneration fragment : solution.getFragments())
        {
            ArchiveStore archiveStore = fragment.getArchiveStore();

            for (String element : archiveStore.getBundleClassPath())
            {
                ResourceLocation resourceLocation = archiveStore.registerClassPathElement(element);
                if (resourceLocation != null) resourceLocations.add(resourceLocation);
            }

            fragment.setHost(bundleGeneration);
            bundleGeneration.getFragments().add(fragment);
        }

        BundleClassLoader classLoader = new BundleClassLoader(bundleGeneration.getBundleController().getLocation(),  //todo: remove?
                                                              framework.getClassLoader(),  //todo: remove?
                                                              framework,
                                                              bundleGeneration,
                                                              wires,
                                                              requiredBundles,
                                                              framework.getBootDelegates(),  //todo: remove?
                                                              exportedPackages.toArray(new String[exportedPackages.size()]),
                                                              currentStore.getDynamicDescriptions(),
                                                              resourceLocations,
                                                              archiveStores);

        bundleGeneration.setClassLoader(classLoader);

        bundleGeneration.setState(Bundle.RESOLVED);

        fireBundleEvent(new BundleEvent(BundleEvent.RESOLVED, bundleGeneration.getBundleController()));
    }

    public void loadBundles()
    {
        List<BundleStore> bundleStores;
        try
        {
            bundleStores = store.loadBundleStores();
        }
        catch (PapooseException e)
        {
            throw new FatalError("Unable to load bundle stores", e);
        }

        for (BundleStore bundleStore : bundleStores)
        {
            try
            {
                while (bundleCounter.get() < bundleStore.getBundleId()) bundleCounter.incrementAndGet();

                long bundleId = bundleStore.getBundleId();

                String location = bundleStore.getLocation();
                ArchiveStore archiveStore = store.loadArchiveStore(framework, bundleId);

                // todo: this always seemed kinda a wacky way to do this
                archiveStore.assignNativeCodeDescriptions(resolveNativeCodeDependencies(archiveStore.getBundleNativeCodeList()));

                confirmRequiredExecutionEnvironment(archiveStore.getBundleRequiredExecutionEnvironment());

                BundleController bundle = new BundleController(framework, bundleStore);

                Generation generation = allocateGeneration(bundle, archiveStore);

                bundle.getGenerations().put(archiveStore.getGeneration(), generation);
                bundle.setCurrentGeneration(generation);

                NameVersionKey key = new NameVersionKey(archiveStore.getBundleSymbolicName(), archiveStore.getBundleVersion());

                if (nameVersions.containsKey(key)) throw new BundleException("Bundle already registered with name " + key.getSymbolicName() + " and version " + key.getVersion());

                nameVersions.put(key, bundle);
                locations.put(location, bundle);
                installedbundles.put(bundleId, bundle);

                bundleStore.markModified();

                generation.setState(Bundle.INSTALLED);

                fireBundleEvent(new BundleEvent(BundleEvent.INSTALLED, bundle));
            }
            catch (BundleException e)
            {
                e.printStackTrace();  //todo: consider this autogenerated code
            }
        }
    }

    public void unloadBundles()
    {
        nameVersions.clear();
        locations.clear();
        installedbundles.clear();

        bundleCounter.set(0);
    }

    public void startBundles()
    {
        List<BundleStore> bundleStores;
        try
        {
            bundleStores = store.loadBundleStores();
        }
        catch (PapooseException e)
        {
            throw new FatalError("Unable to load bundle stores", e);
        }

        for (BundleStore bundleStore : bundleStores)
        {
            try
            {
                long bundleId = bundleStore.getBundleId();

                if (bundleId == 0) continue;

                BundleController bundle = installedbundles.get(bundleId);
                Generation generation = bundle.getCurrentGeneration();

                if (generation instanceof BundleGeneration)
                {
                    if (generation.getArchiveStore().getBundleActivatorClass() != null)
                    {
                        framework.getStartManager().start((BundleGeneration) generation, 0); // todo: this is probably wrong
                    }
                }
            }
            catch (BundleException e)
            {
                e.printStackTrace();  //todo: consider this autogenerated code
            }
        }
    }

    // todo: util candidate
    public void beginStart(BundleGeneration bundleGeneration, int options) throws BundleException
    {
        try
        {
            if (!bundleGeneration.getStarting().tryLock(10000, TimeUnit.MILLISECONDS)) throw new BundleException("Timeout waiting for bundle to start");

            if (bundleGeneration.getState() == Bundle.ACTIVE) return;

            BundleController bundleController = bundleGeneration.getBundleController();
            ArchiveStore archiveStore = bundleGeneration.getArchiveStore();
            LazyActivationDescription lazyActivationDescription = archiveStore.getLazyActivationDescription();
            if (options != Bundle.START_TRANSIENT)
            {
                if (lazyActivationDescription.isLazyActivation())
                {
                    bundleController.setAutostart(AutostartSetting.ACTIVATION_POLICY);
                }
                else
                {
                    bundleController.setAutostart(AutostartSetting.EAGER);
                }
            }

            if (bundleGeneration.getState() == Bundle.INSTALLED && !resolve(bundleGeneration.getBundleController())) throw new BundleException("Unable to resolve bundle");

            if (lazyActivationDescription.isLazyActivation())
            {
                if (bundleGeneration.getState() != Bundle.STARTING)
                {
                    bundleGeneration.setState(Bundle.STARTING);

                    fireBundleEvent(new BundleEvent(BundleEvent.LAZY_ACTIVATION, bundleController));

                    return;
                }

                if (options == Bundle.START_ACTIVATION_POLICY) return;
            }

            bundleGeneration.setState(Bundle.STARTING);

            performActivation(bundleGeneration);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Interrupted while waiting to start bundle", ie);
        }
        finally
        {
            bundleGeneration.getStarting().unlock();
        }
    }

    // todo: util candidate
    public void performActivation(BundleGeneration bundleGeneration) throws BundleException
    {
        try
        {
            if (!bundleGeneration.getStarting().tryLock(10000, TimeUnit.MILLISECONDS)) throw new BundleException("Timeout waiting for bundle to start");

            if (bundleGeneration.getState() != Bundle.STARTING) return;

            final BundleController bundleController = bundleGeneration.getBundleController();
            ArchiveStore archiveStore = bundleGeneration.getArchiveStore();

            fireBundleEvent(new BundleEvent(BundleEvent.STARTING, bundleController));

            bundleGeneration.getClassLoader().setLazyActivation(false);

            try
            {
                String bundleActivatorClassName = archiveStore.getBundleActivatorClass();
                if (bundleActivatorClassName != null)
                {
                    Class bundleActivatorClass = bundleGeneration.getClassLoader().loadClass(bundleActivatorClassName);

                    if (bundleActivatorClass == null) throw new BundleException("Bundle activator class " + bundleActivatorClassName + " not found");

                    final BundleActivator bundleActivator = (BundleActivator) bundleActivatorClass.newInstance();

                    bundleController.setBundleActivator(bundleActivator);

                    SecurityUtils.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
                    {
                        public Void run() throws Exception
                        {
                            bundleActivator.start(bundleController.getBundleContext());
                            return null;
                        }
                    },
                                                              framework.getAcc());
                }

                bundleGeneration.setState(Bundle.ACTIVE);

                fireBundleEvent(new BundleEvent(BundleEvent.STARTED, bundleController));

                return;
            }
            catch (ClassNotFoundException cnfe)
            {
                LOGGER.log(Level.WARNING, "Unable to load bundle activator class", cnfe);
            }
            catch (InstantiationException ie)
            {
                LOGGER.log(Level.WARNING, "Unable to instantiate bundle activator class", ie);
            }
            catch (IllegalAccessException iae)
            {
                LOGGER.log(Level.WARNING, "Unable to instantiate bundle activator class", iae);
            }
            catch (ClassCastException cce)
            {
                LOGGER.log(Level.WARNING, "Bundle activator not an instance of BundleActivator", cce);
            }
            catch (Throwable t)
            {
                LOGGER.log(Level.WARNING, "Unable to start bundle activator class", t);
            }

            bundleGeneration.setState(Bundle.STOPPING);

            fireBundleEvent(new BundleEvent(BundleEvent.STOPPING, bundleController));

            bundleGeneration.setState(Bundle.RESOLVED);

            fireBundleEvent(new BundleEvent(BundleEvent.STOPPED, bundleController));
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Interrupted while waiting to start bundle", ie);
        }
        finally
        {
            bundleGeneration.getStarting().unlock();
        }
    }

    // todo: util candidate
    public void beginStop(BundleGeneration bundleGeneration, int options) throws BundleException
    {
        try
        {
            if (!bundleGeneration.getStarting().tryLock(10000, TimeUnit.MILLISECONDS)) throw new BundleException("Timeout waiting for bundle to stop");

            BundleController bundleController = bundleGeneration.getBundleController();

            if (options != Bundle.STOP_TRANSIENT)
            {
                bundleController.setAutostart(AutostartSetting.STOPPED);
            }

            if (bundleGeneration.getState() != Bundle.ACTIVE) return;

            bundleGeneration.setState(Bundle.STOPPING);

            fireBundleEvent(new BundleEvent(BundleEvent.STOPPING, bundleController));

            Throwable throwable = null;
            try
            {
                performDeactivation(bundleController);
            }
            catch (Throwable t)
            {
                throwable = t;
            }

            framework.getServiceRegistry().unregister(bundleController);

            bundleController.clearListeners();

            bundleGeneration.setState(Bundle.RESOLVED);

            fireBundleEvent(new BundleEvent(BundleEvent.STOPPED, bundleController));

            if (throwable != null) throw new BundleException("Errors stopping bundle", throwable);
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Interrupted while waiting to stop bundle", ie);
        }
        finally
        {
            bundleGeneration.getStarting().unlock();
        }
    }

    // todo: util candidate
    public void performDeactivation(final BundleController bundleController) throws Exception
    {
        final BundleActivator bundleActivator = bundleController.getBundleActivator();

        if (bundleActivator != null)
        {
            SecurityUtils.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
            {
                public Void run() throws Exception
                {
                    bundleActivator.stop(bundleController.getBundleContext());
                    return null;
                }
            },
                                                      framework.getAcc());
            bundleController.setBundleActivator(null);
        }
    }

    public void uninstall(Bundle bundle) throws BundleException
    {
        BundleController bundleController = (BundleController) bundle;
        BundleGeneration bundleGeneration = (BundleGeneration) bundleController.getCurrentGeneration();

        try
        {
            if (!bundleGeneration.getStarting().tryLock(10000, TimeUnit.MILLISECONDS)) throw new BundleException("Timeout waiting for bundle to start");

            if (bundleGeneration.getState() == Bundle.ACTIVE)
            {
                try
                {
                    beginStop(bundleGeneration, 0);
                }
                catch (Throwable throwable)
                {
                    fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, throwable));
                }
            }

            Set<Generation> unused = new HashSet<Generation>(bundleController.getGenerations().values());
            boolean remove = true;
            for (Generation generation : bundleController.getGenerations().values())
            {
                boolean inUse = false;
                for (ExportDescription description : generation.getArchiveStore().getExportDescriptions())
                {
                    if (description.getReferenceCount() > 0)
                    {
                        inUse = true;
                        unused.remove(generation);
                        break;
                    }
                }
                if (inUse) remove = false;
            }

            for (Generation generation : unused)
            {
                framework.getResolver().removed(generation);
            }

            if (remove)
            {
                String location = null;
                for (Map.Entry<String, BundleController> entry : locations.entrySet())
                {
                    if (entry.getValue() == bundleController)
                    {
                        location = entry.getKey();
                        break;
                    }
                }
                if (location != null)
                {
                    locations.remove(location);
                }
                else
                {
                    assert false;
                }

                Set<NameVersionKey> keys = new HashSet<NameVersionKey>();
                for (Map.Entry<NameVersionKey, BundleController> entry : nameVersions.entrySet())
                {
                    if (entry.getValue() == bundleController) keys.add(entry.getKey());
                }
                for (NameVersionKey key : keys) nameVersions.remove(key);

                Long bundleId = null;
                for (Map.Entry<Long, BundleController> entry : installedbundles.entrySet())
                {
                    if (entry.getValue() == bundleController)
                    {
                        bundleId = entry.getKey();
                        break;
                    }
                }
                if (bundleId != null)
                {
                    installedbundles.remove(bundleId);
                }
                else
                {
                    assert false;
                }

                bundleId = null;
                for (Map.Entry<Long, BundleController> entry : installedbundles.entrySet())
                {
                    if (entry.getValue() == bundleController)
                    {
                        bundleId = entry.getKey();
                        break;
                    }
                }
                if (bundleId != null)
                {
                    installedbundles.remove(bundleId);
                }
                else
                {
                    assert false;
                }

                store.removeBundleStore(bundleId);
            }

            bundleGeneration.setState(Bundle.UNINSTALLED);

            fireBundleEvent(new BundleEvent(BundleEvent.UNINSTALLED, bundleController));
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Interrupted while waiting to start bundle", ie);
        }
        finally
        {
            bundleGeneration.getStarting().unlock();
        }
    }

    public void fireBundleEvent(final BundleEvent event)
    {
        Collection<BundleController> bundles = installedbundles.values();

        for (BundleController bundle : bundles)
        {
            Set<SynchronousBundleListener> syncBundleListeners = bundle.getSyncBundleListeners();
            if (syncBundleListeners != null)
            {
                for (BundleListener listener : syncBundleListeners)
                {
                    try
                    {
                        SecurityUtils.bundleChanged(listener, event, framework.getAcc());
                    }
                    catch (Throwable throwable)
                    {
                        fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, throwable));
                    }
                }
            }
        }

        if ((event.getType() & (BundleEvent.LAZY_ACTIVATION | BundleEvent.STARTING | BundleEvent.STOPPING)) == 0)
        {
            for (final BundleController bundle : bundles)
            {
                Set<BundleListener> bundleListeners = bundle.getBundleListeners();
                if (bundleListeners != null)
                {
                    for (final BundleListener listener : bundleListeners)
                    {
                        bundle.getSerialExecutor().execute(new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    SecurityUtils.bundleChanged(listener, event, framework.getAcc());
                                }
                                catch (Throwable throwable)
                                {
                                    fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, throwable));
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    public void fireFrameworkEvent(final FrameworkEvent event)
    {
        for (final BundleController bundle : installedbundles.values())
        {
            Set<FrameworkListener> frameworkListeners = bundle.getFrameworkListeners();
            if (frameworkListeners != null)
            {
                for (final FrameworkListener listener : frameworkListeners)
                {
                    bundle.getSerialExecutor().execute(new Runnable()
                    {
                        public void run()
                        {
                            try
                            {
                                SecurityUtils.frameworkEvent(listener, event, framework.getAcc());
                            }
                            catch (Throwable throwable)
                            {
                                if (event.getType() != FrameworkEvent.ERROR)
                                {
                                    fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, throwable));
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    public void fireServiceEvent(ServiceEvent event)
    {
        ServiceReference reference = event.getServiceReference();
        String[] classes = (String[]) reference.getProperty(Constants.OBJECTCLASS);

        for (BundleController bundle : installedbundles.values())
        {
            for (String clazz : classes)
            {
                if (!bundle.hasPermission(new ServicePermission(clazz, ServicePermission.GET))) continue;

                if (bundle.getAllServiceListeners() != null) fireServiceEvent(event, bundle.getAllServiceListeners(), bundle);

                if (!reference.isAssignableTo(bundle, clazz)) continue;

                if (bundle.getServiceListeners() != null) fireServiceEvent(event, bundle.getServiceListeners(), bundle);
            }
        }
    }

    protected void fireServiceEvent(ServiceEvent event, Set<ServiceListener> listeners, Bundle bundle)
    {
        for (final ServiceListener listener : listeners)
        {
            try
            {
                SecurityUtils.serviceEvent(listener, event, framework.getAcc());
            }
            catch (Throwable throwable)
            {
                fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, throwable));
            }
        }
    }

    public void readLock() throws InterruptedException
    {
        readWriteLock.readLock().lockInterruptibly();
    }

    public void readUnlock()
    {
        readWriteLock.readLock().unlock();
    }

    public void writeLock() throws InterruptedException
    {
        readWriteLock.writeLock().lockInterruptibly();
    }

    public void writeUnlock()
    {
        readWriteLock.writeLock().unlock();
    }

    // todo: util candidate
    private Generation allocateGeneration(BundleController bundle, ArchiveStore archiveStore) throws BundleException
    {
        if (archiveStore.getFragmentDescription() != null)
        {
            FragmentDescription description = archiveStore.getFragmentDescription();
            if (description.getExtension() == null)
            {
                return new FragmentGeneration(bundle, archiveStore);
            }
            else
            {
                if (!archiveStore.getImportDescriptions().isEmpty()) throw new BundleException("Extension bundles cannot import packages");
                if (!archiveStore.getRequireDescriptions().isEmpty()) throw new BundleException("Extension bundles cannot require other bundles");
                if (!archiveStore.getBundleNativeCodeList().isEmpty()) throw new BundleException("Extension bundles cannot load native code");
                if (!archiveStore.getDynamicDescriptions().isEmpty()) throw new BundleException("Extension bundles cannot dynamically import packages");
                if (archiveStore.getBundleActivatorClass() != null) throw new BundleException("Extension bundles cannot have a bundle activator");

                if (archiveStore.getFragmentDescription().getExtension() == Extension.FRAMEWORK)
                {
                    return new FrameworkExtensionGeneration(bundle, archiveStore);
                }
                else
                {
                    BootClasspathManager manager = (BootClasspathManager) framework.getProperty(BootClasspathManager.BOOT_CLASSPATH_MANAGER);
                    if (manager == null || !manager.isSupported()) throw new BundleException("Boot classpath extensions not supported in this framework configuration");

                    return new BootClassExtensionGeneration(bundle, archiveStore);
                }
            }
        }
        else
        {
            return new BundleGeneration(bundle, archiveStore);
        }
    }

    /**
     * Simple class used as a key to make sure that symbolic name/key
     * combinations are unique
     */
    private static class NameVersionKey
    {
        private final String symbolicName;
        private final Version version;

        private NameVersionKey(String symbolicName, Version version)
        {
            assert symbolicName != null && symbolicName.trim().length() != 0;
            assert version != null;

            this.symbolicName = symbolicName;
            this.version = version;
        }

        public String getSymbolicName()
        {
            return symbolicName;
        }

        public Version getVersion()
        {
            return version;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NameVersionKey that = (NameVersionKey) o;

            //noinspection SimplifiableIfStatement
            if (!symbolicName.equals(that.symbolicName)) return false;
            return version.equals(that.version);
        }

        @Override
        public int hashCode()
        {
            int result = symbolicName.hashCode();
            result = 31 * result + version.hashCode();
            return result;
        }

        @Override
        public String toString()
        {
            return "[" + symbolicName + ", " + version + "]";
        }
    }
}
