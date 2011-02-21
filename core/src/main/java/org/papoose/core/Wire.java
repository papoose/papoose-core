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
package org.papoose.core;

import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.util.Util;


/**
 *
 */
public class Wire
{
    private final String packageName;
    private final ExportDescription exportDescription;
    private final BundleGeneration bundleGeneration;

    /**
     * Construct an instance of Wire
     *
     * @param packageName       the package that this wire exports
     * @param exportDescription the export description used for this wire
     * @param bundleGeneration  the bundle that provides this package
     */
    public Wire(String packageName, ExportDescription exportDescription, BundleGeneration bundleGeneration)
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

    public BundleClassLoader getBundleClassLoader()
    {
        return bundleGeneration.getClassLoader();
    }

    /**
     * Check if the resource name passes the class/resource filtering for the
     * wire.
     *
     * @param resource the resource name to be tested
     * @return the results of the test
     */
    public boolean validFor(String resource)
    {
        int packageIndex = resource.lastIndexOf('/');
        packageIndex = (packageIndex < 0 ? 0 : packageIndex);
        String packageName = resource.substring(0, packageIndex).replace('/', '.');
        resource = resource.substring(Math.min(resource.length(), packageIndex + 1));

        if (this.packageName.equals(packageName))
        {
            boolean matched = exportDescription.getInclude().isEmpty();
            for (String[] include : exportDescription.getInclude())
            {
                if (Util.match(include, resource))
                {
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;

            for (String[] exclude : exportDescription.getExclude())
            {
                if (Util.match(exclude, resource)) return false;
            }
            return true;
        }
        return false;
    }


    public String toString()
    {
        return packageName + " " + exportDescription + " " + bundleGeneration.getBundleId();
    }
}
