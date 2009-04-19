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

import net.jcip.annotations.Immutable;
import org.osgi.framework.Version;

import org.papoose.core.descriptions.ExportDescription;

/**
 * A simple wrapper to make sure that export descriptions are searched in
 * the proper order.  This wrapper assumes that <code>BundleImpl</code>
 * classes initially sort by their resolution status, i.e. resolved bundles
 * appear before un-resolved bundles.
 * <p/>
 * TODO: So why don't we include status in compareTo()?
 *
 * @version $Revision$ $Date$
 */
@Immutable
public class ExportDescriptionWrapper implements Comparable<ExportDescriptionWrapper>
{
    private final ExportDescription exportDescription;
    private final Candidate candidate;
    private final long bundleId;
    private final Version version;

    public ExportDescriptionWrapper(ExportDescription exportDescription, Candidate candidate)
    {
        this.exportDescription = exportDescription;
        this.candidate = candidate;
        this.bundleId = candidate.getGeneration().getBundleId();
        this.version = (Version) exportDescription.getParameters().get("version");

        assert bundleId >= 0;
        assert version != null;
    }

    public ExportDescription getExportDescription()
    {
        return exportDescription;
    }

    public Candidate getCandidate()
    {
        return candidate;
    }

    public int compareTo(ExportDescriptionWrapper o)
    {
        int result = version.compareTo(o.version);
        if (result == 0) result = (int) (bundleId - o.bundleId);
        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ExportDescriptionWrapper)) return false;

        ExportDescriptionWrapper that = (ExportDescriptionWrapper) o;

        return exportDescription.equals(that.exportDescription);
    }

    @Override
    public int hashCode()
    {
        return exportDescription.hashCode();
    }

    @Override
    public String toString()
    {
        return exportDescription.toString();
    }
}
