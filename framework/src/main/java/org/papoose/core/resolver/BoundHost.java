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
import org.papoose.core.Generation;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.descriptions.RequireDescription;
import org.papoose.core.util.ToStringCreator;

/**
 * @version $Revision: 137 $ $Date: 2009-02-09 20:22:41 -0800 (Mon, 09 Feb 2009) $
 */
class BoundHost extends UnResolved implements CandidateBundle
{
    private final BundleGeneration bundleGeneration;
    private final List<BoundFragment> fragments;
    private final List<ImportDescriptionWrapper> imports = new ArrayList<ImportDescriptionWrapper>();
    private final Set<ExportDescriptionWrapper> exports = new HashSet<ExportDescriptionWrapper>();
    private final List<RequireDescription> requiredBundles = new ArrayList<RequireDescription>();
    private final Set<CandidateWiring> candidateWirings = new HashSet<CandidateWiring>();
    private final List<RequiredBundleWrapper> candidateRequiredBundles = new ArrayList<RequiredBundleWrapper>();

    public BoundHost(Generation toBeResolved, BundleGeneration bundleGeneration, List<BoundFragment> fragments)
    {
        super(toBeResolved);

        this.bundleGeneration = bundleGeneration;
        this.fragments = Collections.unmodifiableList(fragments);

        for (ImportDescription description : bundleGeneration.getArchiveStore().getImportDescriptions())
        {
            for (String packageName : description.getPackages()) this.imports.add(new ImportDescriptionWrapper(packageName, description));
        }
        this.requiredBundles.addAll(bundleGeneration.getArchiveStore().getRequireDescriptions());

        initializeExports();
    }

    public BoundHost(BoundHost candidateBundle)
    {
        super(candidateBundle.getToBeResolved());

        this.bundleGeneration = candidateBundle.bundleGeneration;
        this.fragments = Collections.unmodifiableList(new ArrayList<BoundFragment>(candidateBundle.fragments));
        this.imports.addAll(candidateBundle.imports);
        this.exports.addAll(candidateBundle.exports);
        this.requiredBundles.addAll(candidateBundle.requiredBundles);
        this.candidateWirings.addAll(candidateBundle.candidateWirings);
        this.candidateRequiredBundles.addAll(candidateBundle.candidateRequiredBundles);
    }

    public BundleGeneration getBundleGeneration()
    {
        return bundleGeneration;
    }

    public List<BoundFragment> getFragments()
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

    public void addCandidateRequiredBundle(RequiredBundleWrapper candidate)
    {
        candidateRequiredBundles.add(candidate);
        initializeExports();
    }

    public Set<CandidateWiring> getCandidateWirings()
    {
        return candidateWirings;
    }

    public List<RequiredBundleWrapper> getCandidateRequiredBundles()
    {
        return Collections.unmodifiableList(candidateRequiredBundles);
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("toBeResolved", getToBeResolved());
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

        for (ExportDescription description : getBundleGeneration().getArchiveStore().getExportDescriptions())
        {
            for (String packageName : description.getPackageNames())
            {
                exports.add(new ExportDescriptionWrapper(description, getBundleGeneration()));
            }
        }

        for (CandidateBundle candidate : candidateRequiredBundles)
        {
            BundleGeneration requiredBundle = candidate.getBundleGeneration();
            for (ExportDescription description : requiredBundle.getArchiveStore().getExportDescriptions())
            {
                exports.add(new ExportDescriptionWrapper(description, requiredBundle));
            }
        }

        for (Candidate candidate : fragments)
        {
            Generation generation;

            if (candidate instanceof UnResolved)
            {
                generation = ((UnResolved) candidate).getToBeResolved();
            }
            else
            {
                generation = ((Resolved) candidate).getBundleGeneration();
            }

            for (ExportDescription description : generation.getArchiveStore().getExportDescriptions())
            {
                exports.add(new ExportDescriptionWrapper(description, getBundleGeneration()));
            }
        }
    }

    @Override
    @SuppressWarnings({ "CloneDoesntCallSuperClone" })
    public Object clone() throws CloneNotSupportedException
    {
        return new BoundHost(this);
    }
}