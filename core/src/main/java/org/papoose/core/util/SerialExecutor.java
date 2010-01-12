/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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
package org.papoose.core.util;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A handy wrapper class that conveniently queues runnables for execution.
 * <p/>
 * Use this class when you need to have multiple queues feeding a single
 * thread pool.
 *
 * @version $Revision$ $Date$
 */
public class SerialExecutor implements Executor
{
    private final static String CLASS_NAME = SerialExecutor.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Queue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();
    private final Executor executor;
    private Runnable active;

    /**
     * Initialzed the serial executor with a shared executor delegate.
     *
     * @param executor the shared executor that is fed by this queue
     */
    public SerialExecutor(Executor executor)
    {
        if (executor == null) throw new IllegalArgumentException("Executor is null");

        if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("executor: " + executor);

        this.executor = executor;
    }

    /**
     * Executes the given command at some time in the future.  The command
     * may execute in a new thread, in a pooled thread, or in the calling
     * thread, at the discretion of the <tt>Executor</tt> that this class was
     * constructed with.
     *
     * @param runnable the runnable task
     * @throws java.util.concurrent.RejectedExecutionException
     *                              if this task cannot be accepted for execution.
     * @throws NullPointerException if command is null
     */
    public synchronized void execute(final Runnable runnable)
    {
        LOGGER.entering(CLASS_NAME, "execute", runnable);

        tasks.offer(new Runnable()
        {
            public void run()
            {
                LOGGER.entering(CLASS_NAME, "execute.Runnable", runnable);

                try
                {
                    runnable.run();
                }
                finally
                {
                    scheduleNext();
                }

                LOGGER.exiting(CLASS_NAME, "execute.Runnable");
            }
        });

        if (active == null) scheduleNext();

        LOGGER.exiting(CLASS_NAME, "execute");
    }

    /**
     * Poll the queue for more work.  If a runnable is found then send it to
     * executor.
     */
    protected synchronized void scheduleNext()
    {
        LOGGER.entering(CLASS_NAME, "scheduleNext");

        if ((active = tasks.poll()) != null)
        {
            executor.execute(active);
        }

        LOGGER.exiting(CLASS_NAME, "scheduleNext");
    }
}