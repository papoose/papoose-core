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
class BundleMemoryStore implements BundleStore
{
    private final static String CLASS_NAME = BundleMemoryStore.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final long bundleId;
    private final String location;
    private long lastModified;
    private AutostartSetting setting;

    BundleMemoryStore(long bundleId, String location) throws BundleException
    {
        assert bundleId >= 0;
        assert location != null && location.trim().length() > 0;

        this.bundleId = bundleId;
        this.location = location;
        this.lastModified = System.currentTimeMillis();
        this.setting = AutostartSetting.STOPPED;

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("Bundle id: " + bundleId);
            LOGGER.config("Location: " + location);
        }
    }

    public long getBundleId()
    {
        return bundleId;
    }

    public File getDataRoot()
    {
        throw new UnsupportedOperationException("Memory based bundle store does not support data roots");
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
