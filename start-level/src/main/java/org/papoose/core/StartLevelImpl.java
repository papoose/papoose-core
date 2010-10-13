/**
 *
 * Copyright 2008-2010 (C) The original author or authors
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
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.startlevel.StartLevel;

import org.papoose.core.spi.StartLevelStore;
import org.papoose.core.spi.StartManager;
import org.papoose.core.util.SerialExecutor;


/**
 * @version $Revision$ $Date$
 */
public class StartLevelImpl implements StartManager, StartLevel, SynchronousBundleListener
{
    private final static String CLASS_NAME = StartLevelImpl.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Object lock = new Object();
    private final Set<StartedState> started = new HashSet<StartedState>();
    private volatile Papoose framework;
    private volatile StartManager savedStartManager;
    private volatile SerialExecutor serialExecutor;
    private volatile StartLevelStore store;
    private volatile ServiceRegistration serviceRegistration;
    private volatile int startLevel = 0;
    private volatile int initialBundleStartLevel = 1;

    public void start(Papoose framework)
    {
        LOGGER.entering(CLASS_NAME, "start", framework);

        this.framework = framework;

        savedStartManager = framework.getStartManager();
        framework.setStartManager(this);

        this.serialExecutor = new SerialExecutor(framework.getExecutorService());

        BundleContext systemBundleContext = framework.getSystemBundleContext();

        ServiceReference reference = systemBundleContext.getServiceReference(StartLevelStore.class.getName());
        if (reference != null)
        {
            this.store = (StartLevelStore) systemBundleContext.getService(reference);
        }
        else
        {
            this.store = new DefaultStartLevelStore();
        }

        store.start(framework);

        systemBundleContext.addBundleListener(this);

        this.serviceRegistration = systemBundleContext.registerService(StartLevel.class.getName(), this, null);

        LOGGER.exiting(CLASS_NAME, "start");
    }

