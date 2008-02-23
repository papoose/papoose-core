/**
 *
 * Copyright 2007 (C) The original author or authors
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
package org.papoose.core.framework;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleManager;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.StartManager;
import org.papoose.core.framework.spi.Store;


/**
 * @version $Revision$ $Date$
 */
public class BundleManagerImpl implements BundleManager
{
    private final String className = getClass().getName();
    private final Logger logger = Logger.getLogger(className);
    private final Map<String, AbstractBundle> locations = new HashMap<String, AbstractBundle>();
    private final Map<Long, AbstractBundle> installedbundles = new HashMap<Long, AbstractBundle>();
    private final Map<Long, BundleImpl> bundles = new HashMap<Long, BundleImpl>();
    private final Papoose framework;
    private final Store store;
    private StartManager startManager;
    private long bundleCounter = 1;


    public BundleManagerImpl(final Papoose framework, Store store)
    {
        this.framework = framework;
        this.store = store;
        this.startManager = new DefaultStartManager(this);
    }

    public void setStartManager(StartManager startManager)
    {
        this.startManager = startManager;
    }

    public InputStream getInputStream(int bundleId, int generation) throws IOException
    {
        return null;  //todo: consider this autogenerated code
    }

    public void recordBundleHasStarted(Bundle bundle)
    {
        //todo: consider this autogenerated code
    }

    public Bundle getBundle(long bundleId)
    {
        return installedbundles.get(bundleId);
    }

    public Bundle getBundle(String symbolicName)
    {
        return null;  //todo: consider this autogenerated code
    }

    public Bundle[] getBundles()
    {
        return new Bundle[0];  //todo: consider this autogenerated code
    }

    public Bundle installSystemBundle(Version version) throws BundleException
    {
        logger.entering(className, "installSystemBundle");

        if (locations.containsKey(Constants.SYSTEM_BUNDLE_LOCATION)) return locations.get(Constants.SYSTEM_BUNDLE_LOCATION);

        final long bundleId = 0;
        try
        {
            BundleStore bundleStore = store.allocateBundleStore(bundleId, Constants.SYSTEM_BUNDLE_LOCATION);

            AbstractBundle bundle = new SystemBundleImpl(framework, bundleId, bundleStore, version);

            locations.put(Constants.SYSTEM_BUNDLE_LOCATION, bundle);
            installedbundles.put(bundleId, bundle);
            bundles.put(bundleId, (BundleImpl) bundle);

            logger.exiting(className, "installSystemBundle", bundle);

            return bundle;
        }
        catch (BundleException be)
        {
            store.removeBundleStore(bundleId);
            logger.log(Level.SEVERE, "Unable to install system bundle " + Constants.SYSTEM_BUNDLE_LOCATION, be);
            throw be;
        }
        catch (Exception e)
        {
            store.removeBundleStore(bundleId);
            logger.log(Level.SEVERE, "Unable to install system bundle " + Constants.SYSTEM_BUNDLE_LOCATION, e);
            throw new BundleException("Error occured while loading location " + Constants.SYSTEM_BUNDLE_LOCATION, e);
        }
    }

    public Bundle installBundle(String location, InputStream inputStream) throws BundleException
    {
        logger.entering(getClass().getName(), "installBundle", new Object[]{ location, inputStream });

        if (locations.containsKey(location)) return locations.get(location);

        long bundleId = bundleCounter++;
        try
        {
            BundleStore bundleStore = store.allocateBundleStore(bundleId, location);

            AbstractStore archiveStore = store.allocateArchiveStore(framework, bundleId, 0, inputStream);

            AbstractBundle bundle = allocateBundle(bundleId, location, bundleStore, archiveStore);

            bundle.markInstalled();

            locations.put(location, bundle);
            installedbundles.put(bundleId, bundle);
            if (bundle instanceof BundleImpl) bundles.put(bundleId, (BundleImpl) bundle);

            return bundle;
        }
        catch (BundleException be)
        {
            store.removeBundleStore(bundleId);
            logger.log(Level.WARNING, "Unable to install bundle " + location, be);
            throw be;
        }
        catch (Exception e)
        {
            store.removeBundleStore(bundleId);
            logger.log(Level.WARNING, "Unable to install bundle " + location, e);
            throw new BundleException("Error occured while loading location " + location, e);
        }
    }

    public void resolve(Bundle bundle)
    {
        try
        {
            BundleImpl bundleImpl = (BundleImpl) bundle;
            ArchiveStore currentStore = bundleImpl.getCurrentStore();
            Set<Wire> wires = framework.getResolver().resolve(currentStore.getBundleImportList(), new HashSet<BundleImpl>(bundles.values()));
            List<Wire> requiredBundles = new ArrayList<Wire>();

            String bootDelegateString = (String) framework.getProperty(Constants.FRAMEWORK_BOOTDELEGATION);
            String[] bootDelegates = (bootDelegateString == null ? new String[]{ } : bootDelegateString.split(","));

            Set<String> exportedPackages = new HashSet<String>();

            for (ImportDescription desc : currentStore.getBundleImportList())
            {
                exportedPackages.addAll(desc.getPackageNames());
            }

            for (ExportDescription desc : currentStore.getBundleExportList())
            {
                exportedPackages.addAll(desc.getPackages());
            }

            for (Wire wire : requiredBundles)
            {
                exportedPackages.add(wire.getPackageName());
            }

            BundleClassLoader classLoader = new BundleClassLoader(bundle.getLocation(),
                                                                  framework.getClassLoader(),
                                                                  framework,
                                                                  bundleImpl,
                                                                  requiredBundles,
                                                                  bootDelegates,
                                                                  exportedPackages.toArray(new String[exportedPackages.size()]),
                                                                  currentStore.getDynamicImportSet(),
                                                                  bundleImpl.getStores());

            classLoader.setWires(wires);  // todo: why this separate call?

            bundleImpl.setClassLoader(classLoader);

            bundleImpl.setResolvedState();
        }
        catch (BundleException e)
        {
            e.printStackTrace();  //todo: consider this autogenerated code
        }
    }

