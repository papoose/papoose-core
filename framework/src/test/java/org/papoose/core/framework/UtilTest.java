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
        Assert.assertEquals(tokens[0], "foo:=bar");
        Assert.assertEquals(tokens[1], "bar");
    }

    @Test
    public void testParseParameters() throws Exception
    {
        MockPOJO pojo = new MockPOJO();
        Map<String, Object> parameters = new HashMap<String, Object>();
        List<String> paths = new ArrayList<String>();

        Util.parseParameters("com.acme.dynamite;com.acme.gasoline;foo:=bar;foo=bar", pojo, parameters, true, paths);

        Assert.assertEquals(pojo.getFoo(), "bar");
        Assert.assertEquals(parameters.size(), 1);
        Assert.assertEquals(parameters.get("foo"), "bar");
        Assert.assertEquals(paths.size(), 2);
        Assert.assertEquals(paths.get(0), "com.acme.dynamite");
        Assert.assertEquals(paths.get(1), "com.acme.gasoline");

        parameters = new HashMap<String, Object>();
        paths = new ArrayList<String>();

        Util.parseParameters("com.acme.dynamite", pojo, parameters, true, paths);

        Assert.assertEquals(paths.size(), 1);
        Assert.assertEquals(paths.get(0), "com.acme.dynamite");

        parameters = new HashMap<String, Object>();
        paths = new ArrayList<String>();
        pojo.setFoo(null);

        Util.parseParameters("com.acme.dynamite;com.acme.gasoline", pojo, parameters, true, paths);

        Assert.assertEquals(pojo.getFoo(), null);
        Assert.assertEquals(parameters.size(), 0);
        Assert.assertEquals(paths.size(), 2);
        Assert.assertEquals(paths.get(0), "com.acme.dynamite");
        Assert.assertEquals(paths.get(1), "com.acme.gasoline");

        parameters = new HashMap<String, Object>();

        Util.parseParameters("foo:=\"bar;car;star\";foo=bar", pojo, parameters, true);

        Assert.assertEquals(pojo.getFoo(), "bar;car;star");
        Assert.assertEquals(parameters.size(), 1);
        Assert.assertEquals(parameters.get("foo"), "bar");

        parameters = new HashMap<String, Object>();

        Util.parseParameters("foo:=bar;foo=\"bar;car;star\"", pojo, parameters, true);

        Assert.assertEquals(pojo.getFoo(), "bar");
        Assert.assertEquals(parameters.size(), 1);
        Assert.assertEquals(parameters.get("foo"), "bar;car;star");
    }

    private static class MockPOJO
    {
        private String foo;
        private String howNowBrownCow;

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
    }
}
