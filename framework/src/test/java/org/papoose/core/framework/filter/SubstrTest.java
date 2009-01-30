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
package org.papoose.core.framework.filter;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;


/**
 * @version $Revision$ $Date$
 */
public class SubstrTest
{
    @Test
    public void testMatch()
    {
        TestSubtr test = new TestSubtr("foo", new String[]{ "", "no", "brown", "" });

        Assert.assertTrue(test.match("How now brown cow"));
        Assert.assertTrue(test.match("How nobrown cow"));
        Assert.assertTrue(test.match("nobrown"));
        Assert.assertFalse(test.match("nomatch"));

        test = new TestSubtr("foo", new String[]{ "", "brown", "" });

        Assert.assertTrue(test.match("How now brown cow"));
        Assert.assertTrue(test.match("How nobrown cow"));
        Assert.assertTrue(test.match("nobrown"));
        Assert.assertFalse(test.match("nomatch"));

        test = new TestSubtr("foo", new String[]{ "H", "no", "brown", "w" });

        Assert.assertTrue(test.match("How now brown cow"));
        Assert.assertTrue(test.match("How nobrown cow"));
        Assert.assertTrue(test.match("Hnobrownw"));
        Assert.assertFalse(test.match("Hnomatchw"));

        test = new TestSubtr("foo", new String[]{ "How", "" });

        Assert.assertTrue(test.match("How now brown cow"));
        Assert.assertTrue(test.match("How"));
        Assert.assertFalse(test.match("Hnomatchw"));

        test = new TestSubtr("foo", new String[]{ "", "cow" });

        Assert.assertTrue(test.match("How now brown cow"));
        Assert.assertTrue(test.match("cow"));
        Assert.assertFalse(test.match("Hnomatchw"));
    }

    @Test
    public void testDictionary()
    {
        TestSubtr test = new TestSubtr("FoO", new String[]{ "", "no", "brown", "" });
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
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("FOO", "How nobrown cow");
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("FOO", "nobrown");
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("FOO", new Object[]{ null, false, 5.0, "nobrown" });
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("FOO", new String[]{ null, "false", "5.0", "nobrown" });
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("FOO", "nomatch");
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("FOO", 5.0);
        Assert.assertFalse(test.match(dictionary));

        dictionary.remove("FOO");
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("FOO", new Object[]{ null, false, 5.0, "nomatch" });
        Assert.assertFalse(test.match(dictionary));
    }

    @Test
    public void testDictionaryCase()
    {
        TestSubtr test = new TestSubtr("FoO", new String[]{ "", "no", "brown", "" });
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();

        dictionary.put("FoO", "How now brown cow");
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("FoO", "How nobrown cow");
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("FoO", "nobrown");
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("FoO", new Object[]{ null, false, 5.0, "nobrown" });
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("FoO", new String[]{ null, "false", "5.0", "nobrown" });
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("FoO", "nomatch");
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("FoO", 5.0);
        Assert.assertFalse(test.match(dictionary));

        dictionary.remove("FoO");
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("FoO", new Object[]{ null, false, 5.0, "nomatch" });
        Assert.assertFalse(test.match(dictionary));
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
