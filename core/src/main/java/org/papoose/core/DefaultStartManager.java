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
package org.papoose.core;

import java.util.logging.Logger;

import org.osgi.framework.BundleException;

import org.papoose.core.spi.StartManager;

/**
 * A simple default start manager that simply passes the request to start a
 * bundle to the bundle manager.
 */
public class DefaultStartManager implements StartManager
{
    private final static String CLASS_NAME = DefaultStartManager.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final BundleManager bundleManager;

    public DefaultStartManager(BundleManager bundleManager)
    {
        assert bundleManager != null;

        this.bundleManager = bundleManager;
    }

    public void start(BundleGeneration bundle, int options) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "start", new Object[]{ bundle, options });

        bundleManager.beginStart(bundle, options);

        LOGGER.exiting(CLASS_NAME, "start");
    }

    public void stop(BundleGeneration bundle, int options) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "stop", new Object[]{ bundle, options });

        bundleManager.beginStop(bundle, options);

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    public void setStartLevel(int startlevel)
    {
        LOGGER.entering(CLASS_NAME, "setStartLevel", startlevel);
        LOGGER.exiting(CLASS_NAME, "setStartLevel");
    }
}
