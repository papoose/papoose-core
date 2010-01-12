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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.papoose.core.filter.Parser;
import org.papoose.core.resolver.DefaultResolver;
import org.papoose.core.spi.LocationMapper;
import org.papoose.core.spi.Resolver;
import org.papoose.core.spi.StartManager;
import org.papoose.core.spi.Store;
import org.papoose.core.spi.TrustManager;
import org.papoose.core.util.ToStringCreator;
import org.papoose.core.util.Util;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date: $
 */
public final class Papoose
{
    public final static String FRAMEWORK_VERSION = "1.5";

    private final static String CLASS_NAME = Papoose.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    private final static Properties DEFAULTS = new Properties();

    private static int FRAMEWORK_COUNTER = 0;
    private final static Map<Integer, Reference<Papoose>> FRAMEWORKS_BY_ID = new HashMap<Integer, Reference<Papoose>>();
    private final static Map<String, Reference<Papoose>> FRAMEWORKS_BY_NAME = new HashMap<String, Reference<Papoose>>();

    private final Object lock = new Object();
    private volatile State state = new Installed();
    private volatile FutureFrameworkEvent futureStop = new FutureFrameworkEvent();
    private final AccessControlContext acc = AccessController.getContext();
    private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final BundleManager bundleManager;
    private final ServiceRegistry serviceRegistry;
    private final ExecutorService executorService;
    private final Properties clientProperties;
    private final String frameworkName;
    private final int frameworkId;
    private volatile Properties properties;
    private volatile StartManager startManager;
    private volatile int startLevel;
    private volatile String[] bootDelegates;
    private volatile Parser parser = new Parser();
    private volatile TrustManager trustManager = new DefaultTrustManager();
    private volatile Resolver resolver = new DefaultResolver();
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
        this.startManager = new DefaultStartManager(bundleManager);
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

    public int getStartLevel()
    {
        return startLevel;
    }

    public StartManager getStartManager()
    {
        return startManager;
    }

    public void setStartManager(StartManager startManager)
    {
        synchronized (lock)
        {
            if (state.getState() >= Bundle.STARTING) throw new IllegalStateException("Cannot change start manager after framework has initialzied");

            if (startManager == null)
            {
                this.startManager = new DefaultStartManager(bundleManager);
            }
            else
            {
                this.startManager = startManager;
            }

            if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("Framework start manager: " + startManager);
        }
    }

    public void setStartLevel(int startLevel)
    {
        if (startLevel < 1) throw new IllegalArgumentException("Invalid start level");

        synchronized (lock)
        {
            if (state.getState() >= Bundle.STARTING) LOGGER.warning("Framework already started, new start level will be ignored");

            this.startLevel = startLevel;

            if (LOGGER.isLoggable(Level.CONFIG)) LOGGER.config("Framework start level: " + startLevel);
        }
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
            if (state.getState() >= Bundle.STARTING) throw new IllegalStateException("Cannot change parser after framework has initialzied");

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
            if (state.getState() >= Bundle.STARTING) throw new IllegalStateException("Cannot change trust manager after framework has initialzied");

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
            if (state.getState() >= Bundle.STARTING) throw new IllegalStateException("Cannot change resolver after framework has initialzied");

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
            if (state.getState() < Bundle.STARTING) throw new IllegalStateException("Framework has not been initialzied");

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

    public void init() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "initialize");

        synchronized (lock)
        {
            state.init();
        }

