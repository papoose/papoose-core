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

import net.jcip.annotations.Immutable;

/**
 * @version $Revision$ $Date$
 */
@Immutable
public class Wire
{
    private final String packageName;
    private final ExportDescription exportDescription;
    private final BundleImpl bundle;

    Wire(String packageName, ExportDescription exportDescription, BundleImpl bundle)
    {
        if (packageName == null) throw new IllegalArgumentException("Package name cannot be null");
        if (exportDescription == null) throw new IllegalArgumentException("Export description cannot be null");
        if (bundle == null) throw new IllegalArgumentException("Bundle cannot be null");

        this.packageName = packageName;
        this.exportDescription = exportDescription;
        this.bundle = bundle;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public ExportDescription getExportDescription()
    {
        return exportDescription;
    }

    public BundleImpl getBundle()
    {
        return bundle;
    }

    public BundleClassLoader getBundleClassLoader()
    {
        return bundle.getClassLoader();
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
        int packageIndex = resource.lastIndexOf('.');
        packageIndex = (packageIndex < 0 ? 0 : packageIndex);
        String packageName = resource.substring(0, packageIndex);
        resource = resource.substring(packageIndex + 1);

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
        return packageName + " " + exportDescription + " " + bundle.getBundleId();
    }
}
