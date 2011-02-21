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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.papoose.core.spi.StartLevelStore;


/**
 *
 */
public class DefaultStartLevelStore implements StartLevelStore
{
    private final static String CLASS_NAME = DefaultStartLevelStore.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final static String INITIAL_START_LEVEL = "initial";
    private final static String BUNDLE_START_LEVEL = "bundle";
    private final Properties properties = new Properties();
    private volatile File file;

    public void start(Papoose framework)
    {
        BundleContext bundleContext = framework.getSystemBundleContext();
        file = bundleContext.getDataFile("start-level.properties");

        if (file.exists())
        {
            load();
        }
        else
        {
            properties.put(INITIAL_START_LEVEL, "1");
            save();
        }
    }

    public void stop()
    {
        file = null;
    }

    public int getBundleStartLevel(Bundle bundle)
    {
        try
        {
            return Integer.valueOf(properties.getProperty(BUNDLE_START_LEVEL + "." + bundle.getBundleId()));
        }
        catch (NumberFormatException e)
        {
            return Integer.valueOf(properties.getProperty(INITIAL_START_LEVEL));
        }
    }

    public void setBundleStartLevel(Bundle bundle, int bundleStartLevel)
    {
        properties.put(BUNDLE_START_LEVEL + "." + bundle.getBundleId(), Integer.toString(bundleStartLevel));
        save();
    }

    public void clearBundleStartLevel(Bundle bundle)
    {
        properties.remove(BUNDLE_START_LEVEL + "." + bundle.getBundleId());
        save();
    }

    public int getInitialBundleStartLevel()
    {
        return Integer.valueOf(properties.getProperty(INITIAL_START_LEVEL));
    }

    public void setInitialBundleStartLevel(int initialBundleStartLevel)
    {
        properties.put(INITIAL_START_LEVEL, Integer.toString(initialBundleStartLevel));
        save();
    }

    private void load()
    {
        InputStream in = null;
        try
        {
            in = new FileInputStream(file);
            properties.load(in);
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, "Error loading " + file, ioe);
            throw new FatalError("Error loading " + file, ioe);
        }
        finally
        {
            if (in != null)
            {
                try
                {
                    in.close();
                }
                catch (IOException ioe)
                {
                    LOGGER.log(Level.WARNING, "Error loading " + file, ioe);
                }
            }
        }
    }

    private void save()
    {
        OutputStream out = null;
        try
        {
            out = new FileOutputStream(file);
            properties.store(out, "Start level properties saved at " + new Date());
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.SEVERE, "Error saving " + file, ioe);
            throw new FatalError("Error saving " + file, ioe);
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException ioe)
                {
                    LOGGER.log(Level.WARNING, "Error saving " + file, ioe);
                }
            }
        }
    }
}
