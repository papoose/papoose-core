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
package org.papoose.core.descriptions;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Version;

import org.papoose.core.VersionRange;


/**
 *
 */
public class DynamicDescription
{
    public final static VersionRange DEFAULT_VERSION_RANGE = new VersionRange(new Version(0, 0, 0), null, true, false);
    private final Set<String> packagePatterns;
    private final Map<String, Object> parameters;

    public DynamicDescription(Set<String> packagePatterns, Map<String, Object> parameters)
    {
        this.packagePatterns = Collections.unmodifiableSet(packagePatterns);
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public Set<String> getPackagePatterns()
    {
        return packagePatterns;
    }

    public Map<String, Object> getParameters()
    {
        return parameters;
    }
}
