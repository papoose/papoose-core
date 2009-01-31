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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;


/**
 * @version $Revision$ $Date$
 */
public class UtilTest
{
    @Test
    public void testSetter()
    {
        MockPOJO pojo = new MockPOJO();

        Assert.assertTrue(Util.callSetter(pojo, "foo", "dogs"));
        Assert.assertEquals(pojo.getFoo(), "dogs");

        Assert.assertTrue(Util.callSetter(pojo, "how-now-brown-cow", "cats"));
        Assert.assertEquals(pojo.getHowNowBrownCow(), "cats");
    }

    @Test
    public void testMatch()
    {
        Assert.assertTrue(Util.match(":=", "foo:=bar", 3));
        Assert.assertFalse(Util.match(":=", "foo:bar", 3));
        Assert.assertFalse(Util.match(":=", "foo:", 3));
    }

    @Test
    public void testSplit()
    {
        String[] tokens = Util.split("foo:=bar", ":=");

        Assert.assertTrue(tokens.length == 2);
        Assert.assertEquals(tokens[0], "foo");
        Assert.assertEquals(tokens[1], "bar");

        tokens = Util.split("\"foo:=bar\":=bar", ":=");

        Assert.assertTrue(tokens.length == 2);
        Assert.assertEquals("\"foo:=bar\"", tokens[0]);
        Assert.assertEquals("bar", tokens[1]);
    }

    @Test
    public void testParseParameters() throws Exception
    {
        MockPOJO pojo = new MockPOJO();
        Map<String, Object> parameters = new HashMap<String, Object>();
        Set<String> paths = new HashSet<String>();

        Util.parseParameters("com.acme.dynamite;com.acme.gasoline;foo:=bar;foo=bar", pojo, parameters, true, paths);

        Assert.assertEquals(pojo.getFoo(), "bar");
        Assert.assertEquals(parameters.size(), 1);
        Assert.assertEquals(parameters.get("foo"), "bar");
        Assert.assertEquals(paths.size(), 2);
        Assert.assertTrue(paths.contains("com.acme.dynamite"));
        Assert.assertTrue(paths.contains("com.acme.gasoline"));

        parameters = new HashMap<String, Object>();
        paths = new HashSet<String>();

        Util.parseParameters("com.acme.dynamite", pojo, parameters, true, paths);

        Assert.assertEquals(paths.size(), 1);
        Assert.assertTrue(paths.contains("com.acme.dynamite"));

        parameters = new HashMap<String, Object>();
        paths = new HashSet<String>();
        pojo = new MockPOJO();

        Util.parseParameters("com.acme.dynamite;com.acme.gasoline", pojo, parameters, true, paths);

        Assert.assertEquals(pojo.getFoo(), null);
        Assert.assertEquals(parameters.size(), 0);
        Assert.assertEquals(paths.size(), 2);
        Assert.assertTrue(paths.contains("com.acme.dynamite"));
        Assert.assertTrue(paths.contains("com.acme.gasoline"));

        parameters = new HashMap<String, Object>();
        pojo = new MockPOJO();

        Util.parseParameters("foo:=\"bar;car;star\";foo=bar", pojo, parameters, true);

        Assert.assertEquals(pojo.getFoo(), "bar;car;star");
        Assert.assertEquals(parameters.size(), 1);
        Assert.assertEquals(parameters.get("foo"), "bar");

        parameters = new HashMap<String, Object>();
        pojo = new MockPOJO();

        Util.parseParameters("foo:=bar;foo=\"bar;car;star\"", pojo, parameters, true);

        Assert.assertEquals(pojo.getFoo(), "bar");
        Assert.assertEquals(parameters.size(), 1);
        Assert.assertEquals(parameters.get("foo"), "bar;car;star");

        parameters = new HashMap<String, Object>();
        pojo = new MockPOJO();

        Util.parseParameters("foo:=bar;include:=\"Qux*,BarImpl,*Foo,*,*Baf*\";exclude:=QuxImpl", pojo, parameters, true);

        Assert.assertEquals("bar", pojo.getFoo());
        Assert.assertEquals(5, pojo.getInclude().size());
        Assert.assertEquals(1, pojo.getExclude().size());
        Assert.assertTrue(Util.match(pojo.getInclude().get(0), "QuxFoo"));
        Assert.assertTrue(Util.match(pojo.getInclude().get(1), "BarImpl"));
        Assert.assertTrue(Util.match(pojo.getInclude().get(2), "QuxFoo"));
        Assert.assertTrue(Util.match(pojo.getInclude().get(3), "ANYTHING"));
        Assert.assertTrue(Util.match(pojo.getInclude().get(4), "ANYBafTHING"));
        Assert.assertEquals("Qux*", Util.encodeName(pojo.getInclude().get(0)));
        Assert.assertEquals("BarImpl", Util.encodeName(pojo.getInclude().get(1)));
        Assert.assertEquals("*Foo", Util.encodeName(pojo.getInclude().get(2)));
        Assert.assertEquals("*", Util.encodeName(pojo.getInclude().get(3)));
        Assert.assertEquals("*Baf*", Util.encodeName(pojo.getInclude().get(4)));
    }

    @Test
    public void testCombinations()
    {
        List<String> strings = new ArrayList<String>();

        strings.add("a");
        strings.add("b");
        strings.add("c");
        strings.add("d");
        strings.add("e");
        strings.add("f");

        int count = 0;
        for (List<String> set : Util.combinations(strings))
        {
            System.err.print("'");
            for (String s : set) System.err.print(s);
            System.err.println("'");
            count++;
        }
        Assert.assertTrue(count == 64);

        count = 0;
        for (List<String> set : Util.combinations(Collections.<String>emptyList()))
        {
            Assert.assertTrue(set.isEmpty());
            Assert.assertTrue(count++ == 0);
        }
    }

    private static class MockPOJO
    {
        private String foo;
        private String howNowBrownCow;
        private List<String[]> include = Collections.emptyList();
        private List<String[]> exclude = Collections.emptyList();

        public String getFoo()
        {
            return foo;
        }

        void setFoo(String foo)
        {
            this.foo = foo;
        }

        public String getHowNowBrownCow()
        {
            return howNowBrownCow;
        }

        void setHowNowBrownCow(String howNowBrownCow)
        {
            this.howNowBrownCow = howNowBrownCow;
        }

        public List<String[]> getInclude()
        {
            return include;
        }

        void setInclude(List<String[]> include)
        {
            this.include = include;
        }

        public List<String[]> getExclude()
        {
            return exclude;
        }

        void setExclude(List<String[]> exclude)
        {
            this.exclude = exclude;
        }
    }
}
