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

import java.io.InputStream;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.Version;

import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleManager;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.StartManager;
import org.papoose.core.framework.util.AttributesWrapper;

/**
 * @version $Revision$ $Date$
 */
public class SystemBundleImpl extends BundleImpl
{
    private final static String className = SystemBundleImpl.class.getName();
    private final static Logger logger = Logger.getLogger(className);

    SystemBundleImpl(Papoose framework, long bundleId, BundleStore bundleStore, Version version)
    {
        super(framework, bundleId, Constants.SYSTEM_BUNDLE_LOCATION, bundleStore, new SystemArchiveStore(framework, version));

        try
        {
            addServiceListener(new ServiceListener()
            {
                public void serviceChanged(ServiceEvent event)
                {
                    BundleManager bundleManager = getFramework().getBundleManager();

                    if (event.getType() == ServiceEvent.REGISTERED)
                    {
                        StartManager service = (StartManager) getBundleContext().getService(event.getServiceReference());
                        bundleManager.setStartManager(service);
                    }
                    else
                    {
                        bundleManager.setStartManager(new DefaultStartManager(bundleManager));
                    }
                }
            },
                               new FilterImpl(getFramework().getParser().parse("(objectclass=org.papoose.framework.spi.StartManager)")));
        }
        catch (InvalidSyntaxException ise)
        {
            logger.log(Level.SEVERE, "Unable to add service manager listener", ise);
        }
    }

    /**
     * Does nothing because the system bundle is already started.
     *
     * @throws BundleException if there was an error
     */
    public void start() throws BundleException
    {
    }

    /**
     * Does nothing because the system bundle is already started.
     *
     * @param options options usually used to control startup behavior
     * @throws BundleException if there was an error
     */
    public void start(int options) throws BundleException
    {
    }

    public void stop(final int options) throws BundleException
    {
        getFramework().getExecutorService().submit(new Runnable()
        {
            public void run()
            {
                performStop(options);
            }
        });
    }

    public void uninstall() throws BundleException
    {
        throw new BundleException("System bundle cannot be uninstalled");
    }

    public void update(InputStream inputStream) throws BundleException
    {
        update();
    }

    public void update() throws BundleException
    {
        getFramework().getExecutorService().submit(new Runnable()
        {
            public void run()
            {
                performStop(Bundle.STOP_TRANSIENT);
                performStart(Bundle.START_TRANSIENT);
            }
        });
    }

    void performStart(int options)
    {
    }

    void performStop(int options)
    {
    }

    private static class SystemArchiveStore implements ArchiveStore
    {
        private final Logger logger = Logger.getLogger(getClass().getName());
        private final List<ExportDescription> exportDescriptions = new ArrayList<ExportDescription>();
        private final Papoose framework;
        private final Version version;
        private final Attributes attributes;

        public SystemArchiveStore(Papoose framework, Version version)
        {
            assert framework != null;
            assert version != null;

            this.framework = framework;
            this.version = version;

            String packages = (String) framework.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);

            if (packages != null)
            {
                try
                {
                    exportDescriptions.addAll(AbstractStore.parseBundleExportList(packages));
                }
                catch (BundleException be)
                {
                    logger.log(Level.WARNING, "Unable to parse " + Constants.FRAMEWORK_SYSTEMPACKAGES + ":" + packages, be);
                }
            }

            exportDescriptions.add(new ExportDescription(Collections.singletonList("org.osgi.framework"), Collections.<String, Object>singletonMap("version", getBundleVersion())));

            Attributes a = new Attributes();
            attributes = new AttributesWrapper(a);
        }

        public String getFrameworkName()
        {
            return framework.getFrameworkName();
        }

        public long getBundleId()
        {
            return 0;
        }

        public int getGeneration()
        {
            return 0;
        }

        public Attributes getAttributes()
        {
            return attributes;
        }

        public String getBundleActivatorClass()
        {
            return null;
        }

        public String getBundleSymbolicName()
        {
            return "org.papoose.system-bundle." + Papoose.FRAMEWORK_VERSION;
        }

        public Version getBundleVersion()
        {
            return version;
        }

        public List<String> getBundleClassPath()
        {
            List<String> result = new ArrayList<String>(1);
            result.add(".");
            return result;
        }

        public List<ExportDescription> getBundleExportList()
        {
            return exportDescriptions;
        }

        public List<ImportDescription> getBundleImportList()
        {
            return Collections.emptyList();
        }

        public List<RequireDescription> getBundleRequireBundle()
        {
            return Collections.emptyList();
        }

        public Set<DynamicDescription> getDynamicImportSet()
        {
            return Collections.emptySet();
        }

        public void refreshClassPath(List<String> classPath) throws BundleException
        {
        }

        public String loadLibrary(String libname)
        {
            return null;
        }

        public Permission[] getPermissionCollection()
        {
            return new Permission[0];  //todo: consider this autogenerated code
        }

        public ResourceHandle getEntry(String name)
        {
            return null;  //todo: consider this autogenerated code
        }

        public Enumeration getEntryPaths(String path)
        {
            return null;  //todo: consider this autogenerated code
        }

        public Enumeration findEntries(String path, String filePattern, boolean recurse)
        {
            return null;  //todo: consider this autogenerated code
        }

        public ResourceHandle getResource(String resourceName)
        {
            return null;  //todo: consider this autogenerated code
        }

        public ResourceHandle getResource(String resourceName, int location)
        {
            return null;  //todo: consider this autogenerated code
        }

        public List<ResourceHandle> findResources(String resourceName)
        {
            return null;  //todo: consider this autogenerated code
        }

        public L18nResourceBundle getResourceBundle(Locale local)
        {
            return null;  //todo: consider this autogenerated code
        }

        public void close()
        {
            //todo: consider this autogenerated code
        }

        public int compareTo(Object o)
        {
            return 0;  //todo: consider this autogenerated code
        }
    }
}
