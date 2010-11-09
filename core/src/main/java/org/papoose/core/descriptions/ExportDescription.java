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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Version;

import org.papoose.core.util.Util;


/**
 * @version $Revision$ $Date$
 *          TODO: Make immutable
 */
public class ExportDescription
{
    public static final Version DEFAULT_VERSION = new Version(0, 0, 0);
    private final Set<String> packageNames;
    private final Map<String, Object> parameters;
    private Set<String> uses = Collections.emptySet();
    private List<String> mandatory = Collections.emptyList();
    private List<String[]> include = Collections.emptyList();
    private List<String[]> exclude = Collections.emptyList();
    private volatile int referenceCount = 0;

    public ExportDescription(Set<String> packageNames, Map<String, Object> parameters)
    {
        if (packageNames == null) throw new IllegalArgumentException("Package names cannot be null");
        if (parameters == null) throw new IllegalArgumentException("Parameters cannot be null");

        this.packageNames = Collections.unmodifiableSet(packageNames);
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public Set<String> getPackageNames()
    {
        return packageNames;
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

    public List<String[]> getInclude()
    {
        return include;
    }

    public void setInclude(List<String[]> include)
    {
        this.include = include;
    }

    public List<String[]> getExclude()
    {
        return exclude;
    }

    public void setExclude(List<String[]> exclude)
    {
        this.exclude = exclude;
    }

    public void incrementReferenceCount()
    {
        referenceCount++;
    }

    public void decrementReferenceCount()
    {
        referenceCount--;
    }

    public int getReferenceCount()
    {
        return referenceCount;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExportDescription that = (ExportDescription) o;

        if (referenceCount != that.referenceCount) return false;
        if (exclude != null ? !exclude.equals(that.exclude) : that.exclude != null) return false;
        if (include != null ? !include.equals(that.include) : that.include != null) return false;
        if (mandatory != null ? !mandatory.equals(that.mandatory) : that.mandatory != null) return false;
        if (!packageNames.equals(that.packageNames)) return false;
        if (!parameters.equals(that.parameters)) return false;
        return !(uses != null ? !uses.equals(that.uses) : that.uses != null);
    }

    @Override
    public int hashCode()
    {
        int result = packageNames.hashCode();
        result = 31 * result + parameters.hashCode();
        result = 31 * result + (uses != null ? uses.hashCode() : 0);
        result = 31 * result + (mandatory != null ? mandatory.hashCode() : 0);
        result = 31 * result + (include != null ? include.hashCode() : 0);
        result = 31 * result + (exclude != null ? exclude.hashCode() : 0);
        result = 31 * result + referenceCount;
        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        for (String pkg : packageNames)
        {
            if (builder.length() > 0) builder.append(";");
            builder.append(pkg);
        }
        if (!uses.isEmpty())
        {
            int count = 0;
            builder.append(";uses:=");
            for (String name : uses)
            {
                if (count++ > 0) builder.append(",");
                builder.append(name);
            }
        }
        if (!mandatory.isEmpty())
        {
            int count = 0;
            builder.append(";mandatory:=");
            if (mandatory.size() > 1) builder.append("\"");
            for (String name : mandatory)
            {
                if (count++ > 0) builder.append(",");
                builder.append(name);
            }
            if (mandatory.size() > 1) builder.append("\"");
        }
        if (!include.isEmpty())
        {
            int count = 0;
            builder.append(";include:=");
            if (include.size() > 1) builder.append("\"");
            for (String[] name : include)
            {
                if (count++ > 0) builder.append(",");
                builder.append(Util.encodeName(name));
            }
            if (include.size() > 1) builder.append("\"");
        }
        if (!exclude.isEmpty())
        {
            int count = 0;
            builder.append(";exclude:=");
            if (exclude.size() > 1) builder.append("\"");
            for (String[] name : exclude)
            {
                if (count++ > 0) builder.append(",");
                builder.append(Util.encodeName(name));
            }
            if (exclude.size() > 1) builder.append("\"");
        }
        if (!parameters.isEmpty())
        {
            for (Map.Entry<String,Object> entry : parameters.entrySet())
            {
                builder.append(";");
                builder.append(entry.getKey());
                builder.append("=");
                builder.append(entry.getValue());
            }
        }

        return builder.toString();
    }
}
