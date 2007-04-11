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


/**
 * @version $Revision$ $Date$
 */
class ExportDescription
{
    private final List<String> packageNames;
    private final Map<String, Object> attributes;
    private final Map<String, Object> directives;

    public ExportDescription(List<String> packageNames, Map<String, Object> attributes, Map<String, Object> directives)
    {
        assert packageNames.size() > 0;
        assert attributes != null;
        assert directives != null;

        this.packageNames = Collections.unmodifiableList(packageNames);
        this.attributes = Collections.unmodifiableMap(attributes);
        this.directives = Collections.unmodifiableMap(directives);
    }

    public List<String> getPackageNames()
    {
        return packageNames;
    }

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public Map<String, Object> getDirectives()
    {
        return directives;
    }
}