        LOGGER.exiting(CLASS_NAME, "initialize");
    }

    public void start() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "start");

        synchronized (lock)
        {
            state.start();
        }

        LOGGER.exiting(CLASS_NAME, "start");
    }

    public void stop() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "stop");

        synchronized (lock)
        {
            state.stop();
        }

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    public void update() throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "update");

        synchronized (lock)
        {
            state.update();
        }

        LOGGER.exiting(CLASS_NAME, "update");
    }

    FrameworkEvent waitForStop(long timeout) throws InterruptedException
    {
        LOGGER.entering(CLASS_NAME, "waitForStop", timeout);

        if (timeout < 0) throw new IllegalArgumentException("Timeout cannot be negative");

        BundleController systemBundleController = getBundleManager().getBundle(0);
        FrameworkEvent frameworkEvent;
        try
        {
            if (timeout == 0)
            {
                frameworkEvent = futureStop.get();
            }
            else
            {
                frameworkEvent = futureStop.get(timeout, TimeUnit.MILLISECONDS);
            }
        }
        catch (ExecutionException e)
        {
            frameworkEvent = new FrameworkEvent(FrameworkEvent.ERROR, systemBundleController, e.getCause());
        }
        catch (TimeoutException e)
        {
            frameworkEvent = new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, systemBundleController, null);
        }

        LOGGER.exiting(CLASS_NAME, "waitForStop", frameworkEvent);

        return frameworkEvent;
    }

    public void requestStart(BundleGeneration bundle, int options) throws BundleException
    {
        startManager.start(bundle, options);
    }

    public void requestStop(BundleGeneration bundle, int options) throws BundleException
    {
        startManager.stop(bundle, options);
    }

    private void doInitialze() throws PapooseException
    {
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

            manager.getStore().start();

            resolver.start(Papoose.this);

            serviceRegistry.start();

            startBootLevelServices();

            SystemBundleController systemBundleController = (SystemBundleController) manager.installSystemBundle(new Version(properties.getProperty(PapooseConstants.PAPOOSE_VERSION)));

            manager.loadBundles();

            manager.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, systemBundleController, null));
        }
        catch (Exception e)
        {
            LOGGER.log(Level.SEVERE, "Framework startup exception", e);
            throw new PapooseException(e);
        }
    }

    private void doStart() throws PapooseException
    {

    }

    private void doStop() throws PapooseException
    {
        BundleManager manager = getBundleManager();

        manager.unloadBundles();

        stopBootLevelServices();

        serviceRegistry.stop();

        resolver.stop();

        manager.getStore().stop();
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

    public int getState()
    {
        return state.getState();
    }

    private interface State
    {
        int getState();

        void init() throws PapooseException;

        void start() throws PapooseException;

        void stop() throws PapooseException;

        void update() throws PapooseException;
    }

    private class Installed implements State
    {
        public int getState()
        {
            return Bundle.INSTALLED;
        }

        public void init() throws PapooseException
        {
            doInitialze();

            state = new Starting();
        }

        public void start() throws PapooseException
        {
            futureStop = new FutureFrameworkEvent();

            doInitialze();
            doStart();

            state = new Active();
        }

        public void stop() throws PapooseException
        {
        }

        public void update() throws PapooseException
        {
        }

        @Override
        public String toString()
        {
            return "INSTALLED";
        }
    }

    private class Resolved implements State
    {
        public int getState()
        {
            return Bundle.RESOLVED;
        }

        public void init() throws PapooseException
        {
            doInitialze();

            state = new Starting();
        }

        public void start() throws PapooseException
        {
            futureStop = new FutureFrameworkEvent();

            doStart();

            state = new Active();
        }

        public void stop() throws PapooseException
        {
        }

        public void update() throws PapooseException
        {
        }

        @Override
        public String toString()
        {
            return "RESOLVED";
        }
    }

    private final class Starting implements State
    {
        public int getState()
        {
            return Bundle.STARTING;
        }

        public void init() throws PapooseException
        {
        }

        public void start() throws PapooseException
        {
            futureStop = new FutureFrameworkEvent();

            doStart();

            state = new Active();
        }

        public void stop() throws PapooseException
        {
            BundleController systemBundleController = getBundleManager().getBundle(0);

            doStop();

            state = new Resolved();

            futureStop.setFrameworkEvent(new FrameworkEvent(FrameworkEvent.STOPPED, systemBundleController, null));
        }

        public void update() throws PapooseException
        {
            futureStop = new FutureFrameworkEvent();

            doStart();

            state = new Active();
        }

        @Override
        public String toString()
        {
            return "STARTING";
        }
    }

    private final class Active implements State
    {
        private volatile CountDownLatch latch = new CountDownLatch(0);

        public int getState()
        {
            return Bundle.ACTIVE;
        }

        public void init() throws PapooseException
        {
        }

        public void start() throws PapooseException
        {
        }

        public void stop() throws PapooseException
        {
            LOGGER.entering(CLASS_NAME, "stop");

            try
            {
                latch.await();

                /**
                 * By the time we are let through, the running thread could have left the framework in an invalid state.
                 */
                if (state instanceof Invalid) throw new PapooseException("Framework is in an invalid state");

                final BundleController systemBundleController = getBundleManager().getBundle(0);
                latch = new CountDownLatch(1);
                executorService.submit(new Runnable()
                {
                    public void run()
                    {
                        synchronized (lock)
                        {
                            try
                            {
                                doStop();

                                state = new Resolved();

                                futureStop.setFrameworkEvent(new FrameworkEvent(FrameworkEvent.STOPPED, systemBundleController, null));
                            }
                            catch (Throwable throwable)
                            {
                                LOGGER.log(Level.SEVERE, "Problem stopping the framework", throwable);

                                state = new Invalid();

                                futureStop.setFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, systemBundleController, throwable));
                            }
                            finally
                            {
                                latch.countDown();
                            }
                        }
                    }
                });
            }
            catch (InterruptedException ie)
            {
                LOGGER.log(Level.WARNING, "Waiting for inflight update interrupted", ie);
                Thread.currentThread().interrupt();
            }

            state = new Stopping();


            LOGGER.exiting(CLASS_NAME, "stop");
        }

        public void update() throws PapooseException
        {
            LOGGER.entering(CLASS_NAME, "update");

            try
            {
                latch.await();

                /**
                 * By the time we are let through, the running thread could have left the framework in an invalid state.
                 */
                if (state instanceof Invalid) throw new PapooseException("Framework is in an invalid state");

                final BundleController systemBundleController = getBundleManager().getBundle(0);
                latch = new CountDownLatch(1);
                executorService.submit(new Runnable()
                {
                    public void run()
                    {
                        synchronized (lock)
                        {
                            try
                            {
                                doStop();

                                futureStop.setFrameworkEvent(new FrameworkEvent(FrameworkEvent.STOPPED_UPDATE, systemBundleController, null));

                                futureStop = new FutureFrameworkEvent();

                                doStart();
                            }
                            catch (Throwable throwable)
                            {
                                LOGGER.log(Level.SEVERE, "Problem stopping and starting (updating) the framework", throwable);

                                state = new Invalid();

                                futureStop.setFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, systemBundleController, throwable));

                                try
                                {
                                    doStop();
                                }
                                catch (Throwable t)
                                {
                                    LOGGER.log(Level.SEVERE, "Problem destroying the framework", t);
                                }
                            }
                            finally
                            {
                                latch.countDown();
                            }
                        }
                    }
                });
            }
            catch (InterruptedException ie)
            {
                LOGGER.log(Level.WARNING, "Waiting for inflight update interrupted", ie);
                Thread.currentThread().interrupt();
            }

            LOGGER.exiting(CLASS_NAME, "update");
        }

        @Override
        public String toString()
        {
            return "ACTIVE";
        }
    }

    private final class Stopping implements State
    {
        public void init() throws PapooseException
        {
        }

        public void start() throws PapooseException
        {
        }

        public int getState()
        {
            return Bundle.STOPPING;
        }

        public void stop() throws PapooseException
        {
        }

        public void update() throws PapooseException
        {
        }

        @Override
        public String toString()
        {
            return "STOPPING";
        }
    }

    private final class Invalid implements State
    {
        public void init() throws PapooseException
        {
            throw new PapooseException("Framework is in an invalid state");
        }

        public void start() throws PapooseException
        {
            throw new PapooseException("Framework is in an invalid state");
        }

        public int getState()
        {
            return Bundle.UNINSTALLED;
        }

        public void stop() throws PapooseException
        {
            throw new PapooseException("Framework is in an invalid state");
        }

        public void update() throws PapooseException
        {
            throw new PapooseException("Framework is in an invalid state");
        }

        @Override
        public String toString()
        {
            return "INVALID";
        }
    }
}
