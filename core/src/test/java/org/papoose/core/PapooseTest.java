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

import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Assert;
import org.junit.Test;

import org.papoose.core.filter.Parser;
import org.papoose.core.mock.MockResolver;
import org.papoose.core.mock.MockStore;


/**
 *
 */
public class PapooseTest
{
    @Test
    public void test() throws Exception
    {
        Papoose poo = new Papoose(new MockStore(), new ScheduledThreadPoolExecutor(10), new Properties());

        poo.start();

        try
        {
            poo.setParser(new Parser());
            Assert.fail("Should not allow parser to be set after framework starts");
        }
        catch (Exception e)
        {
        }

        try
        {
            poo.setResolver(new MockResolver());
            Assert.fail("Should not allow resolver to be set after framework starts");
        }
        catch (Exception e)
        {
        }

        poo.stop();
        poo.waitForStop(0);

        try
        {
            poo.setParser(new Parser());
        }
        catch (Exception e)
        {
            Assert.fail("Should allow parser to be set before framework starts: " + poo.getState());
        }

        try
        {
            poo.setResolver(new MockResolver());
        }
        catch (Exception e)
        {
            Assert.fail("Should allow resolver to be set before framework starts");
        }
    }
}
