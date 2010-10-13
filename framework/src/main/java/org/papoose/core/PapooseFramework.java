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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;

import org.papoose.core.util.Util;


/**
 * @version $Revision$ $Date$
 */
class PapooseFramework implements Framework
{
    private final static String CLASS_NAME = PapooseFramework.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Object lock = new Object();
    private final Papoose framework;
    private final AtomicReference<SystemBundleController> systemBundle = new AtomicReference<SystemBundleController>();
    private volatile NonDaemonThread nonDaemon;

    PapooseFramework(Papoose framework)
    {
        assert framework != null;

        this.framework = framework;

        if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("framework: " + framework);
    }

    /**
     * {@inheritDoc}
     */
    public void init() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "init");

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(this, AdminPermission.EXECUTE));

        synchronized (lock)
        {
            try
            {
                framework.init();

                systemBundle.set((SystemBundleController) framework.getSystemBundleContext().getBundle());
            }
            catch (PapooseException pe)
            {
                BundleException be = new BundleException("Unable to start framework", pe);
                LOGGER.throwing(CLASS_NAME, "init", be);
                throw be;
            }
        }

        LOGGER.exiting(CLASS_NAME, "init");
    }

    /**
     * {@inheritDoc}
     */
    public FrameworkEvent waitForStop(long timeout) throws InterruptedException
    {
        LOGGER.entering(CLASS_NAME, "waitForStop", timeout);

        if (systemBundle.get() == null) return new FrameworkEvent(FrameworkEvent.STOPPED, this, null); // todo: ???

        FrameworkEvent frameworkEvent = framework.waitForStop(timeout);

        LOGGER.exiting(CLASS_NAME, "waitForStop", frameworkEvent);

        return frameworkEvent;
    }

    /**
     * {@inheritDoc}
     */
    public void start() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "start");

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(this, AdminPermission.EXECUTE));

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        nonDaemon = new NonDaemonThread();
        Thread t = new Thread(nonDaemon, "Daemon thread for " + framework.getFrameworkName());
        t.setDaemon(false);
        t.start();

        systemBundle.get().addFrameworkListener(new FrameworkListener()
        {
            public void frameworkEvent(FrameworkEvent event)
            {
                if (event.getType() >= FrameworkEvent.STOPPED)
                {
                    nonDaemon.stop();
                    nonDaemon = null;
                }
            }
        });

        systemBundle.get().start();

        LOGGER.exiting(CLASS_NAME, "start");
    }

    /**
     * {@inheritDoc}
     */
    public int getState()
    {
        LOGGER.entering(CLASS_NAME, "getState");

        int result = systemBundle.get() == null ? INSTALLED : systemBundle.get().getState();

        LOGGER.exiting(CLASS_NAME, "getState", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void start(int options) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "start", options);

        start();

        LOGGER.exiting(CLASS_NAME, "start");
    }

    /**
     * {@inheritDoc}
     */
    public void stop() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "stop");

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(this, AdminPermission.EXECUTE));

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        systemBundle.get().stop();

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    /**
     * {@inheritDoc}
     */
    public void stop(int options) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "stop", options);

        stop();

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    /**
     * {@inheritDoc}
     */
    public void uninstall() throws BundleException
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(this, AdminPermission.LIFECYCLE));

        throw new BundleException("Should not have called uninstall on framework instance");
    }

    /**
     * {@inheritDoc}
     */
    public Dictionary getHeaders()
    {
        LOGGER.entering(CLASS_NAME, "getHeaders");

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(this, AdminPermission.METADATA));

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        Dictionary result = systemBundle.get().getHeaders();

        LOGGER.exiting(CLASS_NAME, "getHeaders", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void update() throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "update");

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(this, AdminPermission.LIFECYCLE));

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        systemBundle.get().update();

        LOGGER.exiting(CLASS_NAME, "update");
    }

    /**
     * {@inheritDoc}
     */
    public void update(InputStream in) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "update", in);

        Util.close(in);

        update();

        LOGGER.exiting(CLASS_NAME, "update");
    }

    /**
     * {@inheritDoc}
     */
    public long getBundleId()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getLocation()
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) sm.checkPermission(new AdminPermission(this, AdminPermission.METADATA));

        return Constants.SYSTEM_BUNDLE_LOCATION;
    }

    /**
     * {@inheritDoc}
     */
    public ServiceReference[] getRegisteredServices()
    {
        LOGGER.entering(CLASS_NAME, "getRegisteredServices");

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        ServiceReference[] result = systemBundle.get().getRegisteredServices();

        LOGGER.exiting(CLASS_NAME, "getRegisteredServices", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public ServiceReference[] getServicesInUse()
    {
        LOGGER.entering(CLASS_NAME, "getServicesInUse");

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        ServiceReference[] result = systemBundle.get().getServicesInUse();

        LOGGER.exiting(CLASS_NAME, "getServicesInUse", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPermission(Object permission)
    {
        LOGGER.entering(CLASS_NAME, "hasPermission", permission);

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        boolean result = systemBundle.get().hasPermission(permission);

        LOGGER.exiting(CLASS_NAME, "hasPermission", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public URL getResource(String name)
    {
        LOGGER.entering(CLASS_NAME, "getResource", name);

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        URL result = systemBundle.get().getResource(name);

        LOGGER.exiting(CLASS_NAME, "getResource", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Dictionary getHeaders(String locale)
    {
        LOGGER.entering(CLASS_NAME, "getHeaders", locale);

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        Dictionary result = systemBundle.get().getHeaders(locale);

        LOGGER.exiting(CLASS_NAME, "getHeaders", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getSymbolicName()
    {
        LOGGER.entering(CLASS_NAME, "getSymbolicName");

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        String result = systemBundle.get().getSymbolicName();

        LOGGER.exiting(CLASS_NAME, "getSymbolicName", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Class loadClass(String name) throws ClassNotFoundException
    {
        LOGGER.entering(CLASS_NAME, "loadClass", name);

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        Class result = systemBundle.get().loadClass(name);

        LOGGER.exiting(CLASS_NAME, "loadClass", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration getResources(String name) throws IOException
    {
        LOGGER.entering(CLASS_NAME, "getResources", name);

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        Enumeration result = systemBundle.get().getResources(name);

        LOGGER.exiting(CLASS_NAME, "getResources", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration getEntryPaths(String path)
    {
        LOGGER.entering(CLASS_NAME, "getEntryPaths", path);

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        Enumeration result = systemBundle.get().getEntryPaths(path);

        LOGGER.exiting(CLASS_NAME, "getEntryPaths", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public URL getEntry(String path)
    {
        LOGGER.entering(CLASS_NAME, "getEntry", path);

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        URL result = systemBundle.get().getEntry(path);

        LOGGER.exiting(CLASS_NAME, "getEntry", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastModified()
    {
        LOGGER.entering(CLASS_NAME, "getLastModified");

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        long result = systemBundle.get().getLastModified();

        LOGGER.exiting(CLASS_NAME, "getLastModified", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        LOGGER.entering(CLASS_NAME, "findEntries", new Object[]{ path, filePattern, recurse });

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        Enumeration result = systemBundle.get().findEntries(path, filePattern, recurse);

        LOGGER.exiting(CLASS_NAME, "findEntries", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public BundleContext getBundleContext()
    {
        LOGGER.entering(CLASS_NAME, "getBundleContext");

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        BundleContext result = systemBundle.get().getBundleContext();

        LOGGER.exiting(CLASS_NAME, "getBundleContext", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Map getSignerCertificates(int signersType)
    {
        LOGGER.entering(CLASS_NAME, "getSignerCertificates");

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        Map result = systemBundle.get().getSignerCertificates(signersType);

        LOGGER.exiting(CLASS_NAME, "getSignerCertificates", result);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Version getVersion()
    {
        LOGGER.entering(CLASS_NAME, "getVersion");

        if (systemBundle.get() == null) throw new IllegalStateException("Framework has not been initialized");

        Version result = systemBundle.get().getVersion();

        LOGGER.exiting(CLASS_NAME, "getVersion", result);

        return result;
    }

    @Override
    public String toString()
    {
        return framework.toString();
    }
}
