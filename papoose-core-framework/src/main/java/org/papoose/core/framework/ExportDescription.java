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
import java.util.Set;

import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
public class ExportDescription
{
    public static final Version DEFAULT_VERSION = new Version(0, 0, 0);
    private final List<String> packages;
    private final Map<String, Object> parameters;
    private Set<String> uses = Collections.emptySet();
    private List<String> mandatory = Collections.emptyList();
    private List<String[]> included = Collections.emptyList();
    private List<String[]> excluded = Collections.emptyList();
    private transient volatile String string;

    ExportDescription(List<String> packages, Map<String, Object> parameters)
    {
        assert packages != null;
        assert parameters != null;

        this.packages = Collections.unmodifiableList(packages);
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public List<String> getPackages()
    {
        return packages;
    }

    public Map<String, Object> getParameters()
    {
        return parameters;
    }

    public Set<String> getUses()
    {
        return uses;
    }

    void setUses(Set<String> uses)
    {
        this.uses = uses;
    }

    public List<String> getMandatory()
    {
        return mandatory;
    }

    void setMandatory(List<String> mandatory)
    {
        this.mandatory = mandatory;
    }

    public List<String[]> getIncluded()
    {
        return included;
    }

    void setIncluded(List<String[]> included)
    {
        this.included = included;
    }

    public List<String[]> getExcluded()
    {
        return excluded;
    }

    void setExcluded(List<String[]> excluded)
    {
        this.excluded = excluded;
    }

    public String toString()
    {
        if (string == null)
        {
            StringBuilder builder = new StringBuilder();

            for (String pkg : packages)
            {
                if (builder.length() > 0) builder.append(";");
                builder.append(pkg);
            }
            if (!uses.isEmpty())
            {
                int count = 0;
                builder.append(";uses=");
                if (uses.size() > 1) builder.append("\"");
                for (String name : uses) { if (count++ > 0) builder.append(",");builder.append(name); }
                if (uses.size() > 1) builder.append("\"");
            }
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

            string = builder.toString();
        }
        return string;
    }
}
