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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;


/**
 * @version $Revision$ $Date$
 */
public class WireTest extends TestCase
{
    public void testMatch()
    {
        List<String> packages = new ArrayList<String>();
        packages.add("com.acme");

        ExportDescription description = new ExportDescription(packages, Collections.<String, Object>emptyMap());

        Wire test = new Wire("com.acme", description, null);

        assertTrue(test.validFor("com.acme.Dynamite"));

        description.setIncluded(Collections.singletonList(new String[]{"Dynamite"}));

        assertTrue(test.validFor("com.acme.Dynamite"));

        description.setIncluded(Collections.singletonList(new String[]{"Dynam", ""}));

        assertTrue(test.validFor("com.acme.Dynamite"));

        description.setIncluded(Collections.singletonList(new String[]{"", "nam", ""}));

        assertTrue(test.validFor("com.acme.Dynamite"));
    }
}
