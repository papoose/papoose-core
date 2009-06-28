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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
public class VersionRangeTest
{
    @Test
    public void testParameters()
    {
        new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), false, false);

        try
        {
            new VersionRange(new Version(1, 1, 1), new Version(1, 0, 0), false, false);
            Assert.fail("Should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException iae)
        {
        }

        new VersionRange(new Version(1, 0, 0), new Version(1, 0, 0), true, true);

        try
        {
            new VersionRange(new Version(1, 0, 0), new Version(1, 0, 0), false, true);
            Assert.fail("Should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException iae)
        {
        }

        try
        {
            new VersionRange(new Version(1, 0, 0), new Version(1, 0, 0), true, false);
            Assert.fail("Should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException iae)
        {
        }
    }

    public void testRanges()
    {
        VersionRange range = new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), false, false);

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertFalse(range.includes(new Version(1, 0, 0)));
        Assert.assertFalse(range.includes(new Version(2, 0, 0)));
        Assert.assertFalse(range.includes(new Version(3, 0, 0)));

        range = new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), true, false);

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertTrue(range.includes(new Version(1, 0, 0)));
        Assert.assertFalse(range.includes(new Version(2, 0, 0)));
        Assert.assertFalse(range.includes(new Version(3, 0, 0)));

        range = new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), false, true);

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertFalse(range.includes(new Version(1, 0, 0)));
        Assert.assertTrue(range.includes(new Version(2, 0, 0)));
        Assert.assertFalse(range.includes(new Version(3, 0, 0)));

        range = new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), true, true);

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertTrue(range.includes(new Version(1, 0, 0)));
        Assert.assertTrue(range.includes(new Version(2, 0, 0)));
        Assert.assertFalse(range.includes(new Version(3, 0, 0)));

        range = new VersionRange(new Version(1, 0, 0), null, true, true);

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertTrue(range.includes(new Version(1, 0, 0)));
        Assert.assertTrue(range.includes(new Version(2, 0, 0)));
        Assert.assertTrue(range.includes(new Version(3, 0, 0)));

        try
        {
            new VersionRange(null, new Version(2, 0, 0), true, true);
        }
        catch (IllegalArgumentException ignore)
        {
        }

    }

    public void testVersion()
    {
        VersionRange range = VersionRange.parseVersionRange("(1.0.0,2.0.0)");

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertFalse(range.includes(new Version(1, 0, 0)));
        Assert.assertFalse(range.includes(new Version(2, 0, 0)));
        Assert.assertFalse(range.includes(new Version(3, 0, 0)));

        range = VersionRange.parseVersionRange("  (  1.0.0   ,    2.0.0  )    ");

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertFalse(range.includes(new Version(1, 0, 0)));
        Assert.assertFalse(range.includes(new Version(2, 0, 0)));
        Assert.assertFalse(range.includes(new Version(3, 0, 0)));

        range = VersionRange.parseVersionRange("[1.0.0, 2.0.0)");

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertTrue(range.includes(new Version(1, 0, 0)));
        Assert.assertFalse(range.includes(new Version(2, 0, 0)));
        Assert.assertFalse(range.includes(new Version(3, 0, 0)));

        range = VersionRange.parseVersionRange("(1.0.0, 2.0.0]");

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertFalse(range.includes(new Version(1, 0, 0)));
        Assert.assertTrue(range.includes(new Version(2, 0, 0)));
        Assert.assertFalse(range.includes(new Version(3, 0, 0)));

        range = VersionRange.parseVersionRange("[1.0.0, 2.0.0]");

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertTrue(range.includes(new Version(1, 0, 0)));
        Assert.assertTrue(range.includes(new Version(2, 0, 0)));
        Assert.assertFalse(range.includes(new Version(3, 0, 0)));

        range = VersionRange.parseVersionRange("1.0.0");

        Assert.assertTrue(range.includes(new Version(1, 1, 0)));
        Assert.assertFalse(range.includes(new Version(0, 0, 1)));
        Assert.assertTrue(range.includes(new Version(1, 0, 0)));
        Assert.assertTrue(range.includes(new Version(2, 0, 0)));
        Assert.assertTrue(range.includes(new Version(3, 0, 0)));

        Assert.assertTrue(range.equals(range));
        Assert.assertFalse(range.equals(Version.parseVersion("1.2.3.wrong")));
        Assert.assertTrue(VersionRange.parseVersionRange("[1.0.0, 2.0.0]").equals(VersionRange.parseVersionRange("[1.0.0, 2.0.0]")));
        Assert.assertFalse(VersionRange.parseVersionRange("[1.0.0, 2.0.0]").equals(VersionRange.parseVersionRange("[1.0.0, 2.0.0)")));
        Assert.assertFalse(VersionRange.parseVersionRange("[1.0.0, 2.0.0]").equals(VersionRange.parseVersionRange("(1.0.0, 2.0.0]")));
        Assert.assertFalse(VersionRange.parseVersionRange("[1.0.0, 2.0.0]").equals(VersionRange.parseVersionRange("[1.0.0.yikes, 2.0.0]")));
        Assert.assertFalse(VersionRange.parseVersionRange("[1.0.0, 2.0.0]").equals(VersionRange.parseVersionRange("[1.0.0, 2.0.0.yikes]")));

        Map<VersionRange, String> map = new HashMap<VersionRange, String>();

        map.put(VersionRange.parseVersionRange("1.0.0"), "Hello");

        Assert.assertTrue("Hello".equals(map.get(VersionRange.parseVersionRange("1.0.0"))));

        map.put(VersionRange.parseVersionRange("[1.0.0, 2.0.0]"), "Hello");

        Assert.assertTrue("Hello".equals(map.get(VersionRange.parseVersionRange("[1.0.0, 2.0.0]"))));

        map.put(VersionRange.parseVersionRange("(1.0.0, 2.0.0]"), "Hello");

        Assert.assertTrue("Hello".equals(map.get(VersionRange.parseVersionRange("(1.0.0, 2.0.0]"))));

        map.put(VersionRange.parseVersionRange("[1.0.0, 2.0.0)"), "Hello");

        Assert.assertTrue("Hello".equals(map.get(VersionRange.parseVersionRange("[1.0.0, 2.0.0)"))));

        try
        {
            VersionRange.parseVersionRange("1.yikes.0");
            Assert.fail("Should have thrown an exception");
        }
        catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            VersionRange.parseVersionRange("[1.yikes.0, 2.0.0]");
            Assert.fail("Should have thrown an exception");
        }
        catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            VersionRange.parseVersionRange("[1.0.0, 2.yikes.0]");
            Assert.fail("Should have thrown an exception");
        }
        catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            VersionRange.parseVersionRange("[1.0.0 + 2.0.0]");
            Assert.fail("Should have thrown an exception");
        }
        catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            VersionRange.parseVersionRange("[1.0.0, 2.0.0+]");
            Assert.fail("Should have thrown an exception");
        }
        catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            VersionRange.parseVersionRange("[1.0.0, 2.0.0 ");
            Assert.fail("Should have thrown an exception");
        }
        catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            VersionRange.parseVersionRange("[1.0.0, 2.0.0 }");
            Assert.fail("Should have thrown an exception");
        }
        catch (IllegalArgumentException ignore)
        {
        }

        try
        {
            VersionRange.parseVersionRange("[1.0.0, 2.0.0");
            Assert.fail("Should have thrown an exception");
        }
        catch (IllegalArgumentException ignore)
        {
        }
    }
}
