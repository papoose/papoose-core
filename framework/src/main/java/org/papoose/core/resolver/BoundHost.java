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
package org.papoose.core.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.papoose.core.BundleGeneration;
import org.papoose.core.FragmentGeneration;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.descriptions.RequireDescription;

/**
 * @version $Revision: 137 $ $Date: 2009-02-09 20:22:41 -0800 (Mon, 09 Feb 2009) $
 */
public class BoundHost extends UnResolved implements CandidateBundle
{
    private final BundleGeneration bundleGeneration;
    private final List<FragmentGeneration> fragments;
    private final List<ImportDescriptionWrapper> imports = new ArrayList<ImportDescriptionWrapper>();
    private final Set<ExportDescriptionWrapper> exports = new HashSet<ExportDescriptionWrapper>();
    private final List<RequireDescription> requireDescriptions = new ArrayList<RequireDescription>();
    private final Set<CandidateWiring> candidateWirings = new HashSet<CandidateWiring>();
    private final List<RequiredBundleWrapper> candidateRequiredBundles = new ArrayList<RequiredBundleWrapper>();
    private final Set<String> removeImports = new HashSet<String>();
    private final Set<String> removeExports = new HashSet<String>();

    public BoundHost(BundleGeneration bundleGeneration, List<FragmentGeneration> fragments) throws IncompatibleException
    {
        super(bundleGeneration);

        assert bundleGeneration != null;
        assert fragments != null;

        this.bundleGeneration = bundleGeneration;
        this.fragments = Collections.unmodifiableList(new ArrayList<FragmentGeneration>(fragments));

        this.requireDescriptions.addAll(bundleGeneration.getArchiveStore().getRequireDescriptions());

        initializeImports();
        initializeExports();
    }

    public BoundHost(BoundHost candidateBundle)
    {
        super(candidateBundle.getBundleGeneration());

        this.bundleGeneration = candidateBundle.getBundleGeneration();
        this.fragments = Collections.unmodifiableList(new ArrayList<FragmentGeneration>(candidateBundle.getFragments()));
        this.imports.addAll(candidateBundle.getImports());
        this.exports.addAll(candidateBundle.getExports());
        this.requireDescriptions.addAll(candidateBundle.getRequireDescriptions());
        this.candidateWirings.addAll(candidateBundle.getWirings());
        this.candidateRequiredBundles.addAll(candidateBundle.getCandidateRequiredBundles());
        this.removeImports.addAll(candidateBundle.getRemoveImports());
        this.removeExports.addAll(candidateBundle.getRemoveExports());
    }

    public BundleGeneration getBundleGeneration()
    {
        return bundleGeneration;
    }

    public List<FragmentGeneration> getFragments()
    {
        return fragments;
    }

    public List<ImportDescriptionWrapper> getImports()
    {
        return imports;
    }

    public Set<ExportDescriptionWrapper> getExports()
    {
        return Collections.unmodifiableSet(exports);
    }

    public List<RequireDescription> getRequireDescriptions()
    {
        return requireDescriptions;
    }

    public void addCandidateRequiredBundle(RequiredBundleWrapper candidate) throws IncompatibleException
    {
        candidateRequiredBundles.add(candidate);
        initializeExports();
    }

    public boolean addCandidateWiring(CandidateWiring candidateWiring)
    {
        return candidateWirings.add(candidateWiring);
    }

    public boolean replaceCandidateWiring(CandidateWiring candidateWiring)
    {
        return candidateWirings.remove(candidateWiring) && candidateWirings.add(candidateWiring);
    }

    public Set<CandidateWiring> getWirings()
    {
        return Collections.unmodifiableSet(candidateWirings);
    }

    public Set<CandidateWiring> getCandidateWirings()
    {
        return Collections.unmodifiableSet(candidateWirings);
    }

    public List<RequiredBundleWrapper> getCandidateRequiredBundles()
    {
        return Collections.unmodifiableList(candidateRequiredBundles);
    }

    public boolean removeImport(String importPackageName)
    {
        assert importPackageName != null;

        for (ImportDescriptionWrapper description : imports)
        {
            if (description.getPackageName().equals(importPackageName))
            {
                assert false;
            }
        }

        return removeImports.add(importPackageName);
    }

