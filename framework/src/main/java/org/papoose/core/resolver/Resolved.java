/**
 *
 * Copyright 2009 (C) The original author or authors
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
import org.papoose.core.FragmentGeneration;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.spi.ArchiveStore;

/**
 * @version $Revision$ $Date$
 */
public class Resolved extends Candidate implements CandidateBundle
{
    private final List<RequiredBundleWrapper> candidateRequiredBundles = new ArrayList<RequiredBundleWrapper>();
    private final List<ImportDescriptionWrapper> imports = new ArrayList<ImportDescriptionWrapper>();
    private final Set<ExportDescriptionWrapper> exports = new HashSet<ExportDescriptionWrapper>();
    private final Set<CandidateWiring> wirings = new HashSet<CandidateWiring>();
    private final Set<CandidateWiring> candidateWirings = new HashSet<CandidateWiring>();

    public Resolved(BundleGeneration bundleGeneration)
    {
        super(bundleGeneration);

        final ArchiveStore archiveStore = bundleGeneration.getArchiveStore();
        for (ImportDescription description : archiveStore.getImportDescriptions())
        {
            for (String packageName : description.getPackageNames())
            {
                imports.add(new ImportDescriptionWrapper(packageName, description));
            }
        }
        for (ExportDescription description : archiveStore.getExportDescriptions())
        {
            exports.add(new ExportDescriptionWrapper(description, this));
        }
    }

    public BundleGeneration getBundleGeneration()
    {
        return (BundleGeneration) getGeneration();
    }

    public List<FragmentGeneration> getFragments()
    {
        return Collections.unmodifiableList(getBundleGeneration().getFragments());
    }

    public List<RequiredBundleWrapper> getCandidateRequiredBundles()
    {
        return Collections.unmodifiableList(candidateRequiredBundles);
    }

    public List<ImportDescriptionWrapper> getImports()
    {
        return Collections.unmodifiableList(imports);
    }

    public Set<ExportDescriptionWrapper> getExports()
    {
        return Collections.unmodifiableSet(exports);
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
        Set<CandidateWiring> result = new HashSet<CandidateWiring>();

        result.addAll(wirings);
        result.addAll(candidateWirings);

        return Collections.unmodifiableSet(result);
    }

    public Set<CandidateWiring> getCandidateWirings()
    {
        return Collections.unmodifiableSet(candidateWirings);
    }
}
