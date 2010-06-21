/**
 *
 * Copyright 2010 (C) The original author or authors
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

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @version $Revision: $ $Date: $
 */
class NonDaemonThread implements Runnable
{
    private final static String CLASS_NAME = NonDaemonThread.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private volatile boolean stop = false;

    public void stop()
    {
        stop = true;
    }

    public void run()
    {
        LOGGER.entering(CLASS_NAME, "run");

        while (!stop)
        {
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException e)
            {
                LOGGER.log(Level.FINEST, "Non-daemon thread interrupted", e);
                stop = true;
                Thread.currentThread().interrupt();
            }
        }

        LOGGER.exiting(CLASS_NAME, "run");
    }
}
