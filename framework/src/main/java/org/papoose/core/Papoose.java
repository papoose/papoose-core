/**
 *
 * Copyright 2007-2009 (C) The original author or authors
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
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.papoose.core.filter.Parser;
import org.papoose.core.spi.LocationMapper;
import org.papoose.core.spi.Resolver;
import org.papoose.core.spi.SecurityAdmin;
import org.papoose.core.spi.Store;
import org.papoose.core.util.ToStringCreator;


/**
 * @version $Revision$ $Date: $
 */
@ThreadSafe
public final class Papoose
{
    public final static String FRAMEWORK_VERSION = "1.4";
    public final static String PAPOOSE_VERSION = "org.papoose.framework.version";

    private final static String CLASS_NAME = Papoose.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    private final static Properties DEFAULTS = new Properties();

    private static volatile int FRAMEWORK_COUNTER = 0;
    private final static Map<Integer, Reference<Papoose>> FRAMEWORKS_BY_ID = new HashMap<Integer, Reference<Papoose>>();
    private final static Map<String, Reference<Papoose>> FRAMEWORKS_BY_NAME = new HashMap<String, Reference<Papoose>>();

    private final Object lock = new Object();
    private volatile boolean running = false;
    private final AccessControlContext acc = AccessController.getContext();
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final BundleManager bundleManager;
    private final ExecutorService executorService;
    private final Properties properties;
    private final String frameworkName;
    private final int frameworkId;
    private volatile long waitPeriod;
    @GuardedBy("lock")
    private volatile Parser parser = new Parser();
    @GuardedBy("lock")
    private volatile Resolver resolver = new DefaultResolver();
    private volatile SecurityAdmin securityAdmin;

    static
    {
        DEFAULTS.setProperty(Constants.FRAMEWORK_VERSION, FRAMEWORK_VERSION);
        DEFAULTS.setProperty(Constants.FRAMEWORK_VENDOR, "Papoose");

        String language = Locale.getDefault().getLanguage();
        DEFAULTS.setProperty(Constants.FRAMEWORK_LANGUAGE, (language.length() == 0 ? "en" : language));

        DEFAULTS.setProperty(Constants.FRAMEWORK_PROCESSOR, System.getProperty("os.arch"));
        DEFAULTS.setProperty(Constants.FRAMEWORK_OS_VERSION, standardizeVersion(System.getProperty("os.version")));
        DEFAULTS.setProperty(Constants.FRAMEWORK_OS_NAME, System.getProperty("os.name"));

        DEFAULTS.setProperty(Constants.SUPPORTS_FRAMEWORK_EXTENSION, Boolean.TRUE.toString());
        DEFAULTS.setProperty(Constants.SUPPORTS_BOOTCLASSPATH_EXTENSION, Boolean.TRUE.toString());
        DEFAULTS.setProperty(Constants.SUPPORTS_FRAMEWORK_FRAGMENT, Boolean.TRUE.toString());
        DEFAULTS.setProperty(Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE, Boolean.TRUE.toString());

        DEFAULTS.put(LocationMapper.LOCATION_MANAGER, new DefaultLocationMapper());
    }


    /**
     * Install the store, thread pool and setup a hierarchy of properties.
     * <p/>
     * The framework instance gets registered and is accessable via its unique
     * framework id.
     *
     * @param store           the bundle store to use
     * @param executorService the thread pool to use
     */
    public Papoose(Store store, ExecutorService executorService)
    {
        this(null, store, executorService, null);
    }

    /**
     * Install the store, thread pool and setup a hierarchy of properties.
     * <p/>
     * The framework instance gets registered and is accessable via its unique
     * framework id.
     *
     * @param store           the bundle store to use
     * @param executorService the thread pool to use
     * @param properties      the set of framework properties to use
     */
    public Papoose(Store store, ExecutorService executorService, Properties properties)
    {
        this(null, store, executorService, properties);
    }


    /**
     * Install the store, thread pool and setup a hierarchy of properties.
     * <p/>
     * The framework instance gets registered and is accessable via its unique
     * framework id.
     *
     * @param frameworkName   the name of this framework instance.  It must be
     *                        unique for the JVM that is has been instantiated
     *                        and it must be follow the same format as a bundle
     *                        symbolic name.
     * @param store           the bundle store to use
     * @param executorService the thread pool to use
     */
    public Papoose(String frameworkName, Store store, ExecutorService executorService)
    {
        this(frameworkName, store, executorService, null);

    }

