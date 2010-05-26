/**
 *
 * Copyright 2010 (C) The original author or authors
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
package org.papoose.core.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.xbean.classloader.ResourceLocation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import org.papoose.core.BootClassExtensionGeneration;
import org.papoose.core.BundleClassLoader;
import org.papoose.core.BundleController;
import org.papoose.core.BundleGeneration;
import org.papoose.core.DefaultFilter;
import org.papoose.core.FragmentGeneration;
import org.papoose.core.FrameworkExtensionGeneration;
import org.papoose.core.Generation;
import org.papoose.core.Papoose;
import org.papoose.core.VersionRange;
import org.papoose.core.Wire;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.Extension;
import org.papoose.core.descriptions.FragmentDescription;
import org.papoose.core.descriptions.NativeCodeDescription;
import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.spi.BootClasspathManager;


/**
 * @version $Revision: $ $Date: $
 */
public class BundleUtils
{
    public static void insertSystemClassLoader(Papoose framework, BundleGeneration bundle) throws BundleException
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

        // todo: do we have any fragments?
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

        BundleClassLoader classLoader = new BundleClassLoader(framework,
                                                              bundle,
                                                              wires,
                                                              requiredBundles,
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


    public static void processNativeCodeDescriptions(Papoose framework, ArchiveStore archiveStore) throws BundleException
    {
        if (!archiveStore.getBundleNativeCodeList().isEmpty()) archiveStore.assignNativeCodeDescriptions(resolveNativeCodeDependencies(framework, archiveStore.getBundleNativeCodeList()));
    }

    public static void confirmRequiredExecutionEnvironment(Papoose framework, List<String> bundleExecutionEnvironment) throws BundleException
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

    public static Generation allocateGeneration(Papoose framework, BundleController bundle, ArchiveStore archiveStore) throws BundleException
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
     * Make sure that at least one native code description is valid.
     *
     * @param bundleNativeCodeList the raw list of native code descriptions to be processed
     * @return a list of resolvable native code descriptions
     * @throws BundleException if the method is unable to find at least one valid native code description
     */
    private static  SortedSet<NativeCodeDescription> resolveNativeCodeDependencies(Papoose framework, List<NativeCodeDescription> bundleNativeCodeList) throws BundleException
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

    private BundleUtils() { }
}
