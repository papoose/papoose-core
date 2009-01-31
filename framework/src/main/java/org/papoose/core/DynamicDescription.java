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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
public class DynamicDescription
{
    public final static VersionRange DEFAULT_VERSION_RANGE = new VersionRange(new Version(0, 0, 0), null, true, false);
    private final Set<String> packages;
    private final Map<String, Object> parameters;
    private VersionRange version;
    private String bundleSymbolicName;
    private VersionRange bundleVersion;

    public DynamicDescription(Set<String> packages, Map<String, Object> parameters)
    {
        this.packages = Collections.unmodifiableSet(packages);
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public Set<String> getPackages()
    {
        return packages;
    }

    public Map<String, Object> getParameters()
    {
        return parameters;
    }

    public VersionRange getVersion()
    {
        return version;
    }

    void setVersion(VersionRange version)
    {
        this.version = version;
    }


    public String getBundleSymbolicName()
    {
        return bundleSymbolicName;
    }

    void setBundleSymbolicName(String bundleSymbolicName)
    {
        this.bundleSymbolicName = bundleSymbolicName;
    }


    public VersionRange getBundleVersion()
    {
        return bundleVersion;
    }

    void setBundleVersion(VersionRange bundleVersion)
    {
        this.bundleVersion = bundleVersion;
    }
}
