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
package org.papoose.store.memory;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import org.papoose.core.Papoose;
import org.papoose.core.PapooseException;
import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.spi.BundleStore;
import org.papoose.core.spi.Store;
import org.papoose.core.util.ToStringCreator;

/**
 * @version $Revision$ $Date$
 */
public class MemoryStore implements Store
{
    private final static String CLASS_NAME = MemoryStore.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final static String GENERATION_KEY = "generation.";
    private final Properties properties = new Properties();

    public MemoryStore()
    {
        LOGGER.entering(CLASS_NAME, "FileStore");

        LOGGER.exiting(CLASS_NAME, "FileStore");
    }

    public boolean isPreviouslyUsed() throws PapooseException
    {
        return false;
    }

    public void clear() throws PapooseException
    {
        properties.clear();
    }

    public void start() throws PapooseException
    {
    }

    public void stop() throws PapooseException
    {
    }

    public synchronized List<BundleStore> loadBundleStores() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "loadBundleStores");

        List<BundleStore> result = Collections.emptyList();

        LOGGER.exiting(CLASS_NAME, "loadBundleStores", result);

        return result;
    }

    public synchronized BundleStore obtainSystemBundleStore() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "obtainSystemBundleStore");

        BundleMemoryStore result = new BundleMemoryStore(0, Constants.SYSTEM_BUNDLE_LOCATION);

        LOGGER.exiting(CLASS_NAME, "obtainSystemBundleStore", result);

        return result;
    }

    public synchronized BundleStore allocateBundleStore(long bundleId, String location) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "allocateBundleStore", new Object[]{ bundleId, location });

        assert location != null;

        properties.setProperty(GENERATION_KEY + bundleId, "-1");

        BundleMemoryStore result = new BundleMemoryStore(bundleId, location);

        LOGGER.exiting(CLASS_NAME, "allocateBundleStore", result);

        return result;
    }

    public synchronized void removeBundleStore(long bundleId) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "removeBundleStore", bundleId);

        properties.remove(GENERATION_KEY + bundleId);

        LOGGER.exiting(CLASS_NAME, "removeBundleStore");
    }

    public synchronized ArchiveStore allocateArchiveStore(Papoose framework, long bundleId, InputStream inputStream) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "allocateArchiveStore", new Object[]{ framework, bundleId, inputStream });

        int generation = Integer.parseInt(properties.getProperty(GENERATION_KEY + bundleId)) + 1;

        properties.setProperty(GENERATION_KEY + bundleId, Integer.toString(generation));

        ArchiveMemoryStore result = new ArchiveMemoryStore(framework, bundleId, generation, inputStream);

        LOGGER.exiting(CLASS_NAME, "allocateArchiveStore", result);

        return result;
    }

    public synchronized ArchiveStore loadArchiveStore(Papoose framework, long bundleId) throws BundleException
    {
        throw new UnsupportedOperationException("Memory based store does not support loading persisted archive");
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("properties", properties);

        return creator.toString();
    }
}
