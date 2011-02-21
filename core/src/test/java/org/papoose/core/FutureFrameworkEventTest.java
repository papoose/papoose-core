/**
 *
 * Copyright 2009 (C) The original author or authors
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

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;

import org.papoose.core.mock.MockBundle;


/**
 *
 */
public class FutureFrameworkEventTest
{
    private final static Bundle MOCK_SYSTEM_BUNDLE = new MockBundle();

    @Test
    public void testIsDone()
    {
        FutureFrameworkEvent futureFrameworkEvent = new FutureFrameworkEvent();

        Assert.assertFalse(futureFrameworkEvent.isDone());

        futureFrameworkEvent.setFrameworkEvent(new FrameworkEvent(FrameworkEvent.STOPPED, MOCK_SYSTEM_BUNDLE, null));

        Assert.assertTrue(futureFrameworkEvent.isDone());
    }

    @Test
    public void testGet() throws Exception
    {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final FutureFrameworkEvent futureFrameworkEvent = new FutureFrameworkEvent();

        Assert.assertFalse(futureFrameworkEvent.isDone());

        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        try
        {
            Future<Boolean> result = pool.submit(new Callable<Boolean>()
            {
                public Boolean call() throws Exception
                {
                    futureFrameworkEvent.get();

                    countDownLatch.countDown();

                    return futureFrameworkEvent.isDone();
                }
            });

            Assert.assertEquals(1, countDownLatch.getCount());

            futureFrameworkEvent.setFrameworkEvent(new FrameworkEvent(FrameworkEvent.STOPPED, MOCK_SYSTEM_BUNDLE, null));

            Assert.assertTrue(result.get());
            Assert.assertEquals(0, countDownLatch.getCount());
            Assert.assertTrue(futureFrameworkEvent.isDone());
        }
        finally
        {
            pool.shutdown();
        }
    }

    @Test
    public void testGetWithTimeout() throws Exception
    {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final FutureFrameworkEvent futureFrameworkEvent = new FutureFrameworkEvent();

        Assert.assertFalse(futureFrameworkEvent.isDone());

        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        try
        {
            Future<Boolean> result = pool.submit(new Callable<Boolean>()
            {
                public Boolean call() throws Exception
                {
                    futureFrameworkEvent.get(60, TimeUnit.SECONDS);

                    countDownLatch.countDown();

                    return futureFrameworkEvent.isDone();
                }
            });

            Assert.assertEquals(1, countDownLatch.getCount());

            futureFrameworkEvent.setFrameworkEvent(new FrameworkEvent(FrameworkEvent.STOPPED, MOCK_SYSTEM_BUNDLE, null));

            Assert.assertTrue(result.get());
            Assert.assertEquals(0, countDownLatch.getCount());
            Assert.assertTrue(futureFrameworkEvent.isDone());
        }
        finally
        {
            pool.shutdown();
        }
    }

    @Test
    public void testTimeout() throws Exception
    {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final FutureFrameworkEvent futureFrameworkEvent = new FutureFrameworkEvent();

        Assert.assertFalse(futureFrameworkEvent.isDone());

        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        try
        {
            Future<Boolean> result = pool.submit(new Callable<Boolean>()
            {
                public Boolean call() throws Exception
                {
                    futureFrameworkEvent.get(1, TimeUnit.SECONDS);

                    countDownLatch.countDown();

                    return futureFrameworkEvent.isDone();
                }
            });

            Assert.assertEquals(1, countDownLatch.getCount());

            result.get();

            Assert.fail("Should have timed out");
        }
        catch (ExecutionException e)
        {
            Assert.assertTrue(e.getCause() instanceof TimeoutException);
            Assert.assertEquals(1, countDownLatch.getCount());
            Assert.assertFalse(futureFrameworkEvent.isDone());
        }
        finally
        {
            pool.shutdown();
        }
    }
}
