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

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;
import org.osgi.framework.Filter;

import org.papoose.core.framework.filter.Parser;


/**
 * @version $Revision$ $Date$
 */
public class FilterImplTest extends TestCase
{
    private final Parser parser = new Parser();

    public void test() throws Exception
    {
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        dictionary.put("c and f", new String[]{ "a", "b", "c" });
        Filter filter = new FilterImpl(parser.parse("   ( c and f    =c) "));

        assertTrue(filter.matchCase(dictionary));

        dictionary.put("service.pid", "USB-1232312452");
        dictionary.put("vendor", "ibm");
        filter = new FilterImpl(parser.parse(" ( & (service.pid=USB-1232312452)( | (vendor~=ericsson)( vendor  ~=ibm) ) ) "));

        assertTrue(filter.matchCase(dictionary));

        dictionary.put("vendor", "ericssin");
        filter = new FilterImpl(parser.parse(" ( & (service.pid=USB-1232312452)( | (   vendor   ~=ericsson)(vendor~=ibm) ) ) "));

        assertFalse(filter.matchCase(dictionary));

        dictionary.put("vendor", "ericssin01");
        filter = new FilterImpl(parser.parse(" ( & (service.pid=USB-1232312452)( | (vendor~=ericsson01)(vendor~=ibm) ) (!(vendor=ibm))) "));

        assertTrue(filter.matchCase(dictionary));

        dictionary.put("present", "oohrah");
        filter = new FilterImpl(parser.parse(" ( present =*) "));

        assertTrue(filter.matchCase(dictionary));

        dictionary.put("substr", "How now brown cow");
        filter = new FilterImpl(parser.parse(" ( substr =*no*brown*) "));

        assertTrue(filter.matchCase(dictionary));

        filter = new FilterImpl(parser.parse(" ( substr =*now*) "));

        assertTrue(filter.matchCase(dictionary));

        filter = new FilterImpl(parser.parse(" ( substr =H*no*brown*w) "));

        assertTrue(filter.matchCase(dictionary));

        filter = new FilterImpl(parser.parse(" ( substr =How*) "));

        assertTrue(filter.matchCase(dictionary));

        filter = new FilterImpl(parser.parse(" ( substr =*cow) "));

        assertTrue(filter.matchCase(dictionary));

        filter = new FilterImpl(parser.parse(" ( substr =How*br*n cow) "));

        assertTrue(filter.matchCase(dictionary));

        dictionary.put("substr", "How now brown cow ");
        filter = new FilterImpl(parser.parse(" ( substr =How*br*n cow ) "));

        assertTrue(filter.matchCase(dictionary));

        dictionary.put("substr", "How now* brown cow ");
        filter = new FilterImpl(parser.parse(" ( substr =How*\\**br*n cow ) "));

        assertTrue(filter.matchCase(dictionary));

        dictionary.put("substr", "How now* brown (cow) ");
        filter = new FilterImpl(parser.parse(" ( substr =How*\\**br*n \\(cow\\) ) "));

        assertTrue(filter.matchCase(dictionary));

        dictionary.remove("substr");
        dictionary.put("SuBsTr", "How now* brown (cow) ");

        assertFalse(filter.matchCase(dictionary));

        dictionary = new Hashtable<String, Object>();
        dictionary.put("SeRvIcE.pId", "USB-1232312452");
        dictionary.put("VeNdOr", "ericssin01");

        filter = new FilterImpl(parser.parse("(&(service.pid=USB-1232312452)(|(vendor~=ericssin01)(vendor~=ibm))(!(vendor=ibm)))"));

        assertTrue(filter.match(dictionary));
    }
}
