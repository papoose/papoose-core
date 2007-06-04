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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;


/**
 * @version $Revision$ $Date$
 */
public class ImportDescriptionTest extends TestCase
{
    public void testConstruction()
    {
        Properties p = System.getProperties();
        List<String> packageNames = new ArrayList<String>();
        Map<String, Object> attributes = new HashMap<String, Object>();

        try
        {
            new ImportDescription(null, attributes);
            fail("Should have caught the null package names list");
        }
        catch (AssertionError donothing)
        {
        }

        try
        {
            new ImportDescription(packageNames, attributes);
            fail("Should have caught the empty package names list");
        }
        catch (AssertionError donothing)
        {
        }

        try
        {
            new ImportDescription(packageNames, null);
            fail("Should have caught the null attributes map");
        }
        catch (AssertionError donothing)
        {
        }

        packageNames.add("com.acme.detonator");
        new ImportDescription(packageNames, attributes);

    }
}
