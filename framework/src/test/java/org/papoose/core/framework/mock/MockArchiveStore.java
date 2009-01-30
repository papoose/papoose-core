/**
 *  Copyright 2008-2009 Picateers Inc., 1720 S. Amphlett Boulevard  Suite 320, San Mateo, CA 94402 U.S.A. All rights reserved.
 */
package org.papoose.core.framework.mock;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.jar.Attributes;

import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import org.papoose.core.framework.DynamicDescription;
import org.papoose.core.framework.ExportDescription;
import org.papoose.core.framework.FragmentDescription;
import org.papoose.core.framework.ImportDescription;
import org.papoose.core.framework.L18nResourceBundle;
import org.papoose.core.framework.NativeCodeDescription;
import org.papoose.core.framework.RequireDescription;
import org.papoose.core.framework.spi.ArchiveStore;

/**
 * @version $Revision$ $Date$
 */
public class MockArchiveStore implements ArchiveStore
{
    public String getFrameworkName()
    {
        return null;  //todo: consider this autogenerated code
    }

    public long getBundleId()
    {
        return 0;  //todo: consider this autogenerated code
    }

    public int getGeneration()
    {
        return 0;  //todo: consider this autogenerated code
    }

    public Attributes getAttributes()
    {
        Attributes result = new Attributes();

        result.put(new Attributes.Name("Manifest-Version"), "1.0");
        result.put(new Attributes.Name("Bundle-Activator"), "com.acme.impl.Activator");
        result.put(new Attributes.Name("Created-By"), "1.5.0_13 (Apple Inc.)");
        result.put(new Attributes.Name("Import-Package"), "com.acme.api,org.osgi.framework");
        result.put(new Attributes.Name("L10N-Bundle"), "%bundle");
        result.put(new Attributes.Name("Include-Resource"), "src/main/resources");
        result.put(new Attributes.Name("Bnd-LastModified"), "1208018376942");
        result.put(new Attributes.Name("Export-Package"), "com.acme.api");
        result.put(new Attributes.Name("Bundle-Version"), "1.0.0.SNAPSHOT");
        result.put(new Attributes.Name("Bundle-Name"), "Papoose :: OSGi R4 test bundle");
        result.put(new Attributes.Name("Bundle-Description"), "OSGi R4 Test Bundle");
        result.put(new Attributes.Name("Bundle-Classpath"), ".,lib/test.jar");
        result.put(new Attributes.Name("Private-Package"), "OSGI-INF.l10n,com.acme,com.acme.impl,com.acme.pvt,com.acme.resource,com.acme.safe,lib");
        result.put(new Attributes.Name("L10N-Test"), "%test");
        result.put(new Attributes.Name("Bundle-ManifestVersion"), "2");
        result.put(new Attributes.Name("L10N-NoTranslation"), "%no translation for this entry");
        result.put(new Attributes.Name("Bundle-SymbolicName"), "org.papoose.test.papoose-test-bundle");
        result.put(new Attributes.Name("Tool"), "Bnd-0.0.160");

        return result;
    }

    public String getBundleActivatorClass()
    {
        return "com.acme.impl.Activator";
    }

    public String getBundleSymbolicName()
    {
        return "org.papoose.test.papoose-test-bundle";
    }

    public Version getBundleVersion()
    {
        return new Version(1, 0, 0, "SNAPSHOT");
    }

    public List<String> getBundleClassPath()
    {
        return Collections.emptyList();
    }

    public List<NativeCodeDescription> getBundleNativeCodeList()
    {
        return Collections.emptyList();
    }

    public List<String> getBundleRequiredExecutionEnvironment()
    {
        return Collections.emptyList();
    }

    public List<ExportDescription> getBundleExportList()
    {
        return Collections.emptyList();
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

    public FragmentDescription getBundleFragmentHost()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void refreshClassPath(List<String> classPath) throws BundleException
    {
        int i = 0;
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

    public Enumeration findEntries(String path, String filePattern, boolean includeDirectory, boolean recurse)
    {
        try
        {
            if ("*.xml".equals(filePattern)) return Collections.enumeration(Arrays.asList(new URL("papoose://org.acme.osgi.0:1/com/acme/anvil.xml")));
            if ("*.class".equals(filePattern))
            {
                return Collections.enumeration(Arrays.asList(
                        new URL("papoose://org.acme.osgi.0:1/com/acme/impl/Activator.class"),
                        new URL("papoose://org.acme.osgi.0:1/com/acme/impl/AnvilImpl.class"),
                        new URL("papoose://org.acme.osgi.0:1/com/acme/pvt/Hidden.class"),
                        new URL("papoose://org.acme.osgi.0:1/com/acme/api/AnvilApi.class"),
                        new URL("papoose://org.acme.osgi.0:1/com/acme/safe/Primary.class")
                ));
            }
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();  //todo: consider this autogenerated code
        }
        return null;  //todo: consider this autogenerated code
    }

    @SuppressWarnings({ "EmptyCatchBlock" })
    public L18nResourceBundle getResourceBundle(Locale locale)
    {
        try
        {
            String path = "org/papoose/core/framework/mock/bundle";
            path += (locale != null ? "_" + locale : "") + ".properties";
            InputStream in = MockArchiveStore.class.getClassLoader().getResourceAsStream(path);
            if (in != null) return new L18nResourceBundle(in);
        }
        catch (IOException ioe)
        {
        }
        return null;
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

    public void assignNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close()
    {
        int i = 0;
        //todo: consider this autogenerated code
    }

    public int compareTo(Object o)
    {
        return 0;  //todo: consider this autogenerated code
    }
}
