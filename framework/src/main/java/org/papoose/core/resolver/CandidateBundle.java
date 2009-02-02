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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.papoose.core.BundleGeneration;
import org.papoose.core.ExportDescription;
import org.papoose.core.FragmentGeneration;
import org.papoose.core.Generation;
import org.papoose.core.ImportDescription;
import org.papoose.core.RequireDescription;
import org.papoose.core.util.ToStringCreator;

/**
 * @version $Revision$ $Date$
 */
class CandidateBundle
{
    private final Generation toBeResolved;
    private final BundleGeneration bundleGeneration;
    private final List<FragmentGeneration> fragments;
    private final List<ImportDescriptionWrapper> imports = new ArrayList<ImportDescriptionWrapper>();
    private final Set<ExportDescriptionWrapper> exports = new HashSet<ExportDescriptionWrapper>();
    private final List<RequireDescription> requiredBundles = new ArrayList<RequireDescription>();
    private final Set<CandidateWiring> candidateWirings = new HashSet<CandidateWiring>();
    private final Set<CandidateRequiredBundle> candidateRequiredBundles = new HashSet<CandidateRequiredBundle>();

    public CandidateBundle(Generation toBeResolved, BundleGeneration bundleGeneration, List<FragmentGeneration> fragments)
    {
        this.toBeResolved = toBeResolved;
        this.bundleGeneration = bundleGeneration;
        this.fragments = Collections.unmodifiableList(fragments);

        for (ImportDescription description : bundleGeneration.getArchiveStore().getBundleImportList())
        {
            for (String packageName : description.getPackages()) this.imports.add(new ImportDescriptionWrapper(packageName, description));
        }
        this.requiredBundles.addAll(bundleGeneration.getArchiveStore().getBundleRequireBundle());

        initializeExports();
    }

    public CandidateBundle(CandidateBundle candidateBundle)
    {
        this.toBeResolved = candidateBundle.toBeResolved;
        this.bundleGeneration = candidateBundle.bundleGeneration;
        this.fragments = Collections.unmodifiableList(new ArrayList<FragmentGeneration>(candidateBundle.fragments));
        this.imports.addAll(candidateBundle.imports);
        this.exports.addAll(candidateBundle.exports);
        this.requiredBundles.addAll(candidateBundle.requiredBundles);
        this.candidateWirings.addAll(candidateBundle.candidateWirings);
        this.candidateRequiredBundles.addAll(candidateBundle.candidateRequiredBundles);
    }

    public Generation getToBeResolved()
    {
        return toBeResolved;
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

    public List<RequireDescription> getRequiredBundles()
    {
        return requiredBundles;
    }

    public void addCandidateRequiredBundle(CandidateRequiredBundle candidate)
    {
        candidateRequiredBundles.add(candidate);
        initializeExports();
    }

    public Set<CandidateWiring> getCandidateWirings()
    {
        return candidateWirings;
    }

    public Set<CandidateRequiredBundle> getCandidateRequiredBundles()
    {
        return Collections.unmodifiableSet(candidateRequiredBundles);
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("toBeResolved", toBeResolved);
        creator.append("bundleGeneration", bundleGeneration);
        creator.append("fragments", fragments);
        creator.append("imports", imports);
        creator.append("exports", exports);
        creator.append("requiredBundles", requiredBundles);
        creator.append("candidateWirings", candidateWirings);
        creator.append("candidateRequiredBundles", candidateRequiredBundles);

        return creator.toString();
    }

    private void initializeExports()
    {
        exports.clear();

        for (ExportDescription description : bundleGeneration.getArchiveStore().getBundleExportList())
        {
            for (String packageName : description.getPackageNames())
            {
                exports.add(new ExportDescriptionWrapper(description, bundleGeneration));
            }
        }

        for (CandidateRequiredBundle candidate : candidateRequiredBundles)
        {
            BundleGeneration requiredBundle = candidate.getBundleGeneration();
            for (ExportDescription description : requiredBundle.getArchiveStore().getBundleExportList())
            {
                exports.add(new ExportDescriptionWrapper(description, requiredBundle));
            }
        }

        for (Generation fragment : fragments)
        {
            for (ExportDescription description : fragment.getArchiveStore().getBundleExportList())
            {
                exports.add(new ExportDescriptionWrapper(description, bundleGeneration));
            }
        }
    }
}