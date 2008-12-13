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
package org.papoose.store.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.apache.xbean.classloader.AbstractResourceHandle;
import org.apache.xbean.classloader.AbstractUrlResourceLocation;
import org.apache.xbean.classloader.ResourceHandle;
import org.apache.xbean.classloader.ResourceLocation;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.papoose.core.framework.AbstractArchiveStore;
import org.papoose.core.framework.L18nResourceBundle;
import org.papoose.core.framework.NativeCodeDescription;
import org.papoose.core.framework.Papoose;
import org.papoose.core.framework.UrlUtils;
import org.papoose.core.framework.Util;
import org.papoose.core.framework.util.FileUtils;


/**
 * @version $Revision$ $Date$
 */
public class NonCachingArchiveStore extends AbstractArchiveStore
{
    private final static String ARCHIVE_JAR_NAME = "archive.jar";
    private final static String ARCHIVE_NAME = "archive";
    private final static String TEMP_NAME = "tmp";
    private final ResourceLocation fileLocation = new BundleDirectoryResourceLocation("", -1);
    private final List<ResourceLocation> resourceLocations = new ArrayList<ResourceLocation>();
    private final File bundleRoot;
    private final File tmp;
    private final JarFile archive;
    private final URL codeSource;
    private SortedSet<NativeCodeDescription> nativeCodeDescriptions;

    public NonCachingArchiveStore(Papoose framework, long bundleId, int generaton, File archiveRoot) throws BundleException
    {
        this(framework, bundleId, generaton, archiveRoot, Util.safeStream(new File(archiveRoot, ARCHIVE_JAR_NAME)));
    }

    public NonCachingArchiveStore(Papoose framework, long bundleId, int generaton, File archiveRoot, InputStream inputStream) throws BundleException
    {
        super(framework, bundleId, generaton, loadAndProvideAttributes(archiveRoot, inputStream));
        this.bundleRoot = archiveRoot;
        try
        {
            this.archive = new JarFile(new File(archiveRoot, ARCHIVE_JAR_NAME));
            this.codeSource = UrlUtils.generateCodeSourceUrl(getFrameworkName(), getBundleId());

            this.tmp = new File(archiveRoot, TEMP_NAME);

            if (!tmp.exists() && !tmp.mkdirs()) throw new BundleException("Unable to create temp directory: " + tmp);
        }
        catch (IOException ioe)
        {
            throw new BundleException("Unable to create jar file", ioe);
        }
    }

    public void setNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException
    {
        this.nativeCodeDescriptions = nativeCodeDescriptions;

        File archiveDir = new File(bundleRoot, ARCHIVE_NAME);
        for (NativeCodeDescription description : nativeCodeDescriptions)
        {
            for (String path : description.getPaths())
            {
                if (!new File(archiveDir, path).exists()) throw new BundleException("Native code library " + path + " cannot be found.");
            }
        }
    }

    public void refreshClassPath(List<String> classPath) throws BundleException
    {
        Util.delete(tmp);
        resourceLocations.clear();

        if (!tmp.exists() && !tmp.mkdirs()) throw new BundleException("Unable to create temp directory: " + tmp);

        for (String path : classPath)
        {
            if (".".equals(path.trim()))
            {
                resourceLocations.add(new BundleDirectoryResourceLocation("", resourceLocations.size()));
            }
            else
            {
                JarEntry entry = archive.getJarEntry(path);
                if (entry == null) entry = archive.getJarEntry(path + "/");
                if (entry != null)
                {
                    if (entry.isDirectory())
                    {
                        resourceLocations.add(new BundleDirectoryResourceLocation(path, resourceLocations.size()));
                    }
                    else
                    {
                        resourceLocations.add(new BundleJarResourceLocation(entry, resourceLocations.size()));
                    }
                }
            }
        }
    }

    public String loadLibrary(String libname)
    {
        String fullNativePath = System.mapLibraryName(libname);

        for (NativeCodeDescription description : nativeCodeDescriptions)
        {
            for (String path : description.getPaths())
            {
                if (fullNativePath.endsWith(path)) return path;
            }
        }
        return null;
    }

    public ResourceHandle getEntry(String name)
    {
        return fileLocation.getResourceHandle(name);
    }

