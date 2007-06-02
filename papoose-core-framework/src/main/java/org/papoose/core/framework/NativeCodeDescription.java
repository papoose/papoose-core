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

import org.osgi.framework.Filter;


/**
 * @version $Revision$ $Date$
 */
class NativeCodeDescription
{
    final static NativeCodeDescription ASTERISK = new NativeCodeDescription(null, null);
    private final List<String> paths;
    private final Map<String, String> parameters;
    private String osname;
    private VersionRange osversion;
    private String processor;
    private String language;
    private Filter filter;

    public NativeCodeDescription(List<String> paths, Map<String, String> parameters)
    {
        this.paths = Collections.unmodifiableList(paths);
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public List<String> getPaths()
    {
        return paths;
    }

    public Map<String, String> getParameters()
    {
        return parameters;
    }

    public String getOsname()
    {
        return osname;
    }

    void setOsname(String osname)
    {
        this.osname = osname;
    }

    public VersionRange getOsversion()
    {
        return osversion;
    }

    void setOsversion(VersionRange osversion)
    {
        this.osversion = osversion;
    }

    public String getProcessor()
    {
        return processor;
    }

    void setProcessor(String processor)
    {
        this.processor = processor;
    }

    public String getLanguage()
    {
        return language;
    }

    void setLanguage(String language)
    {
        this.language = language;
    }

    public Filter getFilter()
    {
        return filter;
    }

    void setFilter(Filter filter)
    {
        this.filter = filter;
    }
}
