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

import org.junit.Assert;
import org.junit.Test;


/**
 * @version $Revision$ $Date$
 */
public class WireTest
{
    @Test
    public void testMatch()
    {
        List<String> packages = new ArrayList<String>();
        packages.add("com.acme");

        ExportDescription description = new ExportDescription(packages, Collections.<String, Object>emptyMap());

        Wire test = new Wire("com.acme", description, null);

        Assert.assertTrue(test.validFor("com.acme.Dynamite"));

        description.setIncluded(Collections.singletonList(new String[]{ "Dynamite" }));

        Assert.assertTrue(test.validFor("com.acme.Dynamite"));

        description.setIncluded(Collections.singletonList(new String[]{ "Dynam", "" }));

        Assert.assertTrue(test.validFor("com.acme.Dynamite"));

        description.setIncluded(Collections.singletonList(new String[]{ "", "nam", "" }));

        Assert.assertTrue(test.validFor("com.acme.Dynamite"));
    }
}
