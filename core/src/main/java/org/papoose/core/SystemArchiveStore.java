/**
 *
 * Copyright 2009 (C) The original author or authors
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
import java.net.URL;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.logging.Logger;

import org.apache.xbean.classloader.ResourceLocation;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.papoose.core.descriptions.DynamicDescription;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.FragmentDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.descriptions.LazyActivationDescription;
import org.papoose.core.descriptions.NativeCodeDescription;
import org.papoose.core.descriptions.RequireDescription;
import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.util.AttributeUtils;
import org.papoose.core.util.AttributesWrapper;

/**
 *
 */
class SystemArchiveStore implements ArchiveStore
{
    private final Logger LOGGER = Logger.getLogger(SystemArchiveStore.class.getName());
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
        exportDescriptions.addAll(AttributeUtils.parseBundleExportList("org.osgi.framework;version=1.5,org.osgi.service.url;version=1.5,org.osgi.util.tracker;version=1.5", getBundleSymbolicName(), getBundleVersion()));

        if (framework.getProperty(PapooseConstants.PAPOOSE_SERVICE_PACKAGE_ADMIN) != null)
        {
            exportDescriptions.addAll(AttributeUtils.parseBundleExportList("org.osgi.service.packageadmin;version=1.2", getBundleSymbolicName(), getBundleVersion()));
        }
        if (framework.getProperty(PapooseConstants.PAPOOSE_SERVICE_START_LEVEL) != null)
        {
            exportDescriptions.addAll(AttributeUtils.parseBundleExportList("org.osgi.service.startlevel;version=1.1", getBundleSymbolicName(), getBundleVersion()));
        }
        if (framework.getProperty(PapooseConstants.PAPOOSE_SERVICE_URL_HANDLERS) != null)
        {
            exportDescriptions.addAll(AttributeUtils.parseBundleExportList("org.osgi.service.url;version=1.0", getBundleSymbolicName(), getBundleVersion()));
        }
        if (framework.getProperty(PapooseConstants.PAPOOSE_SERVICE_PACKAGE_ADMIN) != null)
        {
            exportDescriptions.addAll(AttributeUtils.parseBundleExportList("org.osgi.service.permissionadmin;version=1.2", getBundleSymbolicName(), getBundleVersion()));
        }
        if (framework.getProperty(PapooseConstants.PAPOOSE_SERVICE_CND_PERM_ADMIN) != null)
        {
            exportDescriptions.addAll(AttributeUtils.parseBundleExportList("org.osgi.service.condpermadmin;version=1.0", getBundleSymbolicName(), getBundleVersion()));
        }

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

    public URL getBundleUpdateLocation()
    {
        return null;
    }

    public Version getBundleVersion()
    {
        return version;
    }

    public List<String> getBundleClassPath()
    {
        return Collections.singletonList(".");
    }

    public List<NativeCodeDescription> getBundleNativeCodeList()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<String> getBundleRequiredExecutionEnvironment()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<ExportDescription> getExportDescriptions()
    {
        return exportDescriptions;
    }

    public List<ImportDescription> getImportDescriptions()
    {
        return Collections.emptyList();
    }

    public List<RequireDescription> getRequireDescriptions()
    {
        return Collections.emptyList();
    }

    public List<DynamicDescription> getDynamicDescriptions()
    {
        return Collections.emptyList();
    }

    public FragmentDescription getFragmentDescription()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ResourceLocation registerClassPathElement(String classPathElement) throws BundleException
    {
        return null; // todo:
    }

    public LazyActivationDescription getLazyActivationDescription()
    {
        return null;  //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isLazyActivationPolicy()
    {
        return false;  //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public String loadLibrary(String libname)
    {
        return null;
    }

    public Enumeration findEntries(String path, String filePattern, boolean includeDirectory, boolean recurse)
    {
        return null;  //todo: consider this autogenerated code
    }

    public L18nResourceBundle getResourceBundle(Locale local)
    {
        return null;  //todo: consider this autogenerated code
    }

    public InputStream getInputStreamForCodeSource() throws IOException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getInputStreamForEntry(String path) throws IOException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getInputStreamForResource(int location, String path) throws IOException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Certificate[] getCertificates()
    {
        return new Certificate[0];  //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSingleton()
    {
        return false;  //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public void assignNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException
    {
        //To change body of implemented methods use File | Settings | File Templates.
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