    /**
     * Install the store, thread pool and setup a hierarchy of properties.
     * <p/>
     * The framework instance gets registered and is accessable via its unique
     * framework id.
     *
     * @param frameworkName   the name of this framework instance.  It must be
     *                        unique for the JVM that is has been instantiated
     *                        and it must be follow the same format as a bundle
     *                        symbolic name.
     * @param store           the bundle store to use
     * @param executorService the thread pool to use
     * @param properties      the set of framework properties to use
     */
    public Papoose(String frameworkName, Store store, ExecutorService executorService, Properties properties)
    {
        if (store == null) throw new IllegalArgumentException("store is null");
        if (executorService == null) throw new IllegalArgumentException("threadPool is null");

        this.frameworkId = FRAMEWORK_COUNTER++;

        if (frameworkName == null)
        {
            this.frameworkName = "Papoose." + frameworkId;
        }
        else
        {
            this.frameworkName = frameworkName;
        }

        if (FRAMEWORKS_BY_NAME.containsKey(this.frameworkName)) throw new IllegalArgumentException("Papoose instance with framework name " + this.frameworkName + " already registered");

        FRAMEWORKS_BY_ID.put(this.frameworkId, new WeakReference<Papoose>(this));
        FRAMEWORKS_BY_NAME.put(this.frameworkName, new WeakReference<Papoose>(this));

        this.bundleManager = new BundleManager(this, store);
        this.executorService = executorService;
        this.properties = assembleProperties(properties);

        if (LOGGER.isLoggable(Level.CONFIG))
        {
            LOGGER.config("Framework name: " + frameworkName);
            LOGGER.config("Framework ID: " + frameworkId);
            LOGGER.config("Framework store: " + store);
            LOGGER.config("Framework executor service: " + executorService);
            LOGGER.config("Framework properties: " + this.properties);
        }
    }

    AccessControlContext getAcc()
    {
        return acc;
    }

    ClassLoader getClassLoader()
    {
        return classLoader;
    }

    BundleManager getBundleManager()
    {
        return bundleManager;
    }

    ExecutorService getExecutorService()
    {
        return executorService;
    }

    String getFrameworkName()
    {
        return frameworkName;
    }

    int getFrameworkId()
    {
        return frameworkId;
    }

    public boolean isRunning()
    {
        return running;
    }

    public long getWaitPeriod()
    {
        return waitPeriod;
    }

    public void setWaitPeriod(long waitPeriod)
    {
        this.waitPeriod = waitPeriod;
    }

    /**
     * Get the filter parser that this framework instances uses.
     *
     * @return the filter parser that this framework instances uses
     */
    public Parser getParser()
    {
        return parser;
    }

    /**
     * Set the filter parser for this framework instance to use.  This allows
     * one to supply their own approximation comparison algorithm.
     *
     * @param parser the filter parser for this framework instance to use
     * @throws IllegalStateException if framework instance has already started
     * @see Parser
     */
    public void setParser(Parser parser)
    {
        if (parser == null) throw new IllegalArgumentException("Parser cannot be null");

        synchronized (lock)
        {
            if (running) throw new IllegalStateException("Cannot change parser after framework has started");

            this.parser = parser;

            if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("Framework parser: " + parser);
        }

    }

    Object getProperty(String key)
    {
        return properties.get(key);
    }

    public Properties getProperties()
    {
        return properties;
    }

    public Resolver getResolver()
    {
        return resolver;
    }

    public void setResolver(Resolver resolver)
    {
        if (resolver == null) throw new IllegalArgumentException("Resolver cannot be null");

        synchronized (lock)
        {
            if (running) throw new IllegalStateException("Cannot change resolver after framework has started");

            this.resolver = resolver;

            if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("Framework resolver: " + resolver);
        }
    }

    SecurityAdmin getSecurityAdmin()
    {
        return securityAdmin;
    }

    void setSecurityAdmin(SecurityAdmin securityAdmin)
    {
        this.securityAdmin = securityAdmin;
    }

