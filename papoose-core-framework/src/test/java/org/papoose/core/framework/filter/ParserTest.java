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
public class ParserTest extends TestCase
{
    public void test() throws Exception
    {
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        dictionary.put("C AND F", new String[] { "a", "b", "c" });
        Expr filter = new Parser().parse("   ( c and f    =c) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("SERVICE.PID", "USB-1232312452");
        dictionary.put("VENDOR", "ibm");
        filter = new Parser().parse(" ( & (service.pid=USB-1232312452)( | (vendor~=ericsson)( vendor  ~=ibm) ) ) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("VENDOR", "ericssin");
        filter = new Parser().parse(" ( & (service.pid=USB-1232312452)( | (   vendor   ~=ericsson)(vendor~=ibm) ) ) ");

        assertFalse(filter.match(dictionary));

        dictionary.put("VENDOR", "ericssin01");
        filter = new Parser().parse(" ( & (service.pid=USB-1232312452)( | (vendor~=ericsson01)(vendor~=ibm) ) (!(vendor=ibm))) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("PRESENT", "oohrah");
        filter = new Parser().parse(" ( present =*) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("SUBSTR", "How now brown cow");
        filter = new Parser().parse(" ( substr =*no*brown*) ");

        assertTrue(filter.match(dictionary));

        filter = new Parser().parse(" ( substr =*now*) ");

        assertTrue(filter.match(dictionary));

        filter = new Parser().parse(" ( substr =H*no*brown*w) ");

        assertTrue(filter.match(dictionary));

        filter = new Parser().parse(" ( substr =How*) ");

        assertTrue(filter.match(dictionary));

        filter = new Parser().parse(" ( substr =*cow) ");

        assertTrue(filter.match(dictionary));

        filter = new Parser().parse(" ( substr =How*br*n cow) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("SUBSTR", "How now brown cow ");
        filter = new Parser().parse(" ( substr =How*br*n cow ) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("SUBSTR", "How now* brown cow ");
        filter = new Parser().parse(" ( substr =How*\\**br*n cow ) ");

        assertTrue(filter.match(dictionary));

        dictionary.put("SUBSTR", "How now* brown (cow) ");
        filter = new Parser().parse(" ( substr =How*\\**br*n \\(cow\\) ) ");

        assertTrue(filter.match(dictionary));


        long start = System.currentTimeMillis();
        final int COUNT1 = 500000;

        Parser parser = new Parser();
        for (int i = 0; i < COUNT1; i++)
        {
            parser.parse("(&(service.pid=USB-1232312452)(|(vendor~=ericssin01)(vendor~=ibm))(!(vendor=ibm)))");
        }

        long stop = System.currentTimeMillis();

        System.err.println("time: " + (stop - start) * 1000.0 / COUNT1 + "ns");

        start = System.currentTimeMillis();
        final int COUNT2 = 500000;

        dictionary.put("VENDOR", "ericssin01");
        filter = parser.parse("(&(service.pid=USB-1232312452)(|(vendor~=ericssin01)(vendor~=ibm))(!(vendor=ibm)))");
        for (int i = 0; i < COUNT2; i++)
        {
            filter.match(dictionary);
        }

        stop = System.currentTimeMillis();

        System.err.println("time: " + (stop - start) * 1000.0 / COUNT2 + "ns");
    }
}
