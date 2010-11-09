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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import org.papoose.core.FatalError;
import org.papoose.core.Papoose;
import org.papoose.core.PapooseException;
import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.spi.BundleStore;
import org.papoose.core.spi.Store;
import org.papoose.core.util.FileUtils;
import org.papoose.core.util.ToStringCreator;

/**
 * @version $Revision$ $Date$
 */
public class TmpFileMemoryStore implements Store
{
    private final static String CLASS_NAME = TmpFileMemoryStore.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final static String PROPERTIES_FILE = "store.properties";
    private final static String GENERATION_KEY = "generation.";
    private final static String SYSTEM_DIR = "system";
    private final static String BUNDLES_DIR = "bundles";
    private final Properties properties = new Properties();
    private final File root;

    public TmpFileMemoryStore(File root)
    {
        LOGGER.entering(CLASS_NAME, "FileStore");

        if (root == null) throw new IllegalArgumentException("Root file for file store cannot be null");

        this.root = root;

        if (!root.exists())
        {
            if (!root.mkdirs()) throw new FatalError("Unable to create non-existent root: " + root);
            save();
        }
        else
        {
            load();
        }

        File bundlesRoot = new File(root, BUNDLES_DIR);
        if (!bundlesRoot.exists() && !bundlesRoot.mkdirs()) throw new FatalError("Unable to create bundles root: " + bundlesRoot);

        if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("root: " + root);

        LOGGER.exiting(CLASS_NAME, "FileStore");
    }

    public File getRoot()
    {
        return root;
    }

    public void clear() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "clear");

        File properties = new File(this.root, PROPERTIES_FILE);
        if (properties.exists())
        {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine(properties.toString() + " exists, will delete");

            if (!properties.delete())
            {
                PapooseException pe = new PapooseException("Unable to delete properties file");
                LOGGER.throwing(CLASS_NAME, "clear", pe);
                throw pe;
            }
        }

        File bundleRoot = FileUtils.buildPath(root, BUNDLES_DIR);
        if (bundleRoot.exists())
        {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine(bundleRoot.toString() + " exists, will delete");

            if (!FileUtils.delete(bundleRoot))
            {
                PapooseException pe = new PapooseException("Unable to delete properties file");
                LOGGER.throwing(CLASS_NAME, "clear", pe);
                throw pe;
            }
        }

        LOGGER.exiting(CLASS_NAME, "clear");
    }

    public void start() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "start");
        LOGGER.exiting(CLASS_NAME, "start");
    }

    public void stop() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "stop");
        LOGGER.exiting(CLASS_NAME, "stop");
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

        File bundleRoot = new File(root, SYSTEM_DIR);

        if (!bundleRoot.exists() && !bundleRoot.mkdirs()) throw new FatalError("Unable to create bundle store location: " + bundleRoot);

        BundleStore result = new BundleTmpFileMemoryStore(bundleRoot, 0, Constants.SYSTEM_BUNDLE_LOCATION);

        LOGGER.exiting(CLASS_NAME, "obtainSystemBundleStore", result);

        return result;
    }

    public synchronized BundleStore allocateBundleStore(long bundleId, String location) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "allocateBundleStore", new Object[]{ bundleId, location });

        assert location != null;

        File bundleRoot = FileUtils.buildPath(root, BUNDLES_DIR, bundleId);

        properties.setProperty(GENERATION_KEY + bundleId, "-1");

        save();

        if (bundleRoot.exists()) throw new BundleException("Bundle store location " + bundleRoot + " already exists");
        if (!bundleRoot.mkdirs()) throw new FatalError("Unable to create bundle store location: " + bundleRoot);

        BundleStore result = new BundleTmpFileMemoryStore(bundleRoot, bundleId, location);

        LOGGER.exiting(CLASS_NAME, "allocateBundleStore", result);

        return result;
    }

    public synchronized void removeBundleStore(long bundleId) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "removeBundleStore", bundleId);

        File bundleRoot = FileUtils.buildPath(root, BUNDLES_DIR, bundleId);

        if (bundleRoot.exists())
        {
            FileUtils.delete(bundleRoot);

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
        LOGGER.entering(CLASS_NAME, "allocateArchiveStore", new Object[]{ framework, bundleId, inputStream });

        ArchiveMemoryStore result;
        try
        {
            int generation = Integer.parseInt(properties.getProperty(GENERATION_KEY + bundleId)) + 1;

            properties.setProperty(GENERATION_KEY + bundleId, Integer.toString(generation));

            result = new ArchiveMemoryStore(framework, bundleId, generation, inputStream);

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
        throw new UnsupportedOperationException("Memory based store does not support loading persisted archive");
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
            properties.store(new FileOutputStream(new File(root, PROPERTIES_FILE)), " bundle store state");
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
