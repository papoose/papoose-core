/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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
package org.papoose.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.Attributes;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.papoose.core.descriptions.ExportDescription;


/**
 * @version $Revision$ $Date$
 */
public class AttributeUtils
{
    public static Dictionary allocateReadOnlyDictionary(final Attributes attributes)
    {
        return new Dictionary()
        {
            public int size() { return attributes.size(); }

            public boolean isEmpty() { return attributes.isEmpty(); }

            public Enumeration keys() { return Collections.enumeration(attributes.keySet()); }

            public Enumeration elements() { return Collections.enumeration(attributes.values()); }

            public Object get(Object key) { return attributes.getValue((String) key); }

            public Object put(Object key, Object value) { throw new UnsupportedOperationException("Read-only dictionary"); }

            public Object remove(Object key) { throw new UnsupportedOperationException("Read-only dictionary"); }
        };
    }

    public static Dictionary allocateReadOnlyI18nDictionary(final Attributes attributes, final ResourceBundle resourceBundle)
    {
        return new Dictionary()
        {
            public int size() { return attributes.size(); }

            public boolean isEmpty() { return attributes.isEmpty(); }

            public Enumeration keys() { return Collections.enumeration(attributes.keySet()); }

            public Enumeration elements()
            {
                return new Enumeration()
                {
                    final Enumeration enumeration = Collections.enumeration(attributes.values());

                    public boolean hasMoreElements() { return enumeration.hasMoreElements(); }

                    public Object nextElement() { return localize(enumeration.nextElement().toString()); }
                };
            }

            public Object get(Object key) { return localize(attributes.getValue((String) key)); }

            protected Object localize(String result)
            {
                if (result != null && result.length() > 0 && result.charAt(0) == '%')
                {
                    result = result.substring(1);

                    if (resourceBundle != null) try { result = resourceBundle.getString(result); } catch (MissingResourceException ignore) { }
                }
                return result;
            }

            public Object put(Object key, Object value) { throw new UnsupportedOperationException("Read-only dictionary"); }

            public Object remove(Object key) { throw new UnsupportedOperationException("Read-only dictionary"); }
        };
    }

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

            if (parameters.containsKey(Constants.PACKAGE_SPECIFICATION_VERSION)) parameters.put(Constants.PACKAGE_SPECIFICATION_VERSION, Version.parseVersion((String) parameters.get(Constants.PACKAGE_SPECIFICATION_VERSION)));

            if (!parameters.containsKey(Constants.VERSION_ATTRIBUTE))
            {
                if (parameters.containsKey(Constants.PACKAGE_SPECIFICATION_VERSION))
                {
                    parameters.put(Constants.VERSION_ATTRIBUTE, parameters.get(Constants.PACKAGE_SPECIFICATION_VERSION));
                }
                else
                {
                    parameters.put(Constants.VERSION_ATTRIBUTE, ExportDescription.DEFAULT_VERSION);
                }
            }
            else
            {
                parameters.put(Constants.VERSION_ATTRIBUTE, Version.parseVersion((String) parameters.get("version")));
            }

            if (parameters.containsKey(Constants.PACKAGE_SPECIFICATION_VERSION) && !parameters.get(Constants.PACKAGE_SPECIFICATION_VERSION).equals(parameters.get(Constants.VERSION_ATTRIBUTE))) throw new BundleException("version and specification-version do not match");

            if (parameters.containsKey(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE)) throw new BundleException("Attempted to set bundle-symbolic-name in Export-Package");

            if (parameters.containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE)) throw new BundleException("Attempted to set bundle-version in Export-Package");

            parameters.put(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, bundleSymbolicName);
            parameters.put(Constants.BUNDLE_VERSION_ATTRIBUTE, bundleVersion);

            result.add(description);
        }
        return result;
    }

    private AttributeUtils() { }
}
