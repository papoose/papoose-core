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
public class SubstrTest extends TestCase
{
    public void testMatch()
    {
        TestSubtr test = new TestSubtr("foo", new String[]{"", "no", "brown", ""});

        assertTrue(test.match("How now brown cow"));
        assertTrue(test.match("How nobrown cow"));
        assertTrue(test.match("nobrown"));
        assertFalse(test.match("nomatch"));

        test = new TestSubtr("foo", new String[]{"", "brown", ""});

        assertTrue(test.match("How now brown cow"));
        assertTrue(test.match("How nobrown cow"));
        assertTrue(test.match("nobrown"));
        assertFalse(test.match("nomatch"));

        test = new TestSubtr("foo", new String[]{"H", "no", "brown", "w"});

        assertTrue(test.match("How now brown cow"));
        assertTrue(test.match("How nobrown cow"));
        assertTrue(test.match("Hnobrownw"));
        assertFalse(test.match("Hnomatchw"));

        test = new TestSubtr("foo", new String[]{"How", ""});

        assertTrue(test.match("How now brown cow"));
        assertTrue(test.match("How"));
        assertFalse(test.match("Hnomatchw"));

        test = new TestSubtr("foo", new String[]{"", "cow"});

        assertTrue(test.match("How now brown cow"));
        assertTrue(test.match("cow"));
        assertFalse(test.match("Hnomatchw"));
    }

    public void testDictionary()
    {
        TestSubtr test = new TestSubtr("foo", new String[]{"", "no", "brown", ""});
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();

        dictionary.put("foo", "How now brown cow");
        assertTrue(test.match(dictionary));

        dictionary.put("foo", "How nobrown cow");
        assertTrue(test.match(dictionary));

        dictionary.put("foo", "nobrown");
        assertTrue(test.match(dictionary));

        dictionary.put("foo", new Object[]{null, false, 5.0, "nobrown"});
        assertFalse(test.match(dictionary));

        dictionary.put("foo", new String[]{null, "false", "5.0", "nobrown"});
        assertTrue(test.match(dictionary));

        dictionary.put("foo", "nomatch");
        assertFalse(test.match(dictionary));

        dictionary.put("foo", 5.0);
        assertFalse(test.match(dictionary));

        dictionary.remove("foo");
        assertFalse(test.match(dictionary));

        dictionary.put("foo", new Object[]{null, false, 5.0, "nomatch"});
        assertFalse(test.match(dictionary));

    }

    static class TestSubtr extends Substr
    {
        public TestSubtr(String attribute, String[] values)
        {
            super(attribute, values);
        }

        protected boolean match(String foo)
        {
            return super.match(foo);
        }
    }
}
