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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import net.jcip.annotations.ThreadSafe;
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
@ThreadSafe
public class StartLevelImpl implements StartManager, StartLevel, SynchronousBundleListener
{
    private final static String CLASS_NAME = StartLevelImpl.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Set<StartedState> started = new HashSet<StartedState>();
    private volatile Papoose framework;
    private volatile SerialExecutor serialExecutor;
    private volatile StartLevelStore store;
    private volatile ServiceRegistration serviceRegistration;
    private volatile int startLevel = 1;
    private volatile int initialBundleStartLevel = 1;

    public void start(Papoose framework)
    {
        LOGGER.entering(CLASS_NAME, "start", framework);

        this.framework = framework;

        framework.getBundleManager().setStartManager(this);

        this.serialExecutor = new SerialExecutor(framework.getExecutorService());

        BundleContext context = framework.getSystemBundleContext();

        ServiceReference reference = context.getServiceReference(StartLevelStore.class.getName());
        if (reference != null)
        {
            this.store = (StartLevelStore) context.getService(reference);
        }
        else
        {
            this.store = new DefaltStartLevelStore();
        }

        store.start(framework);

        context.addBundleListener(this);

        this.serviceRegistration = context.registerService(StartLevelImpl.class.getName(), this, null);

        LOGGER.exiting(CLASS_NAME, "start");
    }

    public void stop()
    {
        LOGGER.entering(CLASS_NAME, "stop");

        serviceRegistration.unregister();

        BundleContext context = framework.getSystemBundleContext();

        context.removeBundleListener(this);

        store.stop();

        serviceRegistration = null;
        store = null;
        framework = null;

        started.clear();

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    public void start(BundleGeneration bundle, int options) throws BundleException
    {
        FutureTask<Void> task = new FutureTask<Void>(new BundleStart(bundle.getBundleController(), options));

        serialExecutor.execute(task);


        try
        {
            task.get();
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Interrupted while waiting for bundle to start", ie);
        }
        catch (ExecutionException ee)
        {
            if (ee.getCause() != null)
            {
                throw new BundleException("Interrupted while waiting for bundle to start", ee.getCause());
            }
            else
            {
                throw new BundleException("Interrupted while waiting for bundle to start");
            }
        }
    }

    public void stop(BundleGeneration bundle, int options) throws BundleException
    {
        FutureTask<Void> task = new FutureTask<Void>(new BundleStop(bundle.getBundleController(), options));

        serialExecutor.execute(task);

        try
        {
            task.get();
        }
        catch (InterruptedException ie)
        {
            Thread.currentThread().interrupt();
            throw new BundleException("Interrupted while waiting for bundle to start", ie);
        }
        catch (ExecutionException ee)
        {
            if (ee.getCause() != null)
            {
                throw new BundleException("Interrupted while waiting for bundle to start", ee.getCause());
            }
            else
            {
                throw new BundleException("Interrupted while waiting for bundle to start");
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

            store.setBundleStartLevel(bundle, initialBundleStartLevel);
            if (isBundlePersistentlyStarted(bundle))
            {
                started.add(new StartedState(bundle));
                serialExecutor.execute(new FutureTask<Void>(new BundleStart(bundle, Bundle.START_TRANSIENT)));
            }
        }
        else if (bundleEvent.getType() == BundleEvent.UNINSTALLED)
        {
            BundleController bundle = (BundleController) bundleEvent.getBundle();

            store.clearBundleStartLevel(bundle);
            started.remove(new StartedState(bundle));
            serialExecutor.execute(new FutureTask<Void>(new BundleStop(bundle, Bundle.STOP_TRANSIENT)));
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

    private class BundleStart implements Callable<Void>
    {
        private final BundleController bundleController;
        private final int options;

        private BundleStart(BundleController bundleController, int options)
        {
            assert bundleController != null;
            assert options >= 0;

            this.bundleController = bundleController;
            this.options = options;
        }

        public Void call() throws Exception
        {
            if (startLevel >= StartLevelImpl.this.getBundleStartLevel(bundleController))
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

            return null;
        }
    }

    private class BundleStop implements Callable<Void>
    {
        private final BundleController bundleController;
        private final int options;

        private BundleStop(BundleController bundleController, int options)
        {
            assert bundleController != null;
            assert options >= 0;

            this.bundleController = bundleController;
            this.options = options;
        }

        public Void call() throws Exception
        {
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

            return null;
        }
    }
}
