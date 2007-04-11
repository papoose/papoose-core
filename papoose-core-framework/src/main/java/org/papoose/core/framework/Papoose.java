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

import java.util.logging.Logger;

import org.papoose.core.framework.spi.BundleManager;
import org.papoose.core.framework.spi.ThreadPool;


/**
 * @version $Revision$ $Date: $
 */
public final class Papoose
{
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final BundleManager bundleManager;
    private final ThreadPool threadPool;
    private long waitPeriod;

    public Papoose(BundleManager bundleManager, ThreadPool threadPool)
    {
        this.bundleManager = bundleManager;
        this.threadPool = threadPool;
    }

    BundleManager getBundleManager()
    {
        return bundleManager;
    }

    ThreadPool getThreadPool()
    {
        return threadPool;
    }

    public long getWaitPeriod()
    {
        return waitPeriod;
    }

    public void setWaitPeriod(long waitPeriod)
    {
        this.waitPeriod = waitPeriod;
    }

    public void start()
    {
        logger.entering(getClass().getName(), "start");

        logger.exiting(getClass().getName(), "start");
    }

    public void stop()
    {
        logger.entering(getClass().getName(), "stop");

        logger.exiting(getClass().getName(), "stop");
    }

    void unregisterServices(AbstractBundle bundle)
    {

    }

    void releaseServices(AbstractBundle bundle)
    {

    }
}