    public void stop()
    {
        LOGGER.entering(CLASS_NAME, "stop");

        serviceRegistration.unregister();

        BundleContext systemBundleContext = framework.getSystemBundleContext();

        systemBundleContext.removeBundleListener(this);

        store.stop();

        framework.setStartManager(savedStartManager);

        savedStartManager = null;
        serviceRegistration = null;
        store = null;
        framework = null;

        started.clear();

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    public void start(BundleGeneration bundle, int options) throws BundleException
    {
        synchronized (lock)
        {
            BundleController bundleController = bundle.getBundleController();

            if (startLevel >= getBundleStartLevel(bundleController))
            {
                BundleManager bundleManager = framework.getBundleManager();
                try
                {
                    bundleManager.beginStart((BundleGeneration) bundleController.getCurrentGeneration(), options);
                }
                catch (BundleException be)
                {
                    bundleManager.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundleController, be));
                }
            }

            started.add(new StartedState(bundleController, options));
        }
    }

    public void stop(BundleGeneration bundle, int options) throws BundleException
    {
        synchronized (lock)
        {
            BundleController bundleController = bundle.getBundleController();

            if (bundleController.getState() == Bundle.ACTIVE)
            {
                started.remove(new StartedState(bundleController));

                BundleManager bundleManager = framework.getBundleManager();
                try
                {
                    bundleManager.beginStop((BundleGeneration) bundleController.getCurrentGeneration(), options);
                }
                catch (BundleException be)
                {
                    bundleManager.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundleController, be));
                }
            }
        }
    }

    public int getStartLevel()
    {
        return startLevel;
    }

    public void setStartLevel(int desiredStartLevel)
    {
        serialExecutor.execute(new SetStartLevel(desiredStartLevel));
    }

    public int getBundleStartLevel(Bundle bundle)
    {
        return store.getBundleStartLevel(bundle);
    }

    public void setBundleStartLevel(Bundle bundle, int startLevel)
    {
        store.setBundleStartLevel(bundle, startLevel);
    }

    public int getInitialBundleStartLevel()
    {
        return initialBundleStartLevel;
    }

    public void setInitialBundleStartLevel(int initialBundleStartLevel)
    {
        this.initialBundleStartLevel = initialBundleStartLevel;
    }

    public boolean isBundlePersistentlyStarted(Bundle bundle)
    {
        return ((BundleController) bundle).getAutostartSetting() != AutostartSetting.STOPPED;
    }

    public boolean isBundleActivationPolicyUsed(Bundle bundle)
    {
        return ((BundleController) bundle).getAutostartSetting() == AutostartSetting.ACTIVATION_POLICY;
    }

    public void bundleChanged(BundleEvent bundleEvent)
    {
        if (bundleEvent.getType() == BundleEvent.INSTALLED)
        {
            BundleController bundle = (BundleController) bundleEvent.getBundle();

            if (isBundlePersistentlyStarted(bundle))
            {
                started.add(new StartedState(bundle));
                try
                {
                    start((BundleGeneration) bundle.getCurrentGeneration(), Bundle.START_TRANSIENT);
                }
                catch (BundleException e)
                {
                    e.printStackTrace();  //Todo change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        else if (bundleEvent.getType() == BundleEvent.UNINSTALLED)
        {
            BundleController bundle = (BundleController) bundleEvent.getBundle();

            store.clearBundleStartLevel(bundle);
            started.remove(new StartedState(bundle));
            try
            {
                stop((BundleGeneration) bundle.getCurrentGeneration(), Bundle.STOP_TRANSIENT);
            }
            catch (BundleException e)
            {
                e.printStackTrace();  //Todo change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    private static class StartedState
    {
        private final BundleController bundleController;
        private volatile int options;

        private StartedState(BundleController bundleController)
        {
            this(bundleController, Bundle.START_TRANSIENT);
        }

        private StartedState(BundleController bundleController, int options)
        {
            assert bundleController != null;

            this.bundleController = bundleController;
            this.options = options;
        }

        public BundleController getBundleController()
        {
            return bundleController;
        }

        public int getOptions()
        {
            return options;
        }

        public void clearOptions()
        {
            options = Bundle.START_TRANSIENT;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StartedState that = (StartedState) o;

            return bundleController.equals(that.bundleController);
        }

        @Override
        public int hashCode()
        {
            return bundleController.hashCode();
        }
    }

    private class SetStartLevel implements Runnable
    {
        final int desiredStartLevel;

        private SetStartLevel(int desiredStartLevel)
        {
            assert desiredStartLevel >= 0;

            this.desiredStartLevel = desiredStartLevel;
        }

        public void run()
        {
            BundleManager bundleManager = framework.getBundleManager();

            synchronized (lock)
            {
                if (startLevel < desiredStartLevel)
                {
                    while (startLevel < desiredStartLevel)
                    {
                        startLevel++;

                        for (StartedState startedState : started)
                        {
                            BundleController bundleController = startedState.getBundleController();

                            if (startLevel == StartLevelImpl.this.getBundleStartLevel(bundleController))
                            {
                                try
                                {
                                    bundleManager.beginStart((BundleGeneration) bundleController.getCurrentGeneration(), startedState.getOptions());
                                    startedState.clearOptions();
                                }
                                catch (BundleException be)
                                {
                                    bundleManager.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundleController, be));
                                }
                            }
                        }
                    }
                }
                else if (startLevel > desiredStartLevel)
                {
                    while (startLevel > desiredStartLevel)
                    {
                        for (StartedState startedState : started)
                        {
                            BundleController bundleController = startedState.getBundleController();

                            if (startLevel == StartLevelImpl.this.getBundleStartLevel(bundleController))
                            {
                                try
                                {
                                    bundleManager.beginStop((BundleGeneration) bundleController.getCurrentGeneration(), Bundle.STOP_TRANSIENT);
                                }
                                catch (Exception e)
                                {
                                    bundleManager.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundleController, e));
                                }
                            }
                        }

                        startLevel--;
                    }
                }

                bundleManager.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, bundleManager.getBundle(0), null));
            }
        }
    }
}
