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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.net.URL;
import java.security.Permission;

import org.osgi.framework.BundleException;
import org.apache.xbean.classloader.ResourceHandle;

import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.Store;


/**
 * @version $Revision$ $Date$
 */
public class FileStore implements Store
{
    private final File root;

    public FileStore(File root)
    {
        this.root = root;
    }

    public File getRoot()
    {
        return root;
    }

    public BundleStore allocateBundleStore(long bundleId)
    {
        File bundleRoot = new File(root, "bundles" + File.pathSeparator + bundleId);

        BundleStore result = new FileBundleStore(bundleRoot);

        result.getDataRoot().mkdirs();

        return result;
    }

    public void removeBundleStore(long bundleId)
    {
        File bundleRoot = new File(root, "bundles" + File.pathSeparator + bundleId);

        if (bundleRoot.exists()) Util.delete(bundleRoot);
    }

    public ArchiveStore allocateArchiveStore(long bundleId, int generaton)
    {
        File bundleRoot = new File(root, "bundles" + File.pathSeparator + bundleId + File.pathSeparator + generaton);

        return new FileArchiveStore(bundleRoot);
    }

    public void removeArchiveStore(long bundleId, int generation)
    {
        File bundleRoot = new File(root, "bundles" + File.pathSeparator + bundleId + File.pathSeparator + generation);

        if (bundleRoot.exists()) Util.delete(bundleRoot);
    }

    private class FileBundleStore implements BundleStore
    {
        private final File bundleRoot;

        public FileBundleStore(File bundleRoot)
        {
            this.bundleRoot = bundleRoot;
        }

        public File getDataRoot()
        {
            return new File(bundleRoot, "data");
        }
    }

    private class FileArchiveStore implements ArchiveStore
    {
        private final static String ARCHIVE_JAR_NAME = "archive.jar";
        private final static String ARCHIVE_NAME = "archive";
        private final File bundleRoot;
        private SortedSet<NativeCodeDescription> nativeCodeDescriptions;

        public FileArchiveStore(File bundleRoot)
        {
            this.bundleRoot = bundleRoot;
        }

        public File getDataRoot()
        {
            return new File(bundleRoot, "data");
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

        public void loadArchive(InputStream inputStream) throws BundleException
        {
            try
            {
                File archiveFile = new File(bundleRoot, ARCHIVE_JAR_NAME);
                OutputStream outputStream = new FileOutputStream(archiveFile);

                Util.copy(inputStream, outputStream);

                outputStream.close();

                File archiveDir = new File(bundleRoot, ARCHIVE_NAME);
                archiveDir.mkdirs();

                JarFile jarFile = new JarFile(archiveFile);
                Enumeration list = jarFile.entries();
                while (list.hasMoreElements())
                {
                    dump(archiveDir, jarFile, (ZipEntry) list.nextElement());
                }
            }
            catch (IOException ioe)
            {
                throw new BundleException("Error saving archive", ioe);
            }
        }

        public String loadLibrary(String libname)
        {
            String s = System.mapLibraryName(libname);

            for (NativeCodeDescription description : nativeCodeDescriptions)
            {
                for (String path : description.getPaths())
                {
                    if (path.endsWith(s)) return path;
                }
            }
            return null;
        }

        public Enumeration<URL> findResources(String resourceName)
        {
            return null;  //todo: consider this autogenerated code
        }

        public Permission[] getPermissionCollection()
        {
            return new Permission[0];  //todo: consider this autogenerated code
        }

        public ResourceHandle getResource(String resourceName)
        {
            return null;  //todo: consider this autogenerated code
        }
    }

    private void dump(File root, ZipFile zipFile, ZipEntry zipEntry) throws IOException
    {
        File file = new File(root, zipEntry.getName());
        if (zipEntry.isDirectory())
        {
            file.mkdirs();
        }
        else
        {
            InputStream inputStream = zipFile.getInputStream(zipEntry);
            FileOutputStream outputStream = new FileOutputStream(file);

            Util.copy(inputStream, outputStream);

            inputStream.close();
            outputStream.close();
        }
    }

}
