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

import net.jcip.annotations.ThreadSafe;
import org.osgi.framework.Version;

import org.papoose.core.VersionRange;
import org.papoose.core.Generation;
import org.papoose.core.util.ToStringCreator;


/**
 * @version $Revision$ $Date$
 */
@ThreadSafe
public class RequireDescription
{
    public final static VersionRange DEFAULT_VERSION_RANGE = new VersionRange(new Version(0, 0, 0), null, true, false);
    private final String symbolName;
    private final Map<String, Object> parameters;
    private volatile Visibility visibility = Visibility.PRIVATE;
    private volatile Resolution resolution = Resolution.MANDATORY;

    public RequireDescription(String symbolName, Map<String, Object> parameters)
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

    public Visibility getVisibility()
    {
        return visibility;
    }

    void setVisibility(Visibility visibility)
    {
        this.visibility = visibility;
    }

    public Resolution getResolution()
    {
        return resolution;
    }

    void setResolution(Resolution resolution)
    {
        this.resolution = resolution;
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("symbolicName", symbolName);
        creator.append("visibility", visibility);
        creator.append("resolution", resolution);

        return creator.toString();
    }
}
