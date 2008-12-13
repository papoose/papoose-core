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
package org.papoose.store.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleException;
import org.papoose.core.framework.FatalError;
import org.papoose.core.framework.Papoose;
import org.papoose.core.framework.Util;
import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.Store;
import org.papoose.core.framework.util.FileUtils;
import org.papoose.core.framework.util.ToStringCreator;


/**
 * @version $Revision$ $Date$
 */
public class NonCachingFileStore implements Store
{
    private final static String CLASS_NAME = NonCachingFileStore.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final static String PROPERTIES_FILE = "store.properties";
    private final static String GENERATION_KEY = "generation.";
    private final static String BUNDLES_DIR = "bundles";
    private final static String GENERATIONS_DIR = "generations";
    private final Properties properties = new Properties();
    private final File root;

    public NonCachingFileStore(File root)
    {
        LOGGER.entering(CLASS_NAME, "NonCachingFileStore");

        assert root != null;

        this.root = root;

        if (!root.exists())
        {
            if (!root.mkdirs()) throw new FatalError("Unable to create non-existant root: " + root);
            save();
        }
        else
        {
            load();
        }

        File bundlesRoot = new File(root, BUNDLES_DIR);
        if (!bundlesRoot.exists() && !bundlesRoot.mkdirs()) throw new FatalError("Unable to create bundles root: " + bundlesRoot);

        if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("root: " + root);

        LOGGER.exiting(CLASS_NAME, "NonCachingFileStore");
    }

    public File getRoot()
    {
        return root;
    }

    public synchronized List<BundleStore> loadBundleStores() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "loadBundleStores");

        File bundlesRoot = new File(root, BUNDLES_DIR);
        List<BundleStore> result = new ArrayList<BundleStore>();

        for (String bundleId : bundlesRoot.list())
        {
            if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Loading bundle id " + bundleId);

            try
            {
                result.add(new NonCachingBundleFileStore(new File(bundlesRoot, bundleId), Long.valueOf(bundleId)));
            }
            catch (BundleException be)
            {
                LOGGER.log(Level.WARNING, "Unable to add non-caching bundle file store for bundle id " + bundleId, be);
            }
            catch (NumberFormatException nfe)
            {
                LOGGER.log(Level.SEVERE, "Unable to add non-caching bundle file store for bundle id " + bundleId, nfe);
                throw new FatalError("Unable to add non-caching bundle file store for bundle id " + bundleId);
            }
        }

        LOGGER.exiting(CLASS_NAME, "loadBundleStores", result);

        return result;

    }

    public synchronized BundleStore allocateBundleStore(long bundleId, String location) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "allocateBundleStore", new Object[]{bundleId, location});

        assert location != null;

        File bundleRoot = FileUtils.buildPath(root, BUNDLES_DIR, bundleId);

        if (bundleRoot.exists()) throw new BundleException("Bundle store location " + bundleRoot + " already exists");
        if (!bundleRoot.mkdirs()) throw new FatalError("Unable to create bundle store location: " + bundleRoot);

        properties.setProperty(GENERATION_KEY + bundleId, "-1");

        NonCachingBundleFileStore result = new NonCachingBundleFileStore(bundleRoot, bundleId, location);

        save();

        LOGGER.exiting(CLASS_NAME, "allocateBundleStore", result);

        return result;

    }

    public synchronized void removeBundleStore(long bundleId) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "removeBundleStore", bundleId);

        File bundleRoot = FileUtils.buildPath(root, BUNDLES_DIR, bundleId);

        if (bundleRoot.exists())
        {
            Util.delete(bundleRoot);

            properties.remove(GENERATION_KEY + bundleId);

            save();
        }
        else
        {
            LOGGER.warning("Bundle root: " + bundleRoot + " never existed");
        }

        LOGGER.exiting(CLASS_NAME, "removeBundleStore");
    }

    public synchronized ArchiveStore allocateArchiveStore(Papoose framework, long bundleId, InputStream inputStream) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "allocateArchiveStore", new Object[]{framework, bundleId, inputStream});

        NonCachingArchiveStore result;
        try
        {
            int generation = Integer.parseInt(properties.getProperty(GENERATION_KEY + bundleId)) + 1;

            properties.setProperty(GENERATION_KEY + bundleId, Integer.toString(generation));

            File archiveRoot = FileUtils.buildPath(root, BUNDLES_DIR, bundleId, GENERATIONS_DIR, generation);

            if (archiveRoot.exists()) throw new FatalError("Archive store location " + archiveRoot + " already exists");
            if (!archiveRoot.mkdirs()) throw new FatalError("Unable to create archive store location: " + archiveRoot);

            result = new NonCachingArchiveStore(framework, bundleId, generation, archiveRoot, inputStream);

            save();
        }
        catch (NumberFormatException nfe)
        {
            LOGGER.log(Level.SEVERE, "Unable to obtain last generation", nfe);
            throw new FatalError("Unable to obtain last generation", nfe);
        }

        LOGGER.exiting(CLASS_NAME, "allocateArchiveStore", result);

        return result;
    }

    public synchronized ArchiveStore loadArchiveStore(Papoose framework, long bundleId) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "loadArchiveStore", new Object[]{framework, bundleId});

        File archivesRoot = FileUtils.buildPath(root, BUNDLES_DIR, bundleId, GENERATIONS_DIR);

        SortedSet<Integer> generations = new TreeSet<Integer>();

        for (String generation : archivesRoot.list())
        {
            try
            {
                generations.add(Integer.parseInt(generation));
            }
            catch (NumberFormatException nfe)
            {
                LOGGER.log(Level.SEVERE, "Unable to parse generation id " + generation, nfe);
                throw new FatalError("Unable to parse generation id " + generation, nfe);
            }
        }

        ArchiveStore result = null;
        if (!generations.isEmpty())
        {
            int current = generations.last();

            if (current != Integer.parseInt(properties.getProperty(GENERATION_KEY + bundleId))) throw new FatalError("Generation store inconsistent with bundle store");

            generations.remove(current);

            for (Integer generation : generations)
            {
                Util.delete(new File(archivesRoot, Integer.toString(generation)));
            }

            result = new NonCachingArchiveStore(framework, bundleId, current, new File(archivesRoot, Integer.toString(current)));
        }

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASS_NAME, "loadArchiveStore", result);

        return result;
    }

    private void load()
    {
        LOGGER.entering(CLASS_NAME, "load");

        try
        {
            properties.load(new FileInputStream(new File(this.root, PROPERTIES_FILE)));
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, "Unable to load bundle store state", ioe);
            throw new FatalError("Unable to save bundle store state", ioe);
        }

        LOGGER.exiting(CLASS_NAME, "load");
    }

    private void save()
    {
        LOGGER.entering(CLASS_NAME, "save");

        try
        {
            properties.store(new FileOutputStream(new File(this.root, PROPERTIES_FILE)), " bundle store state");
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, "Unable to save bundle store state", ioe);
            throw new FatalError("Unable to save bundle store state", ioe);
        }

        LOGGER.exiting(CLASS_NAME, "save");
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("root", root);
        creator.append("properties", properties);

        return creator.toString();
    }
}