    public void requestStart(Bundle bundle)
    {
        startManager.start(bundle);
    }

    public void performStart(Bundle bundle)
    {
        //todo: consider this autogenerated code
    }

    public void stop(Bundle bundle)
    {
        //todo: consider this autogenerated code
    }

    public void uninstall(Bundle bundle)
    {
        //todo: consider this autogenerated code
    }

    public void fireBundleEvent(final BundleEvent event)
    {
        Collection<AbstractBundle> bundles = installedbundles.values();

        for (final Bundle bundle : bundles)
        {
            if (bundle instanceof BundleImpl)
            {
                for (final BundleListener listener : ((BundleImpl) bundle).syncBundleListeners)
                {
                    try
                    {
                        if (System.getSecurityManager() == null)
                        {
                            if (bundle.getState() == Bundle.ACTIVE) listener.bundleChanged(event);
                        }
                        else
                        {
                            AccessController.doPrivileged(new PrivilegedAction<Void>()
                            {
                                public Void run()
                                {
                                    if (bundle.getState() == Bundle.ACTIVE) listener.bundleChanged(event);
                                    return null;
                                }
                            });
                        }
                    }
                    catch (Throwable throwable)
                    {
                        fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, throwable));
                    }
                }
            }
        }

        if ((event.getType() & (BundleEvent.LAZY_ACTIVATION | BundleEvent.STARTING | BundleEvent.STOPPING)) == 0)
        {
            for (Bundle b : bundles)
            {
                if (b instanceof BundleImpl)
                {
                    final BundleImpl bundle = (BundleImpl) b;

                    for (final BundleListener listener : bundle.bundleListeners)
                    {
                        bundle.getSerialExecutor().execute(new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    if (System.getSecurityManager() == null)
                                    {
                                        if (bundle.getState() == Bundle.ACTIVE) listener.bundleChanged(event);
                                    }
                                    else
                                    {
                                        AccessController.doPrivileged(new PrivilegedAction<Void>()
                                        {
                                            public Void run()
                                            {
                                                if (bundle.getState() == Bundle.ACTIVE) listener.bundleChanged(event);
                                                return null;
                                            }
                                        });
                                    }
                                }
                                catch (Throwable throwable)
                                {
                                    fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, throwable));
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    public void fireFrameworkEvent(final FrameworkEvent event)
    {
        for (Bundle b : installedbundles.values())
        {
            if (b instanceof BundleImpl)
            {
                final BundleImpl bundle = (BundleImpl) b;

                for (final FrameworkListener listener : bundle.frameworkListeners)
                {
                    bundle.getSerialExecutor().execute(new Runnable()
                    {
                        public void run()
                        {
                            try
                            {
                                if (System.getSecurityManager() == null)
                                {
                                    if (bundle.getState() == Bundle.ACTIVE) listener.frameworkEvent(event);
                                }
                                else
                                {
                                    AccessController.doPrivileged(new PrivilegedAction<Void>()
                                    {
                                        public Void run()
                                        {
                                            if (bundle.getState() == Bundle.ACTIVE) listener.frameworkEvent(event);
                                            return null;
                                        }
                                    });
                                }
                            }
                            catch (Throwable throwable)
                            {
                                if (event.getType() != FrameworkEvent.ERROR)
                                {
                                    fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, throwable));
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    public void fireServiceEvent(final ServiceEvent event)
    {
        ServiceReference reference = event.getServiceReference();
        String[] classes = (String[]) reference.getProperty(Constants.OBJECTCLASS);

        for (Bundle b : installedbundles.values())
        {
            if (b instanceof BundleImpl)
            {
                BundleImpl bundle = (BundleImpl) b;

                for (String clazz : classes)
                {
                    if (!b.hasPermission(new ServicePermission(clazz, ServicePermission.GET))) continue;

                    fireServiceEvent(event, bundle.allServiceListeners, b);

                    if (!reference.isAssignableTo(b, clazz)) continue;

                    fireServiceEvent(event, bundle.serviceListeners, b);
                }
            }
        }
    }

    protected void fireServiceEvent(final ServiceEvent event, Set<ServiceListener> listeners, final Bundle bundle)
    {
        for (final ServiceListener listener : listeners)
        {
            try
            {
                if (System.getSecurityManager() == null)
                {
                    if (bundle.getState() == Bundle.ACTIVE) listener.serviceChanged(event);
                }
                else
                {
                    AccessController.doPrivileged(new PrivilegedAction<Void>()
                    {
                        public Void run()
                        {
                            if (bundle.getState() == Bundle.ACTIVE) listener.serviceChanged(event);
                            return null;
                        }
                    });
                }
            }
            catch (Throwable throwable)
            {
                fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundle, throwable));
            }
        }
    }

    private AbstractBundle allocateBundle(long bundleId, String location, BundleStore bundleStore, AbstractStore archiveStore)
    {
        return new BundleImpl(framework, bundleId, location, bundleStore, archiveStore);
    }
}
