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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import org.papoose.core.framework.filter.Parser;
import org.papoose.core.framework.spi.BundleManager;
import org.papoose.core.framework.spi.Store;
import org.papoose.core.framework.spi.ThreadPool;


/**
 * @version $Revision$ $Date: $
 */
public final class Papoose
{
    private final Logger logger = Logger.getLogger(getClass().getName());

    private static long frameworkCounter = 0;
    private final static Map<Long, Reference<Papoose>> frameworks = new Hashtable<Long, Reference<Papoose>>();

    private final BundleManager bundleManager;
    private final ThreadPool threadPool;
    private final long frameworkId;
    private long waitPeriod;
    private Parser parser;

    public Papoose(Store store, ThreadPool threadPool)
    {
        if (store == null) throw new IllegalArgumentException("store is null");
        if (threadPool == null) throw new IllegalArgumentException("threadPool is null");

        this.bundleManager = new BundleManagerImpl(this, store);
        this.threadPool = threadPool;
        this.frameworkId = frameworkCounter++;

        frameworks.put(frameworkId, new WeakReference<Papoose>(this));
    }

    BundleManager getBundleManager()
    {
        return bundleManager;
    }

    ThreadPool getThreadPool()
    {
        return threadPool;
    }

    long getFrameworkId()
    {
        return frameworkId;
    }

    public long getWaitPeriod()
    {
        return waitPeriod;
    }

    public void setWaitPeriod(long waitPeriod)
    {
        this.waitPeriod = waitPeriod;
    }

    public Parser getParser()
    {
        return parser;
    }

    public void setParser(Parser parser)
    {
        this.parser = parser;
    }

    String getProperty(String key)
    {
        return null; // TODO: fix me
    }

    static Papoose getFramework(Long frameworkId)
    {
        Papoose result = frameworks.get(frameworkId).get();

        if (result == null) frameworks.remove(frameworkId);

        return result;
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

    Wire resolve(ImportDescription importDescription)
    {
        return null;
    }

}
