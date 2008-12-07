/**
 *
 * Copyright 2008 (C) The original author or authors
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
package org.papoose.core.framework.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.papoose.core.framework.ExportDescription;
import org.papoose.core.framework.Util;
import org.osgi.framework.Version;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;


/**
 * @version $Revision$ $Date$
 */
public class AttributeUtils
{
    public static List<ExportDescription> parseBundleExportList(String value, String bundleSymbolicName, Version bundleVersion) throws BundleException
    {
        String[] exportDescriptions = Util.split(value, ",");
        List<ExportDescription> result = new ArrayList<ExportDescription>(exportDescriptions.length);

        for (String exportDescription : exportDescriptions)
        {
            Set<String> paths = new HashSet<String>(1);
            Map<String, Object> parameters = new HashMap<String, Object>();
            ExportDescription description = new ExportDescription(paths, parameters);

            Util.parseParameters(exportDescription, description, parameters, true, paths);

            if (parameters.containsKey("specification-version")) parameters.put("specification-version", Version.parseVersion((String) parameters.get("specification-version")));

            if (!parameters.containsKey("version"))
            {
                if (parameters.containsKey("specification-version"))
                {
                    parameters.put("version", parameters.get("specification-version"));
                }
                else
                {
                    parameters.put("version", ExportDescription.DEFAULT_VERSION);
                }
            }
            else
            {
                parameters.put("version", Version.parseVersion((String) parameters.get("version")));
            }

            if (parameters.containsKey("specification-version") && !parameters.get("specification-version").equals(parameters.get("version"))) throw new BundleException("version and specification-version do not match");

            if (parameters.containsKey("bundle-symbolic-name")) throw new BundleException("Attempted to set bundle-symbolic-name in Export-Package");

            if (parameters.containsKey("bundle-version")) throw new BundleException("Attempted to set bundle-version in Export-Package");

            parameters.put(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, bundleSymbolicName);
            parameters.put(Constants.BUNDLE_VERSION_ATTRIBUTE, bundleVersion);

            result.add(description);
        }
        return result;
    }

    private AttributeUtils() { }
}
