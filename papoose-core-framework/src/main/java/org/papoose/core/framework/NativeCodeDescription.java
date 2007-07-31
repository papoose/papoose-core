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
public class NativeCodeDescription implements Comparable
{
    private final List<String> paths;
    private final Map<String, Object> parameters;
    private final int ordinal;
    private transient Version osVersion;
    private transient String language;

    public NativeCodeDescription(List<String> paths, Map<String, Object> parameters, int ordinal)
    {
        this.paths = Collections.unmodifiableList(paths);
        this.parameters = Collections.unmodifiableMap(parameters);
        this.ordinal = ordinal;
    }

    public List<String> getPaths()
    {
        return paths;
    }

    public Map<String, Object> getParameters()
    {
        return parameters;
    }

    @SuppressWarnings({"EmptyCatchBlock"})
    public Version getOsVersion()
    {
        if (osVersion == null)
        {
            try
            {
                osVersion = Version.parseVersion((String) parameters.get("osversion"));
            }
            catch (IllegalArgumentException doNothing)
            {
            }
        }
        return osVersion;
    }

    public String getLanguage()
    {
        if (language == null)
        {
            language = (String) parameters.get("language");
        }
        return language;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NativeCodeDescription that = (NativeCodeDescription) o;

        return parameters.equals(that.parameters) && paths.equals(that.paths);
    }

    public int hashCode()
    {
        int result = paths.hashCode();
        result = 31 * result + parameters.hashCode();
        return result;
    }

    public int compareTo(Object object)
    {
        NativeCodeDescription that = (NativeCodeDescription) object;
        int result = 0;

        if (getOsVersion() != null && that.getOsVersion() != null) result = getOsVersion().compareTo(that.getOsVersion());
        if (result == 0 && getLanguage() != null && that.getLanguage() != null) result = getLanguage().compareTo(that.getLanguage());
        if (result == 0) result = ordinal - that.ordinal;
        return result;
    }
}
