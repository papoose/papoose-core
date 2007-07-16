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
public class ExportDescription
{
    public static final Version DEFAULT_VERSION = new Version(0, 0, 0);
    private final List<String> packages;
    private final Map<String, Object> parameters;
    private List<String> uses;
    private List<String> mandatory;
    private List<String> include;
    private List<String> exclude;

    ExportDescription(List<String> paths, Map<String, Object> parameters)
    {
        this.packages = Collections.unmodifiableList(paths);
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

    public List<String> getUses()
    {
        return uses;
    }

    void setUses(List<String> uses)
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

    public List<String> getInclude()
    {
        return include;
    }

    void setInclude(List<String> include)
    {
        this.include = include;
    }

    public List<String> getExclude()
    {
        return exclude;
    }

    void setExclude(List<String> exclude)
    {
        this.exclude = exclude;
    }
}
