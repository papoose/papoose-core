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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.Permission;
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

import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.Store;


/**
 * @version $Revision$ $Date$
 */
public class FileStore implements Store
{
    private final static String ARCHIVE_JAR_NAME = "archive.jar";
    private final static String ARCHIVE_NAME = "archive";
    private final static String TEMP_NAME = "tmp";
    private final File root;


    public FileStore(File root) throws BundleException
    {
        this.root = root;

        if (!root.exists() && !root.mkdirs()) throw new BundleException("Unable to create non-existant root: " + root);
    }

    File getRoot()
    {
        return root;
    }

    public List<BundleStore> loadBundleStores() throws BundleException
    {
        File bundlesRoot = new File(root, "bundles" + File.pathSeparator);
        List<BundleStore> result = new ArrayList<BundleStore>();

        for (String bundleId : bundlesRoot.list())
        {
            result.add(new FileBundleStore(new File(bundlesRoot, bundleId), Long.valueOf(bundleId)));
        }

        return result;
    }

    public BundleStore allocateBundleStore(long bundleId, String location) throws BundleException
    {
        File bundleRoot = new File(root, "bundles" + File.separator + bundleId);

        if (bundleRoot.exists()) throw new BundleException("Bundle store location " + bundleRoot + " already exists");
        if (!bundleRoot.mkdirs()) throw new BundleException("Unable to create bundle store location: " + bundleRoot);

        BundleStore result = new FileBundleStore(bundleRoot, bundleId, location);

        result.getDataRoot().mkdirs();

        return result;
    }

    public BundleStore loadBundleStore(long bundleId) throws BundleException
    {
        File bundlesRoot = new File(root, "bundles" + File.separator);
        return new FileBundleStore(new File(bundlesRoot, String.valueOf(bundleId)), bundleId);
    }

    public void removeBundleStore(long bundleId)
    {
        File bundleRoot = new File(root, "bundles" + File.separator + bundleId);

        if (bundleRoot.exists()) Util.delete(bundleRoot);
    }

    public AbstractStore allocateArchiveStore(Papoose framework, long bundleId, int generaton, InputStream inputStream) throws BundleException
    {
        File archiveRoot = new File(root, "bundles" + File.separator + bundleId + File.separator + generaton);

        if (archiveRoot.exists()) throw new BundleException("Archive store location " + archiveRoot + " already exists");
        if (!archiveRoot.mkdirs()) throw new BundleException("Unable to create archive store location: " + archiveRoot);

        return new FileArchiveStore(framework, bundleId, generaton, archiveRoot, inputStream);
    }

    public List<AbstractStore> loadArchiveStores(Papoose framework, long bundleId) throws BundleException
    {
        File archivesRoot = new File(root, "bundles" + File.separator + bundleId + File.separator);
        List<AbstractStore> result = new ArrayList<AbstractStore>();

        for (String generation : archivesRoot.list())
        {
            result.add(new FileArchiveStore(framework, bundleId, Integer.valueOf(generation), new File(archivesRoot, generation)));
        }

        return result;
    }

    public void removeArchiveStore(long bundleId, int generation)
    {
        File bundleRoot = new File(root, "bundles" + File.separator + bundleId + File.separator + generation);

        if (bundleRoot.exists()) Util.delete(bundleRoot);
    }

    private class FileBundleStore implements BundleStore
    {
        private final File bundleRoot;
        private final long bundleId;
        private final String location;
        private transient volatile byte started = -1;
        private transient volatile long lastModified;

        public FileBundleStore(File bundleRoot, long bundleId, String location) throws BundleException
        {
            this.bundleRoot = bundleRoot;
            this.bundleId = bundleId;
            try
            {
                this.location = location;
                File file = new File(bundleRoot, "location");
                DataOutputStream output = new DataOutputStream(new FileOutputStream(file));
                output.writeUTF(this.location);
                output.close();
            }
            catch (IOException ioe)
            {
                throw new BundleException("Unable to set and save bundle location", ioe);
            }
        }

        public FileBundleStore(File bundleRoot, long bundleId) throws BundleException
        {
            this.bundleRoot = bundleRoot;
            this.bundleId = bundleId;
            try
            {
                File file = new File(bundleRoot, "location");
                if (file.exists())
                {
                    DataInputStream input = new DataInputStream(new FileInputStream(file));
                    this.location = input.readUTF();
                    input.close();
                }
                else
                {
                    throw new BundleException("Unable to obtain bundle location");
                }
            }
            catch (FileNotFoundException fnfe)
            {
                throw new BundleException("Unable to obtain bundle location", fnfe);
            }
            catch (IOException ioe)
            {
                throw new BundleException("Unable to obtain bundle location", ioe);
            }
        }

