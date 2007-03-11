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

import junit.framework.TestCase;
import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
public class VersionRangeTest extends TestCase
{
    public void testParameters()
    {
        new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), false, false);

        try
        {
            new VersionRange(new Version(1, 1, 1), new Version(1, 0, 0), false, false);
            fail("Should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException iae)
        {
        }

        new VersionRange(new Version(1, 0, 0), new Version(1, 0, 0), true, true);

        try
        {
            new VersionRange(new Version(1, 0, 0), new Version(1, 0, 0), false, true);
            fail("Should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException iae)
        {
        }

        try
        {
            new VersionRange(new Version(1, 0, 0), new Version(1, 0, 0), true, false);
            fail("Should have thrown an IllegalArgumentException");
        }
        catch (IllegalArgumentException iae)
        {
        }
    }

    public void testRanges()
    {
        VersionRange range = new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), false, false);

        assertTrue(range.includes(new Version(1, 1, 0)));
        assertFalse(range.includes(new Version(0, 0, 1)));
        assertFalse(range.includes(new Version(1, 0, 0)));
        assertFalse(range.includes(new Version(2, 0, 0)));
        assertFalse(range.includes(new Version(3, 0, 0)));

        range = new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), true, false);

        assertTrue(range.includes(new Version(1, 1, 0)));
        assertFalse(range.includes(new Version(0, 0, 1)));
        assertTrue(range.includes(new Version(1, 0, 0)));
        assertFalse(range.includes(new Version(2, 0, 0)));
        assertFalse(range.includes(new Version(3, 0, 0)));

        range = new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), false, true);

        assertTrue(range.includes(new Version(1, 1, 0)));
        assertFalse(range.includes(new Version(0, 0, 1)));
        assertFalse(range.includes(new Version(1, 0, 0)));
        assertTrue(range.includes(new Version(2, 0, 0)));
        assertFalse(range.includes(new Version(3, 0, 0)));

        range = new VersionRange(new Version(1, 0, 0), new Version(2, 0, 0), true, true);

        assertTrue(range.includes(new Version(1, 1, 0)));
        assertFalse(range.includes(new Version(0, 0, 1)));
        assertTrue(range.includes(new Version(1, 0, 0)));
        assertTrue(range.includes(new Version(2, 0, 0)));
        assertFalse(range.includes(new Version(3, 0, 0)));

        range = new VersionRange(null, new Version(2, 0, 0), true, true);

        assertTrue(range.includes(new Version(1, 1, 0)));
        assertTrue(range.includes(new Version(0, 0, 1)));
        assertTrue(range.includes(new Version(1, 0, 0)));
        assertTrue(range.includes(new Version(2, 0, 0)));
        assertFalse(range.includes(new Version(3, 0, 0)));

        range = new VersionRange(new Version(1, 0, 0), null, true, true);

        assertTrue(range.includes(new Version(1, 1, 0)));
        assertFalse(range.includes(new Version(0, 0, 1)));
        assertTrue(range.includes(new Version(1, 0, 0)));
        assertTrue(range.includes(new Version(2, 0, 0)));
        assertTrue(range.includes(new Version(3, 0, 0)));
    }
}
