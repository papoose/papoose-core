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
package org.papoose.framework.launch;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import org.papoose.core.Papoose;
import org.papoose.core.spi.Store;
import org.papoose.store.file.FileStore;
import org.papoose.store.memory.MemoryStore;


/**
 * @version $Revision$ $Date$
 */
public class PapooseFrameworkFactory implements FrameworkFactory
{
    private final static String CLASS_NAME = PapooseFrameworkFactory.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public Framework newFramework(Map configuration)
    {
        LOGGER.entering(CLASS_NAME, "newFramework", configuration);

        if (configuration == null) configuration = Collections.EMPTY_MAP;

        Properties properties = new Properties();

        for (Object key : configuration.keySet())
        {
            Object value = configuration.get(key);
            if (key instanceof String && value instanceof String)
            {
                properties.setProperty((String) key, (String) value);
            }
        }

        String storeTypeString = properties.getProperty(PapooseFrameworkConstants.PAPOOSE_FRAMEWORK_STORE_TYPE, "FILE");

        Store store;
        if ("MEMORY".equals(storeTypeString))
        {
            store = new MemoryStore();
        }
        else
        {
            String storageString = properties.getProperty(PapooseFrameworkConstants.PAPOOSE_FRAMEWORK_STORAGE, ".");
            File file = new File(storageString);
            store = new FileStore(file);

            if (!"FILE".equals(storeTypeString))
            {
                LOGGER.warning("Unable to parse " + PapooseFrameworkConstants.PAPOOSE_FRAMEWORK_STORE_TYPE + ", using file store");
            }
        }

        String threadPoolSizeString = properties.getProperty(PapooseFrameworkConstants.PAPOOSE_FRAMEWORK_THREADPOOL_SIZE, "5");
        int threadPoolSize;

        try
        {
            threadPoolSize = Integer.parseInt(threadPoolSizeString);
        }
        catch (NumberFormatException e)
        {
            LOGGER.warning("Unable to parse " + PapooseFrameworkConstants.PAPOOSE_FRAMEWORK_THREADPOOL_SIZE + ", using default of 5");
            threadPoolSize = 5;
        }

        ExecutorService executorService = new ThreadPoolExecutor(1, threadPoolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        String frameworkName = properties.getProperty(PapooseFrameworkConstants.PAPOOSE_FRAMEWORK_NAME);

        Papoose papose;
        if (frameworkName != null)
        {
            papose = new Papoose(frameworkName, store, executorService, properties);
        }
        else
        {
            papose = new Papoose(store, executorService, properties);
        }

        Framework framework = new PapooseFramework(papose);

        LOGGER.exiting(CLASS_NAME, "newFramework", framework);

        return framework;
    }
}
