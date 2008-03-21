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
package org.papoose.core.framework.filter;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;


/**
 * @version $Revision$ $Date$
 */
public class GreaterTest extends TestCase
{
    public void test()
    {
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        Greater test = new Greater("foo", "45");

        dictionary.put("foo", 44L);
        assertTrue(test.match(dictionary));

        dictionary.put("foo", 45L);
        assertTrue(test.match(dictionary));

        dictionary.put("foo", 46L);
        assertFalse(test.match(dictionary));

        dictionary.put("foo", 44);
        assertTrue(test.match(dictionary));

        dictionary.put("foo", 45);
        assertTrue(test.match(dictionary));

        dictionary.put("foo", 46);
        assertFalse(test.match(dictionary));

        dictionary.put("foo", (short) 44);
        assertTrue(test.match(dictionary));

        dictionary.put("foo", (short) 45);
        assertTrue(test.match(dictionary));

        dictionary.put("foo", (short) 46);
        assertFalse(test.match(dictionary));

        dictionary.put("foo", (byte) 44);
        assertTrue(test.match(dictionary));

        dictionary.put("foo", (byte) 45);
        assertTrue(test.match(dictionary));
        String g = test.attribute;

        dictionary.put("foo", (byte) 46);
        assertFalse(test.match(dictionary));

        dictionary.put("foo", "bar");
        assertFalse(test.match(dictionary));

        dictionary.put("foo", "44");
        assertTrue(test.match(dictionary));

        dictionary.put("foo", "45");
        assertTrue(test.match(dictionary));

        dictionary.put("foo", "46");
        assertFalse(test.match(dictionary));
    }
}
