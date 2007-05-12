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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

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
        TestSubtr test = new TestSubtr("FoO", new String[]{"", "no", "brown", ""});
        Dictionary<String, Object> dictionary = new Dictionary<String, Object>()
        {
            final Map<String, Object> map = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

            public int size()
            {
                return map.size();
            }

            public boolean isEmpty()
            {
                return map.isEmpty();
            }

            public Enumeration<String> keys()
            {
                return Collections.enumeration(map.keySet());
            }

            public Enumeration<Object> elements()
            {
                return Collections.enumeration(map.values());
            }

            public Object get(Object key)
            {
                return map.get(key);
            }

            public Object put(String key, Object value)
            {
                return map.put(key, value);
            }

            public Object remove(Object key)
            {
                return map.remove(key);
            }
        };

        dictionary.put("FOO", "How now brown cow");
        assertTrue(test.match(dictionary));

        dictionary.put("FOO", "How nobrown cow");
        assertTrue(test.match(dictionary));

        dictionary.put("FOO", "nobrown");
        assertTrue(test.match(dictionary));

        dictionary.put("FOO", new Object[]{null, false, 5.0, "nobrown"});
        assertFalse(test.match(dictionary));

        dictionary.put("FOO", new String[]{null, "false", "5.0", "nobrown"});
        assertTrue(test.match(dictionary));

        dictionary.put("FOO", "nomatch");
        assertFalse(test.match(dictionary));

        dictionary.put("FOO", 5.0);
        assertFalse(test.match(dictionary));

        dictionary.remove("FOO");
        assertFalse(test.match(dictionary));

        dictionary.put("FOO", new Object[]{null, false, 5.0, "nomatch"});
        assertFalse(test.match(dictionary));
    }

    public void testDictionaryCase()
    {
        TestSubtr test = new TestSubtr("FoO", new String[]{"", "no", "brown", ""});
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();

        dictionary.put("FoO", "How now brown cow");
        assertTrue(test.match(dictionary));

        dictionary.put("FoO", "How nobrown cow");
        assertTrue(test.match(dictionary));

        dictionary.put("FoO", "nobrown");
        assertTrue(test.match(dictionary));

        dictionary.put("FoO", new Object[]{null, false, 5.0, "nobrown"});
        assertFalse(test.match(dictionary));

        dictionary.put("FoO", new String[]{null, "false", "5.0", "nobrown"});
        assertTrue(test.match(dictionary));

        dictionary.put("FoO", "nomatch");
        assertFalse(test.match(dictionary));

        dictionary.put("FoO", 5.0);
        assertFalse(test.match(dictionary));

        dictionary.remove("FoO");
        assertFalse(test.match(dictionary));

        dictionary.put("FoO", new Object[]{null, false, 5.0, "nomatch"});
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
