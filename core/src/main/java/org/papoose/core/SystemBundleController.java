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

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.papoose.core.spi.BundleStore;


/**
 * @version $Revision$ $Date$
 */
class SystemBundleController extends BundleController
{
    private final static String CLASS_NAME = SystemBundleController.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    SystemBundleController(Papoose framework, BundleStore bundleStore, Version version) throws BundleException
    {
        super(framework, bundleStore);

        SystemArchiveStore archiveStore = new SystemArchiveStore(framework, version);

        BundleGeneration bundleGeneration = new BundleGeneration(this, archiveStore);

        getGenerations().put(0, bundleGeneration);
        setCurrentGeneration(bundleGeneration);

        bundleGeneration.setState(RESOLVED);

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("framework: " + framework);
            LOGGER.config("bundleStore: " + bundleStore);
            LOGGER.config("version: " + version);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getState()
    {
        return getFramework().getState();
    }

    /**
     * Does nothing because the system bundle is already started.
     *
     * @throws BundleException if there was an error
     */
    public void start() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "start");

        try
        {
            getFramework().start();
        }
        catch (PapooseException pe)
        {
            throw new BundleException("Unable to start framework", pe);
        }

        LOGGER.exiting(CLASS_NAME, "start");
    }

    /**
     * Does nothing because the system bundle is already started.
     *
     * @param options options usually used to control startup behavior
     * @throws BundleException if there was an error
     */
    public void start(int options) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "start", options);

        start();

        LOGGER.exiting(CLASS_NAME, "start");
    }

    public void stop() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "stop");

        try
        {
            getFramework().stop();
        }
        catch (PapooseException pe)
        {
            throw new BundleException("Unable to stop framework", pe);
        }

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    public void stop(final int options) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "stop", options);

        stop();

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    public void uninstall() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "uninstall");

        BundleException be = new BundleException("System bundle cannot be uninstalled");

        LOGGER.throwing(CLASS_NAME, "uninstall", be);

        throw be;
    }

    public void update(InputStream inputStream) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "update", inputStream);

        update();

        LOGGER.exiting(CLASS_NAME, "update");
    }

    public void update() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "update");

        try
        {
            getFramework().update();
        }
        catch (PapooseException pe)
        {
            throw new BundleException("Unable to update framework", pe);
        }

        LOGGER.exiting(CLASS_NAME, "update");
    }
}
