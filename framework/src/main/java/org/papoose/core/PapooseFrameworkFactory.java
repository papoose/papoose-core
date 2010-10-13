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

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

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

        //noinspection unchecked
        for (Map.Entry entry : (Set<Map.Entry>) configuration.entrySet())
        {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (key instanceof String && value instanceof String)
            {
                properties.setProperty((String) key, (String) value);
            }
        }

        String storeTypeString = properties.getProperty(PapooseConstants.PAPOOSE_FRAMEWORK_STORE_TYPE, "FILE");

        Store store;
        if ("MEMORY".equalsIgnoreCase(storeTypeString))
        {
            store = new MemoryStore();
        }
        else
        {
            String storageString = properties.getProperty(Constants.FRAMEWORK_STORAGE, ".");

            File file = new File(storageString).getAbsoluteFile();
            store = new FileStore(file);

            if (!"FILE".equals(storeTypeString))
            {
                LOGGER.warning("Unable to parse " + PapooseConstants.PAPOOSE_FRAMEWORK_STORE_TYPE + ", using file store");
            }
        }

        String storageCleanString = properties.getProperty(Constants.FRAMEWORK_STORAGE_CLEAN, "");
        if (Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT.equalsIgnoreCase(storageCleanString))
        {
            try
            {
                store.clear();
            }
            catch (PapooseException pe)
            {
                LOGGER.log(Level.WARNING, "Unable to clean framework storage", pe);
            }
        }

        String threadPoolSizeString = properties.getProperty(PapooseConstants.PAPOOSE_FRAMEWORK_THREADPOOL_SIZE, "5");
        int threadPoolSize;

        try
        {
            threadPoolSize = Integer.parseInt(threadPoolSizeString);
        }
        catch (NumberFormatException e)
        {
            LOGGER.warning("Unable to parse " + PapooseConstants.PAPOOSE_FRAMEWORK_THREADPOOL_SIZE + ", using default of 5");
            threadPoolSize = 5;
        }

        ExecutorService executorService = new ThreadPoolExecutor(1, threadPoolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        String frameworkName = properties.getProperty(PapooseConstants.PAPOOSE_FRAMEWORK_NAME);

        Papoose papoose;
        if (frameworkName != null)
        {
            papoose = new Papoose(frameworkName, store, executorService, properties);
        }
        else
        {
            papoose = new Papoose(store, executorService, properties);
        }

        Framework framework = new PapooseFramework(papoose);

        LOGGER.exiting(CLASS_NAME, "newFramework", framework);

        return framework;
    }
}
