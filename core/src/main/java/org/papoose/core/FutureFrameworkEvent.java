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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.osgi.framework.FrameworkEvent;


/**
 * A {@link java.util.concurrent.Future} that returns the {@link org.osgi.framework.FrameworkEvent} of a stopped OSGi
 * {@link org.osgi.framework.launch.Framework} instance.  This class is used
 * for {@link org.osgi.framework.launch.Framework#waitForStop(long)} and is
 * "triggered" when {@link #setFrameworkEvent(org.osgi.framework.FrameworkEvent)} is used to set
 * the return value.
 *
 * @version $Revision$ $Date$
 */
class FutureFrameworkEvent implements Future<FrameworkEvent>
{
    private final static String CLASS_NAME = FutureFrameworkEvent.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final CountDownLatch latch;
    private volatile FrameworkEvent frameworkEvent = null;

    FutureFrameworkEvent()
    {
        this.latch = new CountDownLatch(1);
        LOGGER.config("Configured with a countdown latch");
    }

    FrameworkEvent getFrameworkEvent()
    {
        return frameworkEvent;
    }

    void setFrameworkEvent(FrameworkEvent frameworkEvent)
    {
        LOGGER.entering(CLASS_NAME, "setFrameworkEvent", frameworkEvent);

        assert !isDone();

        this.frameworkEvent = frameworkEvent;
        latch.countDown();

        LOGGER.exiting(CLASS_NAME, "setFrameworkEvent");
    }

    public boolean cancel(boolean mayInterruptIfRunning)
    {
        LOGGER.entering(CLASS_NAME, "cancel", mayInterruptIfRunning);
        LOGGER.exiting(CLASS_NAME, "cancel", false);

        return false;
    }

    public boolean isCancelled()
    {
        LOGGER.entering(CLASS_NAME, "isCancelled");
        LOGGER.exiting(CLASS_NAME, "isCancelled", false);

        return false;
    }

    public boolean isDone()
    {
        LOGGER.entering(CLASS_NAME, "isCancelled");

        boolean result = latch.getCount() == 0;

        LOGGER.exiting(CLASS_NAME, "isCancelled", result);

        return result;
    }

    public FrameworkEvent get() throws InterruptedException, ExecutionException
    {
        LOGGER.entering(CLASS_NAME, "get");

        latch.await();

        LOGGER.exiting(CLASS_NAME, "get", frameworkEvent);

        return frameworkEvent;
    }

    public FrameworkEvent get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
    {
        LOGGER.entering(CLASS_NAME, "get", new Object[]{ timeout, unit });

        if (!latch.await(timeout, unit)) throw new TimeoutException();

        LOGGER.exiting(CLASS_NAME, "get", frameworkEvent);

        return frameworkEvent;
    }
}
