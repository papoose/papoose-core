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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.Version;
import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.StartManager;
import org.papoose.core.framework.util.AttributeUtils;
import org.papoose.core.framework.util.AttributesWrapper;


/**
 * @version $Revision$ $Date$
 */
public class SystemBundleImpl extends BundleImpl
{
    private final static String CLASS_NAME = SystemBundleImpl.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    SystemBundleImpl(Papoose framework, long bundleId, BundleStore bundleStore, Version version) throws BundleException
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
            LOGGER.log(Level.SEVERE, "Unable to add service manager listener", ise);
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
        Papoose framework = getFramework();
        BundleManager manager = framework.getBundleManager();

        setStartingState();

        manager.loadAndStartBundles();

        setActiveState();

        manager.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.STARTED, this, null));
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

        public SystemArchiveStore(Papoose framework, Version version) throws BundleException
        {
            assert framework != null;
            assert version != null;

            this.framework = framework;
            this.version = version;

            String packages = (String) framework.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
            if (packages != null)
            {
                exportDescriptions.addAll(AttributeUtils.parseBundleExportList(packages, getBundleSymbolicName(), getBundleVersion()));
            }
            exportDescriptions.addAll(AttributeUtils.parseBundleExportList("org.osgi.framework", getBundleSymbolicName(), getBundleVersion()));

            Properties p = framework.getProperties();
            Attributes a = new Attributes();
            a.putValue(Constants.BUNDLE_CLASSPATH, ".");
            a.putValue(Constants.BUNDLE_CONTACTADDRESS, p.getProperty(PapooseConstants.PAPOOSE_CONTACT_ADDRESS));
            a.putValue(Constants.BUNDLE_COPYRIGHT, p.getProperty(PapooseConstants.PAPOOSE_COPYRIGHT));
            a.putValue(Constants.BUNDLE_DESCRIPTION, p.getProperty(PapooseConstants.PAPOOSE_DESCRIPTION));
            a.putValue(Constants.BUNDLE_DOCURL, p.getProperty(PapooseConstants.PAPOOSE_DOC_URL));
            a.putValue(Constants.BUNDLE_MANIFESTVERSION, Integer.toString(2));
            a.putValue(Constants.BUNDLE_NAME, "Papoose OSGi R4 System Bundle");
            a.putValue(Constants.BUNDLE_SYMBOLICNAME, getBundleSymbolicName());
            a.putValue(Constants.BUNDLE_VENDOR, p.getProperty(PapooseConstants.PAPOOSE_VENDOR));
            a.putValue(Constants.BUNDLE_VERSION, version.toString());

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
            return Collections.singletonList(".");
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
            if (!(o instanceof SystemArchiveStore)) return 1;
            return 0;
        }
    }
}
