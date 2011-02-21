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

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkEvent;


/**
 *
 */
public class RefreshPackagesRunnable implements Runnable
{
    private final static String CLASSNAME = RefreshPackagesRunnable.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASSNAME);
    private final Papoose framework;
    private final Bundle[] bundles;

    public RefreshPackagesRunnable(Papoose framework, Bundle[] bundles)
    {
        assert framework != null;
        assert bundles != null;

        this.framework = framework;
        this.bundles = bundles;

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("framework: " + framework);
            LOGGER.config("bundles#: " + bundles.length);
        }
    }

    public void run()
    {
        if (LOGGER.isLoggable(Level.FINER)) LOGGER.entering(CLASSNAME, "run");

        BundleManager manager = framework.getBundleManager();

        try
        {
            manager.writeLock();
        }
        catch (InterruptedException ie)
        {
            framework.getBundleManager().fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, framework.getBundleManager().getBundle(0), ie));
            LOGGER.log(Level.WARNING, "Wait for write lock interrupted", ie);
            return;
        }

        try
        {
            Set<Bundle> old = new HashSet<Bundle>();
            for (Bundle bundle : bundles)
            {
            }

            old = searchz(old);

            for (Bundle bundle : old)
            {

            }

            framework.getBundleManager().fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, framework.getBundleManager().getBundle(0), null));
            //todo: consider this autogenerated code
        }
        finally
        {
            manager.writeUnlock();
        }

        if (LOGGER.isLoggable(Level.FINER)) LOGGER.exiting(CLASSNAME, "run");
    }

    protected Set<Bundle> searchz(Set<Bundle> bundles)
    {
        return bundles;
    }
}
