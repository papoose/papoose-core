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

import net.jcip.annotations.ThreadSafe;
import org.osgi.framework.Version;

import org.papoose.core.util.ToStringCreator;


/**
 * @version $Revision$ $Date$
 */
@ThreadSafe
public class NativeCodeDescription implements Comparable
{
    public final static NativeCodeDescription WILDCARD = new NativeCodeDescription(Collections.<String>emptySet(), Collections.<String, Object>emptyMap(), 0);
    private final Set<String> paths;
    private final Map<String, Object> parameters;
    private final int ordinal;
    private transient Version osVersion;
    private transient String language;

    public NativeCodeDescription(Set<String> paths, Map<String, Object> parameters, int ordinal)
    {
        this.paths = Collections.unmodifiableSet(paths);
        this.parameters = Collections.unmodifiableMap(parameters);
        this.ordinal = ordinal;
    }

    public Set<String> getPaths()
    {
        return paths;
    }

    public Map<String, Object> getParameters()
    {
        return parameters;
    }

    @SuppressWarnings({ "EmptyCatchBlock" })
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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NativeCodeDescription that = (NativeCodeDescription) o;

        return parameters.equals(that.parameters) && paths.equals(that.paths);
    }

    @Override
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

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("paths", paths);
        creator.append("parameters", parameters);
        creator.append("ordinal", ordinal);
        creator.append("osVersion", getOsVersion());
        creator.append("language", getLanguage());

        return creator.toString();

    }
}
