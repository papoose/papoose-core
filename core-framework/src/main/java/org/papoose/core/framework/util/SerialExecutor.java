/**
 *
 * Copyright 2008 (C) The original author or authors
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
package org.papoose.core.framework.util;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @version $Revision$ $Date$
 */
public class SerialExecutor implements Executor
{
    final Queue<Runnable> tasks = new LinkedBlockingQueue<Runnable>();
    final Executor executor;
    Runnable active;

    public SerialExecutor(Executor executor)
    {
        this.executor = executor;
    }

    public synchronized void execute(final Runnable runnable)
    {
        tasks.offer(new Runnable()
        {
            public void run()
            {
                try
                {
                    runnable.run();
                }
                finally
                {
                    scheduleNext();
                }
            }
        });
        if (active == null)
        {
            scheduleNext();
        }
    }

    protected synchronized void scheduleNext()
    {
        if ((active = tasks.poll()) != null)
        {
            executor.execute(active);
        }
    }
}