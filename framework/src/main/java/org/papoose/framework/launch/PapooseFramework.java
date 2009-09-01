/**
 *  Copyright 2009 Picateers Inc., 1720 S. Amphlett Boulevard  Suite 320, San Mateo, CA 94402 U.S.A. All rights reserved.
 */
package org.papoose.framework.launch;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import net.jcip.annotations.ThreadSafe;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.service.startlevel.StartLevel;
import org.papoose.core.Papoose;
import org.papoose.core.PapooseException;


/**
 * @version $Revision$ $Date$
 */
@ThreadSafe
public class PapooseFramework implements Framework
{
    private final static String CLASS_NAME = PapooseFramework.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Object lock = new Object();
    private final Papoose framework;
    private volatile int state = RESOLVED;
    private volatile Bundle systemBundle;
    private volatile Future<FrameworkEvent> futureStop = new Future<FrameworkEvent>()
    {
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            return false;  //Todo: change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isCancelled()
        {
            return false;  //Todo: change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isDone()
        {
            return false;  //Todo: change body of implemented methods use File | Settings | File Templates.
        }

        public FrameworkEvent get() throws InterruptedException, ExecutionException
        {
            return null;  //Todo: change body of implemented methods use File | Settings | File Templates.
        }

        public FrameworkEvent get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
        {
            return null;  //Todo: change body of implemented methods use File | Settings | File Templates.
        }
    };

    PapooseFramework(Papoose framework)
    {
        assert framework != null;

        this.framework = framework;
    }

    public void init() throws BundleException
    {
        synchronized (lock)
        {
            LOGGER.entering(CLASS_NAME, "init");

            if (state != RESOLVED)
            {
                LOGGER.exiting(CLASS_NAME, "init");
                return;
            }

            try
            {
                framework.initialize();

                systemBundle = framework.getSystemBundleContext().getBundle();

                state = Bundle.STARTING;
            }
            catch (PapooseException pe)
            {
                state = RESOLVED;

                BundleException be = new BundleException("Unable to start framework", pe);
                LOGGER.throwing(CLASS_NAME, "init", be);
                throw be;
            }

            LOGGER.exiting(CLASS_NAME, "init");
        }
    }

    public FrameworkEvent waitForStop(long timeout) throws InterruptedException
    {
        LOGGER.entering(CLASS_NAME, "waitForStop", timeout);

        FrameworkEvent result;
        try
        {
            result = futureStop.get(timeout, TimeUnit.SECONDS);
        }
        catch (ExecutionException e)
        {
            result = new FrameworkEvent(FrameworkEvent.ERROR, this, e.getCause());
        }
        catch (TimeoutException e)
        {
            result = new FrameworkEvent(FrameworkEvent.ERROR, this, e.getCause());
        }

        LOGGER.exiting(CLASS_NAME, "waitForStop", result);

        return result;
    }

    public void start() throws BundleException
    {
        synchronized (lock)
        {
            LOGGER.entering(CLASS_NAME, "start");

            if (state == RESOLVED) init();

            BundleContext bundleContext = systemBundle.getBundleContext();
            ServiceReference reference = bundleContext.getServiceReference(StartLevel.class.getName());

            if (reference == null) throw new BundleException("Unable to obtain a reference to a start level service");

            try
            {
                framework.start();
            }
            catch (PapooseException e)
            {
                e.printStackTrace();  //Todo: change body of catch statement use File | Settings | File Templates.
            }

            //Todo: change body of implemented methods use File | Settings | File Templates.
            LOGGER.exiting(CLASS_NAME, "start");
        }
    }

    public int getState()
    {
        return state;
    }

    public void start(int options) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "start", options);

        start();

        LOGGER.exiting(CLASS_NAME, "start");
    }

    public void stop() throws BundleException
    {
        synchronized (lock)
        {
            LOGGER.entering(CLASS_NAME, "start");

            framework.getExecutorService().submit(new Runnable()
            {
                public void run()
                {
                    //Todo: change body of implemented methods use File | Settings | File Templates.
                    try
                    {
                        framework.terminate();
                    }
                    catch (PapooseException e)
                    {
                        framework.getBundleManager().fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, PapooseFramework.this, e));
                    }
                    finally
                    {
                        state = Bundle.RESOLVED;
                    }
                }
            });

            LOGGER.exiting(CLASS_NAME, "start");
        }
    }

    public void stop(int options) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "stop", options);

        stop();

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    public void uninstall() throws BundleException
    {
        throw new BundleException("Should not have called uninstall on framework instance");
    }

    public Dictionary getHeaders()
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getHeaders();
    }

    public void update() throws BundleException
    {
        //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public void update(InputStream in) throws BundleException
    {
        try
        {
            in.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();  //Todo: change body of catch statement use File | Settings | File Templates.
        }
        //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public long getBundleId()
    {
        return 0;
    }

    public String getLocation()
    {
        return Constants.SYSTEM_BUNDLE_LOCATION;
    }

    public ServiceReference[] getRegisteredServices()
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getRegisteredServices();
    }

    public ServiceReference[] getServicesInUse()
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getServicesInUse();
    }

    public boolean hasPermission(Object permission)
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.hasPermission(permission);
    }

    public URL getResource(String name)
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getResource(name);
    }

    public Dictionary getHeaders(String locale)
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getHeaders(locale);
    }

    public String getSymbolicName()
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return framework.getSystemBundleContext().getBundle().getSymbolicName();
    }

    public Class loadClass(String name) throws ClassNotFoundException
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.loadClass(name);
    }

    public Enumeration getResources(String name) throws IOException
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getResources(CLASS_NAME);
    }

    public Enumeration getEntryPaths(String path)
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getEntryPaths(path);
    }

    public URL getEntry(String path)
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getEntry(path);
    }

    public long getLastModified()
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getLastModified();
    }

    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.findEntries(path, filePattern, recurse);
    }

    public BundleContext getBundleContext()
    {
        if (systemBundle == null) throw new IllegalStateException("Framework has not been initialzied");
        return systemBundle.getBundleContext();
    }
}
