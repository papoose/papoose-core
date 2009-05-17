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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleException;

import org.papoose.core.AutostartSetting;
import org.papoose.core.spi.BundleStore;

/**
 * @version $Revision$ $Date$
 */
public class BundleTmpFileMemoryStore implements BundleStore
{
    private final static String CLASS_NAME = BundleTmpFileMemoryStore.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    public final static String LOCATION_KEY = "location";
    public final static String AUTOSTART_KEY = "autostart";
    public final static String DATA_DIR = "data";
    private final File dataRoot;
    private final long bundleId;
    private final String location;
    private long lastModified;
    private AutostartSetting setting;

    public BundleTmpFileMemoryStore(File bundleRoot, long bundleId, String location) throws BundleException
    {
        assert bundleRoot != null;
        assert bundleId >= 0;
        assert location != null && location.trim().length() > 0;

        if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Creating file store for id " + bundleId + " in " + bundleRoot);

        this.dataRoot = new File(bundleRoot, DATA_DIR);
        this.bundleId = bundleId;
        this.location = location;
        this.lastModified = System.currentTimeMillis();
        this.setting = AutostartSetting.STOPPED;

        if (!dataRoot.exists() && !dataRoot.mkdirs()) throw new BundleException("Unable to create data directory for bundle id " + bundleId + " in " + bundleRoot);

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("Bundle id: " + bundleId);
            LOGGER.config("Location: " + location);
            LOGGER.config("Data root: " + dataRoot);
        }
    }

    public long getBundleId()
    {
        return bundleId;
    }

    public File getDataRoot()
    {
        return dataRoot;
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public void markModified()
    {
        lastModified = System.currentTimeMillis();
    }

    public String getLocation()
    {
        return location;
    }

    public AutostartSetting getAutostart()
    {
        return setting;
    }

    public void setAutoStart(AutostartSetting setting)
    {
        this.setting = setting;
    }
}