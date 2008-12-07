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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleException;

import org.papoose.core.framework.Papoose;
import org.papoose.core.framework.PapooseException;
import org.papoose.core.framework.Util;
import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.Store;

/**
 * @version $Revision$ $Date$
 */
public class NonCachingFileStore implements Store
{
    private final static String CLASSNAME = NonCachingFileStore.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);
    private final static String BUNDLES_DIR = "bundles";
    private final static String DATA_DIR = "data";
    private final static String GENERATIONS_DIR = "generations";
    private final File root;

    public NonCachingFileStore(File root) throws PapooseException
    {
        this.root = root;

        if (!root.exists() && !root.mkdirs()) throw new PapooseException("Unable to create non-existant root: " + root);

        File bundlesRoot = new File(root, BUNDLES_DIR);
        if (!bundlesRoot.exists() && !bundlesRoot.mkdirs()) throw new PapooseException("Unable to create bundles root: " + bundlesRoot);
    }

    public File getRoot()
    {
        return root;
    }

    public List<BundleStore> loadBundleStores() throws BundleException
    {
        File bundlesRoot = new File(root, BUNDLES_DIR);
        List<BundleStore> result = new ArrayList<BundleStore>();

        for (String bundleId : bundlesRoot.list())
        {
            result.add(new NonCachingBundleFileStore(new File(bundlesRoot, bundleId), Long.valueOf(bundleId), "TODO")); // TODO
        }

        return result;
    }

    public BundleStore allocateBundleStore(long bundleId, String location) throws BundleException
    {
        File bundleRoot = new File(root, BUNDLES_DIR + File.separator + bundleId);

        if (bundleRoot.exists()) throw new BundleException("Bundle store location " + bundleRoot + " already exists");
        if (!bundleRoot.mkdirs()) throw new BundleException("Unable to create bundle store location: " + bundleRoot);

        BundleStore result = new NonCachingBundleFileStore(bundleRoot, bundleId, location);

        result.getDataRoot().mkdirs();

        return result;
    }

    public void removeBundleStore(long bundleId) throws BundleException
    {
        File bundleRoot = new File(root, BUNDLES_DIR + File.separator + bundleId);

        if (bundleRoot.exists()) Util.delete(bundleRoot);
    }

    public ArchiveStore allocateArchiveStore(Papoose framework, long bundleId, InputStream inputStream) throws BundleException
    {
        File archiveRoot = new File(root, BUNDLES_DIR + File.separator + bundleId + File.separator + 0); // TODO: This needs to  be fixed

        if (archiveRoot.exists()) throw new BundleException("Archive store location " + archiveRoot + " already exists");
        if (!archiveRoot.mkdirs()) throw new BundleException("Unable to create archive store location: " + archiveRoot);

        return new NonCachingArchiveStore(framework, bundleId, archiveRoot, inputStream);
    }

    public ArchiveStore loadArchiveStore(Papoose framework, long bundleId) throws BundleException
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "loadArchiveStore", new Object[]{ framework, bundleId });

        File archivesRoot = new File(root, BUNDLES_DIR + File.separator + bundleId + File.separator + GENERATIONS_DIR);
        ArchiveStore result = null;

        for (String generation : archivesRoot.list())
        {
            try
            {
                result = new NonCachingArchiveStore(framework, bundleId, new File(archivesRoot, generation)); // TODO: This needs to  be fixed
                break;
            }
            catch (NumberFormatException nfe)
            {
                LOGGER.log(Level.SEVERE, "Unable to parse generation id " + generation, nfe);
                throw new BundleException("Unable to parse generation id " + generation, nfe);
            }
        }

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "loadArchiveStore", result);

        return result;
    }

    public void pack(Papoose framework, long bundleId) throws BundleException
    {
        //todo: consider this autogenerated code
    }
}
