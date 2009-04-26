/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

import org.papoose.core.AbstractArchiveStore;
import org.papoose.core.L18nResourceBundle;
import org.papoose.core.Papoose;
import org.papoose.core.UrlUtils;
import org.papoose.core.descriptions.NativeCodeDescription;
import org.papoose.core.util.FileUtils;
import org.papoose.core.util.Util;


/**
 * @version $Revision$ $Date$
 */
public class NonCachingArchiveStore extends AbstractArchiveStore
{
    private final static String ARCHIVE_JAR_NAME = "archive.jar";
    private final static String ARCHIVE_NAME = "archive";
    private final static String TEMP_NAME = "tmp";
    private final List<ResourceLocation> resourceLocations = new ArrayList<ResourceLocation>();
    private final Map<String, ResourceLocation> path2locations = new HashMap<String, ResourceLocation>();
    private final File archiveRoot;
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

        this.archiveRoot = archiveRoot;

        try
        {
            this.archive = new JarFile(new File(archiveRoot, ARCHIVE_JAR_NAME));
            this.codeSource = UrlUtils.generateCodeSourceUrl(getFrameworkName(), getBundleId());

            assert this.codeSource != null;

            this.tmp = new File(archiveRoot, TEMP_NAME);

            if (!tmp.exists() && !tmp.mkdirs()) throw new BundleException("Unable to create temp directory: " + tmp);

            Util.delete(tmp);

            for (String element : getBundleClassPath()) registerClassPathElement(element);
        }
        catch (IOException ioe)
        {
            throw new BundleException("Unable to create jar file", ioe);
        }
    }

    public void assignNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException
    {
        this.nativeCodeDescriptions = nativeCodeDescriptions;

        File archiveDir = new File(archiveRoot, ARCHIVE_NAME);
        for (NativeCodeDescription description : nativeCodeDescriptions)
        {
            for (String path : description.getPaths())
            {
                if (!new File(archiveDir, path).exists()) throw new BundleException("Native code library " + path + " cannot be found.");
            }
        }
    }

    public ResourceLocation registerClassPathElement(String path) throws BundleException
    {
        if (path2locations.containsKey(path)) return path2locations.get(path);

        ResourceLocation result = null;
        if (".".equals(path.trim()))
        {
            result = new BundleDirectoryResourceLocation("", resourceLocations.size());
            resourceLocations.add(result);
            path2locations.put(path, result);
        }
        else
        {
            JarEntry entry = archive.getJarEntry(path + "/");
            if (entry == null) entry = archive.getJarEntry(path);
            if (entry != null)
            {
                if (entry.isDirectory())
                {
                    result = new BundleDirectoryResourceLocation(path, resourceLocations.size());
                }
                else
                {
                    result = new BundleJarResourceLocation(entry, resourceLocations.size());
                }
                resourceLocations.add(result);
                path2locations.put(path, result);
            }
        }

        return result;
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

    public Enumeration<URL> findEntries(String path, String filePattern, boolean includeDirectory, boolean recurse)
    {
        if (path.startsWith("/")) path = path.substring(1);
        if (!path.endsWith("/") && path.length() > 1) path += "/";
        if (filePattern == null) filePattern = "*";

        if (path.length() == 0 && filePattern.length() == 0)
        {
            return Collections.enumeration(Collections.<URL>singleton(UrlUtils.generateEntryUrl(getFrameworkName(), getBundleId(), "", getGeneration())));
        }

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

        List<URL> result = new ArrayList<URL>();
        Enumeration entries = archive.entries();
        while (entries.hasMoreElements())
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String entryName = entry.getName();
            if (entryName.startsWith(path) && (entryName.length() != path.length() || filePattern.length() == 0))
            {
                int count = 0;
                entryName = entryName.substring(path.length());
                for (int i = 0; i < entryName.length(); i++) if (entryName.charAt(i) == '/') count++;

                if (!entry.isDirectory())
                {
                    if (count == 0 && Util.match(targets, entryName))
                    {
                        result.add(UrlUtils.generateEntryUrl(getFrameworkName(), getBundleId(), entry.getName(), getGeneration()));
                    }
                    else if (recurse && Util.match(targets, entryName.substring(entryName.lastIndexOf('/') + 1)))
                    {
                        result.add(UrlUtils.generateEntryUrl(getFrameworkName(), getBundleId(), entry.getName(), getGeneration()));
                    }
                }
                else if (includeDirectory)
                {
                    entryName = entryName.substring(0, Math.max(0, entryName.length() - 1));

                    if (count == 0 && Util.match(targets, entryName))
                    {
                        result.add(UrlUtils.generateEntryUrl(getFrameworkName(), getBundleId(), entry.getName(), getGeneration()));
                    }
                    else if ((recurse || count <= 1) && Util.match(targets, entryName.substring(entryName.lastIndexOf('/') + 1)))
                    {
                        result.add(UrlUtils.generateEntryUrl(getFrameworkName(), getBundleId(), entry.getName(), getGeneration()));
                    }
                }
            }
        }

        return result.isEmpty() ? null : Collections.enumeration(result);
    }

    @SuppressWarnings({ "EmptyCatchBlock" })
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

    public InputStream getInputStreamForCodeSource() throws IOException
    {
        return new FileInputStream(new File(archiveRoot, ARCHIVE_JAR_NAME));
    }

    public InputStream getInputStreamForEntry(String path) throws IOException
    {
        ZipEntry zipEntry = archive.getEntry(path);

        if (zipEntry == null)
        {
            throw new IOException("Path does not exist: " + path);
        }
        else
        {
            return archive.getInputStream(archive.getEntry(path));
        }
    }

    public InputStream getInputStreamForResource(int location, String path) throws IOException
    {
        if (location < 0 || location >= resourceLocations.size()) throw new IOException("Resource location index is out of bounds");

        ResourceLocation resourceLocation = resourceLocations.get(location);
        ResourceHandle handle = resourceLocation.getResourceHandle(path);

        if (handle == null) throw new IOException("Path  does not correspond to a resource");

        return handle.getInputStream();
    }

    @SuppressWarnings({ "EmptyCatchBlock" })
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
            this.path = (path.length() == 0 || path.endsWith("/") ? path : path + "/");
            this.location = location;
        }

        public ResourceHandle getResourceHandle(String resourceName)
        {
            NonCachingArchiveStore archiveStore = NonCachingArchiveStore.this;

            if (resourceName.length() == 0)
            {
                return new BundleRootResourceHandle(UrlUtils.generateResourceUrl(archiveStore.getFrameworkName(), archiveStore.getBundleId(), path + "/", getGeneration(), location));
            }

            ZipEntry entry = archive.getEntry(path + resourceName);
            if (entry != null)
            {
                return new BundleDirectoryResourceHandle(entry, UrlUtils.generateResourceUrl(archiveStore.getFrameworkName(), archiveStore.getBundleId(), path + "/" + entry.getName(), getGeneration(), location));
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

        private class BundleRootResourceHandle extends AbstractResourceHandle
        {
            private final URL url;

            public BundleRootResourceHandle(URL url)
            {
                this.url = url;
            }

            public String getName() { return "/"; }

            public URL getUrl() { return url; }

            public boolean isDirectory() { return true; }

            public URL getCodeSourceUrl() { return codeSource; }

            public InputStream getInputStream() throws IOException
            {
                return new InputStream()
                {
                    public int read() throws IOException { return -1; }
                };
            }

            public int getContentLength() { return 0; }
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
                return new BundleJarResourceHandle(entry, UrlUtils.generateResourceUrl(getFrameworkName(), getBundleId(), "/" + resourceName, getGeneration(), location));
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
            assert archiveDir.mkdirs();

            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(archiveFile));

            return jarInputStream.getManifest().getMainAttributes();
        }
        catch (IOException ioe)
        {
            throw new BundleException("Problems with the bundle archive", ioe);
        }
    }

}
