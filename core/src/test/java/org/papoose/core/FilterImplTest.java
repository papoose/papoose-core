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
package org.papoose.core;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Filter;

import org.papoose.core.filter.Parser;


/**
 * @version $Revision$ $Date$
 */
public class FilterImplTest
{
    private final Parser parser = new Parser();

    @Test
    public void test() throws Exception
    {
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        dictionary.put("c and f", new String[]{ "a", "b", "c" });
        Filter filter = new DefaultFilter(parser.parse("   ( c and f    =c) "));

        Assert.assertTrue(filter.matchCase(dictionary));
        Assert.assertEquals("(c and f=c)", filter.toString());

        dictionary.put("service.pid", "USB-1232312452");
        dictionary.put("vendor", "ibm");
        filter = new DefaultFilter(parser.parse(" ( & (service.pid=USB-1232312452)( | (vendor~=ericsson)( vendor  ~=ibm) ) ) "));
        Assert.assertEquals("(&(service.pid=USB-1232312452)(|(vendor~=ericsson)(vendor~=ibm)))", filter.toString());

        Assert.assertTrue(filter.matchCase(dictionary));

        dictionary.put("vendor", "ericssin");
        filter = new DefaultFilter(parser.parse(" ( & (service.pid=USB-1232312452)( | (   vendor   ~=ericsson)(vendor~=ibm) ) ) "));

        Assert.assertFalse(filter.matchCase(dictionary));

        dictionary.put("vendor", "ericssin01");
        filter = new DefaultFilter(parser.parse(" ( & (service.pid=USB-1232312452)( | (vendor~=ericsson01)(vendor~=ibm) ) (!(vendor=ibm))) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        dictionary.put("present", "oohrah");
        filter = new DefaultFilter(parser.parse(" ( present =*) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        dictionary.put("substr", "How now brown cow");
        filter = new DefaultFilter(parser.parse(" ( substr =*no*brown*) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        filter = new DefaultFilter(parser.parse(" ( substr =*now*) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        filter = new DefaultFilter(parser.parse(" ( substr =H*no*brown*w) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        filter = new DefaultFilter(parser.parse(" ( substr =How*) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        filter = new DefaultFilter(parser.parse(" ( substr =*cow) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        filter = new DefaultFilter(parser.parse(" ( substr =How*br*n cow) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        dictionary.put("substr", "How now brown cow ");
        filter = new DefaultFilter(parser.parse(" ( substr =How*br*n cow ) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        dictionary.put("substr", "How now* brown cow ");
        filter = new DefaultFilter(parser.parse(" ( substr =How*\\**br*n cow ) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        dictionary.put("substr", "How now* brown (cow) ");
        filter = new DefaultFilter(parser.parse(" ( substr =How*\\**br*n \\(cow\\) ) "));

        Assert.assertTrue(filter.matchCase(dictionary));

        dictionary.remove("substr");
        dictionary.put("SuBsTr", "How now* brown (cow) ");

        Assert.assertFalse(filter.matchCase(dictionary));

        dictionary = new Hashtable<String, Object>();
        dictionary.put("SeRvIcE.pId", "USB-1232312452");
        dictionary.put("VeNdOr", "ericssin01");

        filter = new DefaultFilter(parser.parse("(&(service.pid=USB-1232312452)(|(vendor~=ericssin01)(vendor~=ibm))(!(vendor=ibm)))"));

        Assert.assertTrue(filter.match(dictionary));
    }
}
