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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.papoose.core.BundleGeneration;
import org.papoose.core.FragmentGeneration;

/**
 *
 */
class RequiredBundleWrapper implements CandidateBundle
{
    private final CandidateBundle delegate;
    private final boolean reExport;

    RequiredBundleWrapper(CandidateBundle delegate, boolean reExport)
    {
        assert delegate != null;

        this.delegate = delegate;
        this.reExport = reExport;
    }

    public BundleGeneration getBundleGeneration() { return delegate.getBundleGeneration(); }

    public List<FragmentGeneration> getFragments() { return delegate.getFragments(); }

    public List<RequiredBundleWrapper> getCandidateRequiredBundles()
    {
        return Collections.emptyList();
    }

    public List<ImportDescriptionWrapper> getImports() { return delegate.getImports(); }

    public Set<ExportDescriptionWrapper> getExports() { return delegate.getExports(); }

    public boolean addCandidateWiring(CandidateWiring candidateWiring) { return delegate.addCandidateWiring(candidateWiring); }

    public boolean replaceCandidateWiring(CandidateWiring candidateWiring) { return delegate.replaceCandidateWiring(candidateWiring); }

    public Set<CandidateWiring> getWirings() { return delegate.getWirings(); }

    public Set<CandidateWiring> getCandidateWirings() { return delegate.getCandidateWirings(); }

    public boolean isReExport()
    {
        return reExport;
    }
}