        public long getBundleId()
        {
            return bundleId;
        }

        public String getLocation()
        {
            return location;
        }

        public File getDataRoot()
        {
            return new File(bundleRoot, "data");
        }

        public boolean isStarted() throws BundleException
        {
            try
            {
                if (started == -1)
                {
                    File state = new File(bundleRoot, "state");
                    if (state.exists())
                    {
                        DataInputStream input = new DataInputStream(new FileInputStream(state));
                        started = input.readByte();
                        input.close();
                    }
                    else
                    {
                        started = 0;
                    }
                }
                return started == 1;
            }
            catch (FileNotFoundException fnfe)
            {
                throw new BundleException("Unable to obtain bundle state", fnfe);
            }
            catch (IOException ioe)
            {
                throw new BundleException("Unable to obtain bundle state", ioe);
            }
        }

        public void setStarted(boolean started) throws BundleException
        {
            try
            {
                this.started = (byte) (started ? 1 : 0);
                File state = new File(bundleRoot, "state");
                DataOutputStream output = new DataOutputStream(new FileOutputStream(state));
                output.writeByte(this.started);
                output.close();
            }
            catch (IOException ioe)
            {
                this.started = -1;
                throw new BundleException("Unable to set and save bundle state", ioe);
            }
        }

        public long getLastModified() throws BundleException
        {
            return lastModified;
        }

        public void updateLastModified() throws BundleException
        {
            try
            {
                File modification = new File(bundleRoot, "modification");
                DataOutputStream output = new DataOutputStream(new FileOutputStream(modification));
                output.writeLong(lastModified);
                output.close();
            }
            catch (IOException ioe)
            {
                throw new BundleException("Unable to set and save bundle state", ioe);
            }
        }
    }

    private class FileArchiveStore extends AbstractStore
    {
        private final ResourceLocation fileLocation = new BundleDirectoryResourceLocation("", -1);
        private final List<ResourceLocation> resourceLocations = new ArrayList<ResourceLocation>();
        private final File bundleRoot;
        private final File tmp;
        private final JarFile archive;
        private final URL codeSource;
        private SortedSet<NativeCodeDescription> nativeCodeDescriptions;

        public FileArchiveStore(Papoose framework, long bundleId, int generation, File bundleRoot) throws BundleException
        {
            this(framework, bundleId, generation, bundleRoot, safeStream(new File(bundleRoot, ARCHIVE_JAR_NAME)));
        }

        public FileArchiveStore(Papoose framework, long bundleId, int generation, File bundleRoot, InputStream inputStream) throws BundleException
        {
            super(framework, bundleId, generation, loadAndProvideAttributes(bundleRoot, inputStream));
            this.bundleRoot = bundleRoot;
            try
            {
                this.archive = new JarFile(new File(bundleRoot, ARCHIVE_JAR_NAME));
                this.codeSource = Util.generateUrl(this, "", 0); // TODO: need better URL

                this.tmp = new File(bundleRoot, TEMP_NAME);

                if (!tmp.exists() && !tmp.mkdirs()) throw new BundleException("Unable to create temp directory: " + tmp);
            }
            catch (IOException ioe)
            {
                throw new BundleException("Unable to create jar file", ioe);
            }
        }

        public File getArchive()
        {
            return new File(bundleRoot, ARCHIVE_JAR_NAME);
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

            if (!tmp.mkdirs()) throw new BundleException("Unable to create temp directory: " + tmp);

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

        public Permission[] getPermissionCollection()
        {
            return new Permission[0];  //todo: consider this autogenerated code
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
            return getFrameworkName() + " " + getBundleId() + " " + getGeneration() + " " + archive.getName();
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
                if (entry != null) return new BundleDirectoryResourceHandle(entry, Util.generateUrl(FileArchiveStore.this, path + "/" + entry.getName(), location));
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
                    File jarLocation = new File(tmp, strip(entry.getName()));

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
                if (entry != null) return new BundleJarResourceHandle(entry, Util.generateUrl(FileArchiveStore.this, "/" + path + "!/" + resourceName, location));
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

    private static InputStream safeStream(File file) throws BundleException
    {
        try
        {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException ioe)
        {
            throw new BundleException("Unable to obtain input stream", ioe);
        }
    }

    private static String strip(String src)
    {
        StringBuilder builder = new StringBuilder();

        for (char c : src.toCharArray())
        {
            switch (c)
            {
                case'/':
                {
                    builder.append('_');
                    break;
                }
                case'_':
                {
                    builder.append("__");
                    break;
                }
                default:
                    builder.append(c);
            }
        }

        return builder.toString();
    }
}
