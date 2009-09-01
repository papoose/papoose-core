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
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.papoose.core.resolver.DefaultResolver;
import org.papoose.core.spi.LocationMapper;
import org.papoose.core.spi.Resolver;
import org.papoose.core.spi.StartManager;
import org.papoose.core.spi.Store;
import org.papoose.core.spi.TrustManager;
import org.papoose.core.util.ToStringCreator;
import org.papoose.core.util.Util;


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

    private static int FRAMEWORK_COUNTER = 0;
    private final static Map<Integer, Reference<Papoose>> FRAMEWORKS_BY_ID = new HashMap<Integer, Reference<Papoose>>();
    private final static Map<String, Reference<Papoose>> FRAMEWORKS_BY_NAME = new HashMap<String, Reference<Papoose>>();

    private final Object lock = new Object();
    private volatile State state = State.TERMINATED;
    private final AccessControlContext acc = AccessController.getContext();
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final BundleManager bundleManager;
    private final ServiceRegistry serviceRegistry;
    private final ExecutorService executorService;
    private final Properties clientProperties;
    private final String frameworkName;
    private final int frameworkId;
    private final long timestamp;
    private volatile Properties properties;
    private volatile long waitPeriod;
    private volatile int startLevel;
    private volatile String[] bootDelegates;
    @GuardedBy("lock") private volatile Parser parser = new Parser();
    @GuardedBy("lock") private volatile TrustManager trustManager = new DefaultTrustManager();
    @GuardedBy("lock") private volatile Resolver resolver = new DefaultResolver();
    private final List<Object> bootServices = new ArrayList<Object>();

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

        this.timestamp = System.currentTimeMillis();

        synchronized (FRAMEWORKS_BY_NAME)
        {
            ensureUrlHandling();

            this.frameworkId = FRAMEWORK_COUNTER++;

            if (frameworkName == null)
            {
                this.frameworkName = "org.papoose.framework-" + frameworkId;
            }
            else
            {
                this.frameworkName = frameworkName;
            }

            if (FRAMEWORKS_BY_NAME.containsKey(this.frameworkName)) throw new IllegalArgumentException("Papoose instance with framework name " + this.frameworkName + " already registered");

            FRAMEWORKS_BY_ID.put(this.frameworkId, new WeakReference<Papoose>(this));
            FRAMEWORKS_BY_NAME.put(this.frameworkName, new WeakReference<Papoose>(this));
        }

        this.bundleManager = new BundleManager(this, store);
        this.serviceRegistry = new ServiceRegistry(this);
        this.executorService = executorService;
        this.clientProperties = properties;
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

    public BundleManager getBundleManager()
    {
        return bundleManager;
    }

    ServiceRegistry getServiceRegistry()
    {
        return serviceRegistry;
    }

    public ExecutorService getExecutorService()
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

    long getTimestamp()
    {
        return timestamp;
    }

    public int getStartLevel()
    {
        return startLevel;
    }

    public void setStartLevel(int startLevel)
    {
        if (startLevel < 1) throw new IllegalArgumentException("Invalid start level");

        if (state == State.STARTED) LOGGER.warning("Framework already started, new start level will be ignored");

        this.startLevel = startLevel;
    }

    public long getWaitPeriod()
    {
        return waitPeriod;
    }

    public void setWaitPeriod(long waitPeriod)
    {
        this.waitPeriod = waitPeriod;
    }

    public String[] getBootDelegates()
    {
        String[] result = new String[bootDelegates.length];

        System.arraycopy(bootDelegates, 0, result, 0, result.length);

        return result;
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
            if (state != State.TERMINATED) throw new IllegalStateException("Cannot change parser after framework has initialzied");

            this.parser = parser;

            if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("Framework parser: " + parser);
        }
    }

    public TrustManager getTrustManager()
    {
        return trustManager;
    }

    public void setTrustManager(TrustManager trustManager)
    {
        if (trustManager == null) throw new IllegalArgumentException("Trust manager cannot be null");

        synchronized (lock)
        {
            if (state != State.TERMINATED) throw new IllegalStateException("Cannot change trust manager after framework has initialzied");

            this.trustManager = trustManager;

            if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("Trust manager: " + trustManager);
        }
    }

    public Properties getClientProperties()
    {
        return clientProperties;
    }

    public Object getProperty(String key)
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
            if (state != State.TERMINATED) throw new IllegalStateException("Cannot change trust manager after framework has initialzied");

            this.resolver = resolver;

            if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("Framework resolver: " + resolver);
        }
    }

    public BundleContext getSystemBundleContext()
    {
        LOGGER.entering(CLASS_NAME, "getSystemBundleContext");

        BundleContext systemBundleContext;

        synchronized (lock)
        {
            if (state == State.TERMINATED) throw new IllegalStateException("Framework has not been initialzied");

            BundleManager manager = getBundleManager();

            /**
             * Keep this redundant cast in place.  It's a kludgy workaround due
             * to the fact that Equinox bundles v4.0 OSGi R4 spec classes in
             * with its framework.  This breaks the Spring OSGi integration
             * tests.
             */
            //noinspection RedundantCast
            systemBundleContext = ((BundleController) manager.getBundle(0)).getBundleContext();
        }

        LOGGER.exiting(CLASS_NAME, "getSystemBundleContext", systemBundleContext);

        return systemBundleContext;
    }

    @GuardedBy("lock")
    public void initialize() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "initialize");

        synchronized (lock)
        {
            if (state != State.TERMINATED)
            {
                LOGGER.warning("Framework already initialized");
                return;
            }

            BundleManager manager = getBundleManager();

            try
            {
                properties = assembleProperties(clientProperties);

                String bootDelegateString = properties.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
                if (bootDelegateString == null)
                {
                    bootDelegateString = "org.papoose.*";
                }
                else
                {
                    bootDelegateString += ",org.papoose.*";
                }

                bootDelegates = bootDelegateString.split(",");

                for (int i = 0; i < bootDelegates.length; i++)
                {
                    bootDelegates[i] = bootDelegates[i].trim();
                    if (bootDelegates[i].endsWith(".*")) bootDelegates[i] = bootDelegates[i].substring(0, bootDelegates[i].length() - 1);
                }

                resolver.start(this);

                SystemBundleController systemBundleController = (SystemBundleController) manager.installSystemBundle(new Version(properties.getProperty(PAPOOSE_VERSION)));

                systemBundleController.performStart(0);

                serviceRegistry.start();

                startBootLevelServices();

                state = State.INITIALIZED;
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

        LOGGER.exiting(CLASS_NAME, "initialize");
    }

    @GuardedBy("lock")
    public void start() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "start");

        synchronized (lock)
        {
            if (state == State.STARTED)
            {
                LOGGER.warning("Framework already started");
                return;
            }

            if (state == State.TERMINATED)
            {
                initialize();
            }

            BundleManager manager = getBundleManager();
            StartManager startManager = manager.getStartManager();

            startManager.setStartLevel(startLevel);

            state = State.STARTED;
        }

        LOGGER.exiting(CLASS_NAME, "start");
    }

    @GuardedBy("lock")
    public void stop() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "stop");

        synchronized (lock)
        {
            if (state != State.STARTED)
            {
                LOGGER.warning("Framework was not started");
                return;
            }

            BundleManager manager = getBundleManager();
            StartManager startManager = manager.getStartManager();

            startManager.setStartLevel(0);

            state = State.INITIALIZED;
        }

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    @GuardedBy("lock")
    public void terminate() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "terminate");

        synchronized (lock)
        {
            if (state == State.TERMINATED)
            {
                LOGGER.warning("Framework was not started");
                return;
            }

            if (state == State.STARTED)
            {
                stop();
            }

            BundleManager manager = getBundleManager();

            try
            {
                stopBootLevelServices();

                SystemBundleController system = (SystemBundleController) manager.getBundle(0);

                system.performStop(0);
            }
            finally
            {
                manager.uninstallSystemBundle();
                serviceRegistry.stop();
                resolver.stop();

                state = State.TERMINATED;
            }
        }

        LOGGER.exiting(CLASS_NAME, "terminate");
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("frameworkName", frameworkName);
        creator.append("frameworkId", frameworkId);
        creator.append("executorService", executorService);
        creator.append("resolver", resolver);
        creator.append("state", state);

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

    private static void ensureUrlHandling()
    {
        LOGGER.entering(CLASS_NAME, "ensureUrlHandling");

        try
        {
            new URL("codesource://1:0@org.papoose.framework.0");

            LOGGER.finest("Handler for codesource protocol found");
        }
        catch (MalformedURLException e)
        {
            LOGGER.finest("Handler for codesource protocol not found");

            String prefixes = System.getProperty("java.protocol.handler.pkgs");

            if (prefixes == null)
            {
                prefixes = "org.papoose.core.protocols";
            }
            else
            {
                prefixes = prefixes + "|org.papoose.core.protocols";
            }

            if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("java.protocol.handler.pkgs: " + prefixes);

            System.setProperty("java.protocol.handler.pkgs", prefixes);

            try
            {
                new URL("codesource://1:0@org.papoose.framework-0");

                LOGGER.finest("Handler for codesource protocol found");
            }
            catch (MalformedURLException mue)
            {
                LOGGER.severe("Unable to pick up Papoose protocol handlers");

                throw new FatalError("Unable to pick up Papoose protocol handlers", mue);
            }
        }

        LOGGER.exiting(CLASS_NAME, "ensureUrlHandling");
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

    private void startBootLevelServices()
    {
        LOGGER.entering(CLASS_NAME, "startBootLevelServices");

        assert Thread.holdsLock(lock);

        String bootLevelServiceClassKeys = (String) getProperty(PapooseConstants.PAPOOSE_BOOT_LEVEL_SERVICES);
        if (bootLevelServiceClassKeys != null)
        {
            if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest(PapooseConstants.PAPOOSE_BOOT_LEVEL_SERVICES + ": " + bootLevelServiceClassKeys);

            for (String bootLevelServiceClassKey : bootLevelServiceClassKeys.split(","))
            {
                startBootLevelService(bootLevelServiceClassKey.trim());
            }
        }

        LOGGER.exiting(CLASS_NAME, "startBootLevelServices");
    }

    private void startBootLevelService(String bootLevelServiceClassKey)
    {
        LOGGER.entering(CLASS_NAME, "startBootLevelService", bootLevelServiceClassKey);

        String bootLevelServiceClassName = (String) getProperty(bootLevelServiceClassKey);
        if (bootLevelServiceClassName != null)
        {
            if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("bootLevelServiceClassName: " + bootLevelServiceClassName);

            try
            {
                Class bootLevelServiceClass = getSystemBundleContext().getBundle().loadClass(bootLevelServiceClassName);
                Object pojo = bootLevelServiceClass.newInstance();

                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Starting " + pojo);

                Util.callStart(pojo, this);

                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Starting " + pojo);

                bootServices.add(pojo);
            }
            catch (ClassNotFoundException e)
            {
                FatalError fe = new FatalError("Unable to instantiate " + bootLevelServiceClassName, e);
                LOGGER.throwing(CLASS_NAME, "startBootLevelService", fe);
                throw fe;
            }
            catch (IllegalAccessException e)
            {
                FatalError fe = new FatalError("Unable to instantiate " + bootLevelServiceClassName, e);
                LOGGER.throwing(CLASS_NAME, "startBootLevelService", fe);
                throw fe;
            }
            catch (InstantiationException e)
            {
                FatalError fe = new FatalError("Unable to instantiate " + bootLevelServiceClassName, e);
                LOGGER.throwing(CLASS_NAME, "startBootLevelService", fe);
                throw fe;
            }
            catch (NoSuchMethodException e)
            {
                FatalError fe = new FatalError("Unable to instantiate " + bootLevelServiceClassName, e);
                LOGGER.throwing(CLASS_NAME, "startBootLevelService", fe);
                throw fe;
            }
            catch (InvocationTargetException e)
            {
                FatalError fe = new FatalError("Unable to instantiate " + bootLevelServiceClassName, e);
                LOGGER.throwing(CLASS_NAME, "startBootLevelService", fe);
                throw fe;
            }
        }

        LOGGER.exiting(CLASS_NAME, "startBootLevelService");
    }

    private void stopBootLevelServices()
    {
        LOGGER.entering(CLASS_NAME, "stopBootLevelServices");

        assert Thread.holdsLock(lock);

        Object pojo = null;

        try
        {
            for (int i = bootServices.size() - 1; 0 < i; i--)
            {
                pojo = bootServices.get(i);

                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Stopping " + pojo);

                Util.callStop(pojo);

                if (LOGGER.isLoggable(Level.FINEST)) LOGGER.finest("Stopped " + pojo);
            }
        }
        catch (NoSuchMethodException e)
        {
            FatalError fe = new FatalError("Unable to stop " + pojo, e);
            LOGGER.throwing(CLASS_NAME, "stopBootLevelServices", fe);
            throw fe;
        }
        catch (IllegalAccessException e)
        {
            FatalError fe = new FatalError("Unable to stop " + pojo, e);
            LOGGER.throwing(CLASS_NAME, "stopBootLevelServices", fe);
            throw fe;
        }
        catch (InvocationTargetException e)
        {
            FatalError fe = new FatalError("Unable to stop " + pojo, e);
            LOGGER.throwing(CLASS_NAME, "stopBootLevelServices", fe);
            throw fe;
        }
        finally
        {
            bootServices.clear();
        }

        LOGGER.exiting(CLASS_NAME, "stopBootLevelServices");
    }

    /**
     *
     */
    private static enum State
    {
        TERMINATED,
        INITIALIZED,
        STARTED
    }
}
