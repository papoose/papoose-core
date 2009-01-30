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
package org.papoose.core.framework;

import java.util.Collections;
import java.util.Map;

import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
public class FragmentDescription
{
    public final static VersionRange DEFAULT_VERSION_RANGE = new VersionRange(new Version(0, 0, 0), null, true, false);
    private final String symbolName;
    private final Map<String, Object> parameters;
    private Extension extension;
    private VersionRange versionRange;

    public FragmentDescription(String symbolName, Map<String, Object> parameters)
    {
        this.symbolName = symbolName;
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    public String getSymbolName()
    {
        return symbolName;
    }

    public Map<String, Object> getParameters()
    {
        return parameters;
    }

    public Extension getExtension()
    {
        return extension;
    }

    void setExtension(Extension extension)
    {
        this.extension = extension;
    }

    public VersionRange getVersionRange()
    {
        return versionRange;
    }

    public void setVersionRange(VersionRange versionRange)
    {
        this.versionRange = versionRange;
    }
}
