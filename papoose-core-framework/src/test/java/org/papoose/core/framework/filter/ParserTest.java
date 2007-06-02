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
import org.osgi.framework.InvalidSyntaxException;


/**
 * @version $Revision$ $Date$
 */
public class ParserTest extends TestCase
{
    private final Parser parser = new Parser();

    public void test() throws Exception
    {

        try
        {
            parser.parse(" ( & (service.pid=USB-1232312452)( | (vendor~=ericsson)( vendor  ~ibm) ) ) ");
            fail("Should have thrown an exception");
        }
        catch (InvalidSyntaxException doNothing)
        {
        }

        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        dictionary.put("c and f", new String[]{"a", "b", "c"});
        Expr filter = parser.parse("   ( c and f    =c) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("service.pid", "USB-1232312452");
        dictionary.put("vendor", "ibm");
        filter = parser.parse(" ( & (service.pid=USB-1232312452)( | (vendor~=ericsson)( vendor  ~=ibm) ) ) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("vendor", "ericssin");
        filter = parser.parse(" ( & (service.pid=USB-1232312452)( | (   vendor   ~=ericsson)(vendor~=ibm) ) ) ");

        assertFalse(filter.match(dictionary));

        dictionary.put("vendor", "ericssin01");
        filter = parser.parse(" ( & (service.pid=USB-1232312452)( | (vendor~=ericsson01)(vendor~=ibm) ) (!(vendor=ibm))) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("present", "oohrah");
        filter = parser.parse(" ( present =*) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("substr", "How now brown cow");
        filter = parser.parse(" ( substr =*no*brown*) ");

        assertTrue(filter.match(dictionary));

        filter = parser.parse(" ( substr =*now*) ");

        assertTrue(filter.match(dictionary));

        filter = parser.parse(" ( substr =H*no*brown*w) ");

        assertTrue(filter.match(dictionary));

        filter = parser.parse(" ( substr =How*) ");

        assertTrue(filter.match(dictionary));

        filter = parser.parse(" ( substr =*cow) ");

        assertTrue(filter.match(dictionary));

        filter = parser.parse(" ( substr =How*br*n cow) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("substr", "How now brown cow ");
        filter = parser.parse(" ( substr =How*br*n cow ) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("substr", "How now* brown cow ");
        filter = parser.parse(" ( substr =How*\\**br*n cow ) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("substr", "How now* brown (cow) ");
        filter = parser.parse(" ( substr =How*\\**br*n \\(cow\\) ) ");

        assertTrue(filter.match(dictionary));

        dictionary.remove("substr");
        dictionary.put("SuBsTr", "How now* brown (cow) ");

        assertFalse(filter.match(dictionary));


        dictionary = new Dictionary<String, Object>()
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

        dictionary.put("SeRvIcE.pId", "USB-1232312452");
        dictionary.put("VeNdOr", "ericssin01");

        filter = parser.parse("(&(service.pid=USB-1232312452)(|(vendor~=ericssin01)(vendor~=ibm))(!(vendor=ibm)))");

        assertTrue(filter.match(dictionary));


        long start = System.currentTimeMillis();
        final int COUNT1 = 500000;

        for (int i = 0; i < COUNT1; i++)
        {
            parser.parse("(&(service.pid=USB-1232312452)(|(vendor~=ericssin01)(vendor~=ibm))(!(vendor=ibm)))");
        }

        long stop = System.currentTimeMillis();

        System.err.println("time: " + (stop - start) * 1000.0 / COUNT1 + "ns");

        start = System.currentTimeMillis();
        final int COUNT2 = 500000;

        dictionary.put("vendor", "ericssin01");
        filter = parser.parse("(&(service.pid=USB-1232312452)(|(vendor~=ericssin01)(vendor~=ibm))(!(vendor=ibm)))");
        for (int i = 0; i < COUNT2; i++)
        {
            filter.match(dictionary);
        }

        stop = System.currentTimeMillis();

        System.err.println("time case sensitive: " + (stop - start) * 1000.0 / COUNT2 + "ns");

        start = System.currentTimeMillis();
        final int COUNT3 = 500000;

        filter = parser.parse("(&(service.pid=USB-1232312452)(|(vendor~=ericssin01)(vendor~=ibm))(!(vendor=ibm)))");
        for (int i = 0; i < COUNT2; i++)
        {
            dictionary = new Dictionary<String, Object>()
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

            dictionary.put("SeRvIcE.pId", "USB-1232312452");
            dictionary.put("VeNdOr", "ericssin01");

            filter.match(dictionary);
        }

        stop = System.currentTimeMillis();

        System.err.println("time case insensitive " + (stop - start) * 1000.0 / COUNT3 + "ns");
    }
}
