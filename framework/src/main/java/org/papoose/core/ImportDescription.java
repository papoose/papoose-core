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
public class ImportDescription
{
    public final static VersionRange DEFAULT_VERSION_RANGE = new VersionRange(new Version(0, 0, 0), null, true, false);
    private final Set<String> packages;
    private final Map<String, Object> parameters;
    private Resolution resolution = Resolution.MANDATORY;

    public ImportDescription(Set<String> packages, Map<String, Object> parameters)
    {
        assert packages != null;
        assert parameters != null;

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


    public Resolution getResolution()
    {
        return resolution;
    }

    void setResolution(Resolution resolution)
    {
        this.resolution = resolution;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        for (String pkg : packages)
        {
            if (builder.length() > 0) builder.append(";");
            builder.append(pkg);
        }
        builder.append(";resolution:=");
        builder.append(resolution);
        if (!parameters.isEmpty())
        {
            for (String key : parameters.keySet())
            {
                builder.append(";");
                builder.append(key);
                builder.append("=");
                builder.append(parameters.get(key));
            }
        }

        return builder.toString();
    }
}