    public Enumeration getEntryPaths(String path)
    {
        if (path.startsWith("/")) path = path.substring(1);
        if (!path.endsWith("/") && path.length() > 1) path += "/";

        Set<String> result = new HashSet<String>();
        Enumeration entries = archive.entries();
        while (entries.hasMoreElements())
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(path))
            {
                String s = name.substring(path.length());
                int count = 0;
                for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '/') count++;
                if (entry.isDirectory())
                {
                    if (count == 1) result.add(name);
                }
                else if (count == 0) result.add(name);
            }
        }

        return result.isEmpty() ? null : Collections.enumeration(result);
    }

    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        if (path.startsWith("/")) path = path.substring(1);
        if (!path.endsWith("/") && path.length() > 1) path += "/";

        Object targets;
        try
        {
            targets = parseValue(filePattern);
            if (targets == null) return null;
        }
        catch (InvalidSyntaxException ise)
        {
            return null;
        }

        Set<URL> result = new HashSet<URL>();
        Enumeration entries = archive.entries();
        while (entries.hasMoreElements())
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (name.startsWith(path))
            {
                String s = name.substring(path.length());
                int count = 0;
                for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '/') count++;
                if (!entry.isDirectory())
                {
                    if (count == 0 && Util.match(targets, s))
                    {
                        result.add(fileLocation.getResourceHandle(name).getUrl());
                    }
                    else if (recurse && Util.match(targets, s.substring(s.lastIndexOf('/') + 1)))
                    {
                        result.add(fileLocation.getResourceHandle(name).getUrl());
                    }
                }
            }
        }

        return result.isEmpty() ? null : Collections.enumeration(result);
    }

    public ResourceHandle getResource(String resourceName)
    {
        ResourceHandle handle;

        for (ResourceLocation location : resourceLocations)
        {
            if ((handle = location.getResourceHandle(resourceName)) != null) return handle;
        }

        return null;
    }


    public ResourceHandle getResource(String resourceName, int location)
    {
        return resourceLocations.get(location).getResourceHandle(resourceName);
    }

    public List<ResourceHandle> findResources(String resourceName)
    {
        List<ResourceHandle> result = new ArrayList<ResourceHandle>();
        ResourceHandle handle;

        for (ResourceLocation location : resourceLocations)
        {
            if ((handle = location.getResourceHandle(resourceName)) != null) result.add(handle);
        }

        return result;
    }

    @SuppressWarnings({"EmptyCatchBlock"})
    public L18nResourceBundle getResourceBundle(Locale locale)
    {
        try
        {
            String path = this.getBundleLocalization();
            if (path == null) path = "OSGI-INF/l10n/bundle";
            path += (locale != null ? "_" + locale : "") + ".properties";
            JarEntry entry = archive.getJarEntry(path);
            if (entry != null) return new L18nResourceBundle(archive.getInputStream(entry));
        }
        catch (IOException ioe)
        {
        }
        return null;
    }

    @SuppressWarnings({"EmptyCatchBlock"})
    public void close()
    {
        try
        {
            archive.close();
        }
        catch (IOException ioe)
        {
        }
    }

    public String toString()
    {
        return getFrameworkName() + " " + getBundleId() + " " + archive.getName();
    }

    private class BundleDirectoryResourceLocation extends AbstractUrlResourceLocation
    {
        private final String path;
        private final int location;

        public BundleDirectoryResourceLocation(String path, int location)
        {
            super(codeSource);
            this.path = path;
            this.location = location;
        }

        public ResourceHandle getResourceHandle(String resourceName)
        {
            ZipEntry entry = archive.getEntry(path + resourceName);
            if (entry != null)
            {
                NonCachingArchiveStore archiveStore = NonCachingArchiveStore.this;
                return new BundleDirectoryResourceHandle(entry, UrlUtils.generateResourceUrl(archiveStore.getFrameworkName(), archiveStore.getBundleId(), path + "/" + entry.getName(), location));
            }
            return null;
        }

        public Manifest getManifest() throws IOException
        {
            return archive.getManifest();
        }

        private class BundleDirectoryResourceHandle extends AbstractResourceHandle
        {
            private final ZipEntry entry;
            private final URL url;

            public BundleDirectoryResourceHandle(ZipEntry entry, URL url)
            {
                this.entry = entry;
                this.url = url;
            }

            public String getName() { return entry.getName(); }

            public URL getUrl() { return url; }

            public boolean isDirectory() { return entry.isDirectory(); }

            public URL getCodeSourceUrl() { return codeSource; }

            public InputStream getInputStream() throws IOException { return archive.getInputStream(entry); }

            public int getContentLength() { return (int) entry.getSize(); }
        }
    }

    private class BundleJarResourceLocation extends AbstractUrlResourceLocation
    {
        private final JarFile jarFile;
        private final String path;
        private final int location;

        public BundleJarResourceLocation(JarEntry entry, int location) throws BundleException
        {
            super(codeSource);

            this.path = entry.getName();
            this.location = location;

            try
            {
                FileUtils.buildDirectoriesFromFilePath(tmp, entry.getName(), '/');
                File jarLocation = new File(tmp, Util.strip(entry.getName()));

                Util.copy(archive.getInputStream(entry), new FileOutputStream(jarLocation));

                this.jarFile = new JarFile(jarLocation);
            }
            catch (IOException ioe)
            {
                throw new BundleException("Unable to save nested jar: " + entry.getName(), ioe);
            }
        }

        public ResourceHandle getResourceHandle(String resourceName)
        {
            JarEntry entry = jarFile.getJarEntry(resourceName);
            if (entry != null)
            {
                return new BundleJarResourceHandle(entry, UrlUtils.generateResourceUrl(getFrameworkName(), getBundleId(), "/" + path + "!/" + resourceName, location));
            }
            return null;
        }

        public Manifest getManifest() throws IOException
        {
            return jarFile.getManifest();
        }

        private class BundleJarResourceHandle extends AbstractResourceHandle
        {
            private final JarEntry entry;
            private final URL url;

            public BundleJarResourceHandle(JarEntry entry, URL url)
            {
                this.entry = entry;
                this.url = url;
            }

            public String getName() { return entry.getName(); }

            public URL getUrl() { return url; }

            public boolean isDirectory() { return entry.isDirectory(); }

            public URL getCodeSourceUrl() { return codeSource; }

            public InputStream getInputStream() throws IOException { return jarFile.getInputStream(entry); }

            public int getContentLength() { return (int) entry.getSize(); }
        }
    }

    private static Attributes loadAndProvideAttributes(File bundleRoot, InputStream inputStream) throws BundleException
    {
        try
        {
            File archiveFile = new File(bundleRoot, ARCHIVE_JAR_NAME);
            OutputStream outputStream = new FileOutputStream(archiveFile);

            Util.copy(inputStream, outputStream);

            outputStream.close();

            File archiveDir = new File(bundleRoot, ARCHIVE_NAME);
            archiveDir.mkdirs();

            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(archiveFile));

            return jarInputStream.getManifest().getMainAttributes();
        }
        catch (IOException ioe)
        {
            throw new BundleException("Problems with the bundle archive", ioe);
        }
    }

}
