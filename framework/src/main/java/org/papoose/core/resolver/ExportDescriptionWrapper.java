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

import org.papoose.core.BundleGeneration;
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
class ExportDescriptionWrapper implements Comparable<ExportDescriptionWrapper>
{
    private final ExportDescription exportDescription;
    private final BundleGeneration bundleGeneration;
    private final long bundleId;
    private final Version version;

    public ExportDescriptionWrapper(ExportDescription exportDescription, BundleGeneration bundleGeneration)
    {
        this.exportDescription = exportDescription;
        this.bundleGeneration = bundleGeneration;
        this.bundleId = bundleGeneration.getBundleId();
        this.version = (Version) exportDescription.getParameters().get("version");
    }

    public ExportDescription getExportDescription()
    {
        return exportDescription;
    }

    public BundleGeneration getBundleGeneration()
    {
        return bundleGeneration;
    }

    public int compareTo(ExportDescriptionWrapper o)
    {
        int result = version.compareTo(o.version);
        if (result == 0) result = (int) (bundleId - o.bundleId);
        return result;
    }
}