    public Set<String> getRemoveImports()
    {
        return Collections.unmodifiableSet(removeImports);
    }

    public boolean removeExport(String exportPackageName)
    {
        assert exportPackageName != null;

        ExportDescriptionWrapper target = null;

        done:
        for (ExportDescriptionWrapper description : exports)
        {
            for (String packageName : description.getExportDescription().getPackageNames())
            {
                if (packageName.equals(exportPackageName))
                {
                    target = description;
                    break done;
                }
            }
        }

        return exports.remove(target) && removeExports.add(exportPackageName);
    }

    public Set<String> getRemoveExports()
    {
        return Collections.unmodifiableSet(removeExports);
    }

    /**
     * TODO: There will be overlap here
     *
     * @throws IncompatibleException if the host is incompatible with its fragments
     */
    private void initializeImports() throws IncompatibleException
    {
        Map<String, List<ImportDescription>> descriptions = new HashMap<String, List<ImportDescription>>();

        for (ImportDescription description : getBundleGeneration().getArchiveStore().getImportDescriptions())
        {
            for (String packageName : description.getPackageNames())
            {
                List<ImportDescription> list = descriptions.get(packageName);
                if (list == null) descriptions.put(packageName, list = new ArrayList<ImportDescription>());
                if (!list.contains(description)) list.add(description);
            }
        }

        for (FragmentGeneration fragmentGeneration : fragments)
        {
            for (ImportDescription description : fragmentGeneration.getArchiveStore().getImportDescriptions())
            {
                for (String packageName : description.getPackageNames())
                {
                    List<ImportDescription> list = descriptions.get(packageName);
                    if (list == null) descriptions.put(packageName, list = new ArrayList<ImportDescription>());
                    if (!list.contains(description)) list.add(description);
                }
            }
        }

        for (String packageName : descriptions.keySet())
        {
            for (ImportDescription description : descriptions.get(packageName))
            {
                imports.add(new ImportDescriptionWrapper(packageName, description));
            }
        }
    }

    /**
     * TODO: There will be overlap here
     *
     * @throws IncompatibleException if the host is incompatible with its fragments
     */
    private void initializeExports() throws IncompatibleException
    {
        exports.clear();

        Map<String, List<ExportDescription>> descriptions = new HashMap<String, List<ExportDescription>>();
        Set<ExportDescription> set = new HashSet<ExportDescription>();

        for (ExportDescription description : getBundleGeneration().getArchiveStore().getExportDescriptions())
        {
            for (String packageName : description.getPackageNames())
            {
                List<ExportDescription> list = descriptions.get(packageName);
                if (list == null) descriptions.put(packageName, list = new ArrayList<ExportDescription>());
                if (!list.contains(description)) list.add(description);
                if (!set.contains(description)) set.add(description);
            }
        }

        for (RequiredBundleWrapper bundleWrapper : candidateRequiredBundles)
        {
            if (bundleWrapper.isReExport())
            {
                BundleGeneration requiredBundle = bundleWrapper.getBundleGeneration();
                for (ExportDescription description : requiredBundle.getArchiveStore().getExportDescriptions())
                {
                    for (String packageName : description.getPackageNames())
                    {
                        List<ExportDescription> list = descriptions.get(packageName);
                        if (list == null) descriptions.put(packageName, list = new ArrayList<ExportDescription>());
                        if (!list.contains(description)) list.add(description);
                        if (!set.contains(description)) set.add(description);
                    }
                }
            }
        }

        for (FragmentGeneration fragmentGeneration : fragments)
        {
            for (ExportDescription description : fragmentGeneration.getArchiveStore().getExportDescriptions())
            {
                for (String packageName : description.getPackageNames())
                {
                    List<ExportDescription> list = descriptions.get(packageName);
                    if (list == null) descriptions.put(packageName, list = new ArrayList<ExportDescription>());
                    if (!list.contains(description)) list.add(description);
                    if (!set.contains(description)) set.add(description);
                }
            }
        }

        for (ExportDescription description : set)
        {
            exports.add(new ExportDescriptionWrapper(description, this));
        }
    }

    @Override
    @SuppressWarnings({ "CloneDoesntCallSuperClone" })
    public Object clone() throws CloneNotSupportedException
    {
        return new BoundHost(this);
    }
}