    public BundleContext getSystemBundleContext()
    {
        LOGGER.entering(CLASS_NAME, "getSystemBundleContext");

        BundleContext systemBundleContext;

        synchronized (lock)
        {
            if (!running) throw new IllegalStateException("Framework has not started");

            BundleManager manager = getBundleManager();

            systemBundleContext = manager.getBundle(0).getBundleContext();
        }

        LOGGER.exiting(CLASS_NAME, "getSystemBundleContext", systemBundleContext);

        return systemBundleContext;
    }

    @GuardedBy("lock")
    public void start() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "start");

        synchronized (lock)
        {
            if (running)
            {
                LOGGER.warning("Framework already started");
                return;
            }

            BundleManager manager = getBundleManager();

            try
            {
                resolver.start(this);

                SystemBundleController systemBundleController = (SystemBundleController) manager.installSystemBundle(new Version(properties.getProperty(PAPOOSE_VERSION)));

                systemBundleController.performStart(0);

                running = true;
            }
            catch (PapooseException pe)
            {
                LOGGER.log(Level.SEVERE, "Framework startup exception", pe);
                manager.uninstallSystemBundle();
                throw pe;
            }
            catch (Exception e)
            {
                LOGGER.log(Level.SEVERE, "Framework startup exception", e);
                manager.uninstallSystemBundle();
                throw new PapooseException(e);
            }
        }

        LOGGER.exiting(CLASS_NAME, "start");
    }

    @GuardedBy("lock")
    public void stop() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "stop");

        synchronized (lock)
        {
            if (!running)
            {
                LOGGER.warning("Framework was not started");
                return;
            }

            BundleManager manager = getBundleManager();

            try
            {
                SystemBundleController system = (SystemBundleController) manager.getBundle(0);

                system.performStop(0);
            }
            finally
            {
                manager.uninstallSystemBundle();
                resolver.stop();
                running = false;
            }
        }

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("frameworkName", frameworkName);
        creator.append("frameworkId", frameworkId);
        creator.append("executorService", executorService);
        creator.append("resolver", resolver);

        return creator.toString();
    }

    static Papoose getFramework(Integer frameworkId)
    {
        Papoose result = FRAMEWORKS_BY_ID.get(frameworkId).get();

        if (result == null) FRAMEWORKS_BY_ID.remove(frameworkId);

        return result;
    }

    static Papoose getFramework(String name)
    {
        Papoose result = FRAMEWORKS_BY_NAME.get(name).get();

        if (result == null) FRAMEWORKS_BY_NAME.remove(name);

        return result;
    }

    private Properties assembleProperties(Properties properties)
    {
        Properties result = new Properties();

        result.putAll(DEFAULTS);

        InputStream inputStream = null;
        try
        {
            inputStream = Papoose.class.getClassLoader().getResourceAsStream("papoose.properties");
            if (inputStream != null)
            {
                result.load(inputStream);
            }
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.WARNING, "Error loading papoose.properties", ioe);
        }
        finally
        {
            try
            {
                if (inputStream != null) inputStream.close();
            }
            catch (IOException ioe)
            {
                LOGGER.log(Level.WARNING, "Error closing papoose.properties", ioe);
            }
        }

        result.putAll(System.getProperties());

        if (properties != null)
        {
            result.putAll(properties);
        }

        result.setProperty("org.papoose.framework.name", this.frameworkName);
        result.setProperty("org.papoose.framework.id", Integer.toString(this.frameworkId));

        return result;
    }

    /**
     * Attempt to convert the OS version from the System properties into a
     * canonical form.
     *
     * @param version the OS version as returned from the System properties
     * @return a canonical form of the OS version
     */
    private static String standardizeVersion(String version)
    {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        int digits = 0;

        do
        {
            while (i < version.length() && Character.isDigit(version.charAt(i)))
            {
                builder.append(version.charAt(i));
                i++;
            }
            if (++digits < 3)
            {
                if (i < version.length() && version.charAt(i) == '.')
                {
                    builder.append(version.charAt(i));
                    i++;
                }
                else
                {
                    break;
                }
            }
            else
            {
                break;
            }
        }
        while (true);

        return builder.toString();
    }
}
