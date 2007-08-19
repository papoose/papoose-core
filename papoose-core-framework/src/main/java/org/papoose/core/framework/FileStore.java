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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.jar.JarEntry;

import org.apache.xbean.classloader.JarResourceHandle;
import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.BundleException;

import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.Store;


/**
 * @version $Revision$ $Date$
 */
public class FileStore implements Store
{
    private final File root;
    private final long frameworkId;
    private final long bundleId;
    private final int generation;


    public FileStore(File root, long frameworkId, long bundleId, int generation)
    {
        this.root = root;
        this.frameworkId = frameworkId;
        this.bundleId = bundleId;
        this.generation = generation;
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

    public ArchiveStore allocateArchiveStore(long bundleId, int generaton, InputStream inputStream) throws BundleException
    {
        try
        {
            File bundleRoot = new File(root, "bundles" + File.pathSeparator + bundleId + File.pathSeparator + generaton);

            return new FileArchiveStore(bundleRoot, inputStream);
        }
        catch (IOException e)
        {
            throw new BundleException("Unable to allocate archive store", e);
        }
    }

    public void removeArchiveStore(long bundleId, int generation)
    {
        File bundleRoot = new File(root, "bundles" + File.pathSeparator + bundleId + File.pathSeparator + generation);

        if (bundleRoot.exists()) Util.delete(bundleRoot);
    }

    private class FileBundleStore implements BundleStore
    {
        private final File bundleRoot;
        private transient volatile byte started = -1;

        public FileBundleStore(File bundleRoot)
        {
            this.bundleRoot = bundleRoot;
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
    }

    private class FileArchiveStore implements ArchiveStore
    {
        private final static String ARCHIVE_JAR_NAME = "archive.jar";
        private final static String ARCHIVE_NAME = "archive";
        private final Map<String, Certificate[]> certificates = new HashMap<String, Certificate[]>();
        private final File bundleRoot;
        private SortedSet<NativeCodeDescription> nativeCodeDescriptions;

        public FileArchiveStore(File bundleRoot, InputStream inputStream) throws IOException
        {
            this.bundleRoot = bundleRoot;

            File archiveFile = new File(bundleRoot, ARCHIVE_JAR_NAME);
            OutputStream outputStream = new FileOutputStream(archiveFile);

            Util.copy(inputStream, outputStream);

            outputStream.close();

            File archiveDir = new File(bundleRoot, ARCHIVE_NAME);
            archiveDir.mkdirs();
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

        public Permission[] getPermissionCollection()
        {
            return new Permission[0];  //todo: consider this autogenerated code
        }

        public ResourceHandle getResource(String resourceName)
        {
            return null;  //todo: consider this autogenerated code
        }
    }
}
