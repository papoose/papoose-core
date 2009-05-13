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
package org.papoose.core;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;


/**
 * Instantiates an instance of the <code>PackageAdminImpl</code> and registers
 * the <code>PackageAdmin</code>.  A <code>SynchronousBundleListener<code> is
 * registered so that the service can keep track of bundle state changes
 * without a tight coupling to the Bundle manager's internals.
 *
 * @version $Revision$ $Date$
 */
public class PackageAdminActivator implements BundleActivator
{
    private final static String CLASSNAME = PackageAdminActivator.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);
    private PackageAdminImpl packageAdmin;

    public void start(BundleContext bundleContext) throws Exception
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "start", bundleContext);

        if (!(bundleContext instanceof BundleContextImpl)) throw new IllegalArgumentException("Package Admin Service will only work with Papoose");

        BundleContextImpl bundleContextImpl = (BundleContextImpl) bundleContext;
        packageAdmin = new PackageAdminImpl();

        packageAdmin.start(bundleContextImpl.getFramework());

        bundleContext.registerService(PackageAdmin.class.getName(), packageAdmin, null);

        bundleContext.addBundleListener(packageAdmin);

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "start");
    }

    public void stop(BundleContext bundleContext) throws Exception
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "stop", bundleContext);

        packageAdmin.stop();

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "stop");
    }
}
