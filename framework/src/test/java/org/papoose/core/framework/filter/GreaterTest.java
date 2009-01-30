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

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Assert;
import org.junit.Test;


/**
 * @version $Revision$ $Date$
 */
public class GreaterTest
{
    @Test
    public void test()
    {
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        Greater test = new Greater("foo", "45");

        dictionary.put("foo", 44L);
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", 45L);
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", 46L);
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("foo", 44);
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", 45);
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", 46);
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("foo", (short) 44);
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", (short) 45);
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", (short) 46);
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("foo", (byte) 44);
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", (byte) 45);
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", (byte) 46);
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("foo", "bar");
        Assert.assertFalse(test.match(dictionary));

        dictionary.put("foo", "44");
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", "45");
        Assert.assertTrue(test.match(dictionary));

        dictionary.put("foo", "46");
        Assert.assertFalse(test.match(dictionary));
    }
}
