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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
class ImportDescription
{
    public final static VersionRange DEFAULT_VERSION_RANGE = new VersionRange(new Version(0, 0, 0), null, true, false);
    private final List<String> packageNames;
    private final Map<String, Object> attributes;
    private final Resolution resolution;

    public ImportDescription(List<String> packageNames, Map<String, Object> attributes)
    {
        this(packageNames, attributes, Resolution.MANDATORY);
    }

    public ImportDescription(List<String> packageNames, Map<String, Object> attributes, Resolution resolution)
    {
        assert packageNames != null;
        assert packageNames.size() > 0;
        assert attributes != null;
        assert resolution != null;

        this.packageNames = Collections.unmodifiableList(packageNames);
        this.attributes = Collections.unmodifiableMap(attributes);
        this.resolution = resolution;
    }

    public List<String> getPackageNames()
    {
        return packageNames;
    }

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public Resolution getResolution()
    {
        return resolution;
    }
}
