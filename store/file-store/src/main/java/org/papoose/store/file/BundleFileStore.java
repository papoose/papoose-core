/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleException;

import org.papoose.core.AutostartSetting;
import org.papoose.core.FatalError;
import org.papoose.core.spi.BundleStore;


/**
 * @version $Revision$ $Date$
 */
public class BundleFileStore implements BundleStore
{
    private final static String CLASS_NAME = BundleFileStore.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    public final static String LOCATION_KEY = "location";
    public final static String AUTOSTART_KEY = "autostart";
    public final static String DATA_DIR = "data";
    private final File bundleRoot;
    private final File dataRoot;
    private final long bundleId;
    private final String location;
    private long lastModified;
    private AutostartSetting setting;

    public BundleFileStore(File bundleRoot, long bundleId, String location) throws BundleException
    {
        assert bundleRoot != null;
        assert bundleId >= 0;
        assert location != null && location.trim().length() > 0;

        if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Creating file store for id " + bundleId + " in " + bundleRoot);

        this.bundleRoot = bundleRoot;
        this.dataRoot = new File(bundleRoot, DATA_DIR);
        this.bundleId = bundleId;
        this.location = location;
        this.lastModified = System.currentTimeMillis();
        this.setting = AutostartSetting.STOPPED;

        if (!dataRoot.exists() && !dataRoot.mkdirs()) throw new BundleException("Unable to create data directory for bundle id " + bundleId + " in " + bundleRoot);

        try
        {
            save();
        }
        catch (IOException ioe)
        {
            throw new BundleException("Unable to save bundle properties", ioe);
        }

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("Bundle id: " + bundleId);
            LOGGER.config("Location: " + location);
            LOGGER.config("Data root: " + dataRoot);
        }
    }

    public BundleFileStore(File bundleRoot, long bundleId) throws BundleException
    {
        assert bundleRoot != null;
        assert bundleId >= 0;

        if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Loading file store for id " + bundleId + " in " + bundleRoot);

        this.bundleRoot = bundleRoot;
        this.dataRoot = new File(bundleRoot, DATA_DIR);
        this.bundleId = bundleId;

        if (!dataRoot.exists()) throw new BundleException("Data directory does not exist for bundle id " + bundleId + " in " + bundleRoot);

        try
        {
            Properties properties = new Properties();
            properties.load(new FileInputStream(bundleRoot));
            this.location = properties.getProperty(LOCATION_KEY);
            this.lastModified = System.currentTimeMillis();
            this.setting = AutostartSetting.valueOf(properties.getProperty(AUTOSTART_KEY));
        }
        catch (IOException ioe)
        {
            throw new BundleException("Unable to load bundle properties", ioe);
        }
        catch (IllegalArgumentException iae)
        {
            throw new BundleException("Unable to load bundle properties", iae);
        }

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

        try
        {
            save();
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, "Unable to save settings", ioe);
            throw new FatalError("Unable to save settings", ioe);
        }
    }

    private void save() throws IOException
    {
        Properties properties = new Properties();
        properties.setProperty(LOCATION_KEY, location);
        properties.setProperty(AUTOSTART_KEY, setting.toString());
        properties.store(new FileOutputStream(new File(bundleRoot, "bundle.properties")), " bundle id: " + bundleId + " location: " + location);
    }
}
