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

import org.papoose.core.BundleGeneration;
import org.papoose.core.ExportDescription;

/**
 * @version $Revision$ $Date$
 */
class CandidateWiring
{
    private final String packageName;
    private final ExportDescription exportDescription;
    private final BundleGeneration bundleGeneration;

    public CandidateWiring(String packageName, ExportDescription exportDescription, BundleGeneration bundleGeneration)
    {
        assert packageName != null;
        assert exportDescription != null;
        assert bundleGeneration != null;

        this.packageName = packageName;
        this.exportDescription = exportDescription;
        this.bundleGeneration = bundleGeneration;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public ExportDescription getExportDescription()
    {
        return exportDescription;
    }

    public BundleGeneration getBundleGeneration()
    {
        return bundleGeneration;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CandidateWiring candidateWiring = (CandidateWiring) o;

        return packageName.equals(candidateWiring.packageName);
    }

    @Override
    public int hashCode()
    {
        return packageName.hashCode();
    }
}
