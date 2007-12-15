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

import java.io.File;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;

/**
 * @version $Revision$ $Date$
 */
public class SystemBundle extends BundleImpl
{
    private final Logger logger = Logger.getLogger(getClass().getName());

    SystemBundle(Papoose framework, long bundleId, String location, BundleStore bundleStore)
    {
        super(framework, bundleId, location, bundleStore, new SystemArchiveStore(framework));
    }

    private static class SystemArchiveStore implements ArchiveStore
    {
        private final Logger logger = Logger.getLogger(getClass().getName());
        private final Papoose framework;
        private final List<ExportDescription> exportDescriptions = new ArrayList<ExportDescription>();

        public SystemArchiveStore(Papoose framework)
        {
            this.framework = framework;

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

        }

        public File getArchive()
        {
            return null;  //todo: consider this autogenerated code
        }

        public String getFrameworkName()
        {
            return null;  //todo: consider this autogenerated code
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
            return null;  //todo: consider this autogenerated code
        }

        public String getBundleActivatorClass()
        {
            return null;  //todo: consider this autogenerated code
        }

        public String getBundleSymbolicName()
        {
            return "org.papoose.system-bundle";
        }

        public Version getBundleVersion()
        {
            return new Version(1, 0, 0);
        }

        public List<String> getBundleClassPath()
        {
            return null;  //todo: consider this autogenerated code
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
            //todo: consider this autogenerated code
        }

        public String loadLibrary(String libname)
        {
            return null;  //todo: consider this autogenerated code
        }

        public Permission[] getPermissionCollection()
        {
            return new Permission[0];  //todo: consider this autogenerated code
        }

        public ResourceHandle getEntry(String name)
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
