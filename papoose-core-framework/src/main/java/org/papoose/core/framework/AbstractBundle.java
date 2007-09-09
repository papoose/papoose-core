/**
 *
 * Copyright 2007 (C) The original author or authors
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
package org.papoose.core.framework;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import org.papoose.core.framework.spi.ArchiveStore;


/**
 * @version $Revision$ $Date$
 */
abstract class AbstractBundle implements Bundle
{
    private final Papoose framework;
    protected final long bundleId;
    private final List<NativeCodeDescription> bundleNativeCodeList;
    private final boolean bundleNativeCodeListOptional;
    private final SortedSet<NativeCodeDescription> nativeCodeDescriptions;
    private ArchiveStore archiveStore;

    protected AbstractBundle(Papoose framework, long bundleId, List<NativeCodeDescription> bundleNativeCodeList, boolean bundleNativeCodeListOptional) throws BundleException
    {
        this.framework = framework;
        this.bundleId = bundleId;
        this.bundleNativeCodeList = bundleNativeCodeList;
        this.bundleNativeCodeListOptional = bundleNativeCodeListOptional;
        this.nativeCodeDescriptions = resolveNativeCodeDependencies();
    }

    Papoose getFramework()
    {
        return framework;
    }

    public long getBundleId()
    {
        return bundleId;
    }

    ArchiveStore getArchiveStore()
    {
        return archiveStore;
    }

    void assignArchiveStore(ArchiveStore archiveStore) throws BundleException
    {
        this.archiveStore = archiveStore;
        this.archiveStore.setNativeCodeDescriptions(nativeCodeDescriptions);
    }

    abstract void markInstalled();

    /**
     * Make sure that at least one native code description is valid.
     * <p/>
     * This could be done during the <code>BundleImpl</code> constructor but
     * it seems to be more transparent to have the bundle manager call this
     * method.
     *
     * @return a list of resolvable native code descriptions
     * @throws BundleException if the method is unable to find at least one valid native code description
     */
    private SortedSet<NativeCodeDescription> resolveNativeCodeDependencies() throws BundleException
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
                    if ("osname".equals(key) && !framework.getProperty(Constants.FRAMEWORK_OS_NAME).equals(parameters.get(key))) continue nextDescription;
                    else if ("processor".equals(key) && !framework.getProperty(Constants.FRAMEWORK_PROCESSOR).equals(parameters.get(key))) continue nextDescription;
                    else if ("osversion".equals(key))
                    {
                        if (!osVersionRange.includes(description.getOsVersion())) continue nextDescription;
                    }
                    else if ("language".equals(key) && !framework.getProperty(Constants.FRAMEWORK_LANGUAGE).equals(description.getLanguage())) continue nextDescription;
                    else if ("selection-filter".equals(key))
                    {
                        try
                        {
                            Filter selectionFilter = new FilterImpl(framework.getParser().parse((String) parameters.get(key)));
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
}
