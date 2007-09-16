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
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;

import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.BundleException;

import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.Store;


/**
 * @version $Revision$ $Date$
 */
public class FileStore implements Store
{
    private final static String ARCHIVE_JAR_NAME = "archive.jar";
    private final static String ARCHIVE_NAME = "archive";
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
    }

    private class FileArchiveStore extends AbstractStore
    {
        private final Map<String, Certificate[]> certificates = new HashMap<String, Certificate[]>();
        private final File bundleRoot;
        private SortedSet<NativeCodeDescription> nativeCodeDescriptions;

        public FileArchiveStore(Papoose framework, long bundleId, int generation, File bundleRoot, InputStream inputStream) throws BundleException
        {
            super(framework, bundleId, generation, loadAndProvideAttributes(bundleRoot, inputStream));
            this.bundleRoot = bundleRoot;
        }

        public FileArchiveStore(Papoose framework, long bundleId, int generation, File bundleRoot) throws BundleException
        {
            super(framework, bundleId, generation, loadAndProvideAttributes(bundleRoot, new File(bundleRoot, ARCHIVE_JAR_NAME)));
            this.bundleRoot = bundleRoot;
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

        public ResourceHandle getResource(String resourceName, List<String> classPath)
        {
            return null;  //todo: consider this autogenerated code
        }

        public List<URL> findResources(String resourceName, List<String> classPath)
        {
            return null;  //todo: consider this autogenerated code
        }
    }

    static Attributes loadAndProvideAttributes(File bundleRoot, File archiveFile) throws BundleException
    {
        try
        {
            return loadAndProvideAttributes(bundleRoot, new FileInputStream(archiveFile));
        }
        catch (IOException ioe)
        {
            throw new BundleException("Problems with the bundle archive", ioe);
        }
    }

    static Attributes loadAndProvideAttributes(File bundleRoot, InputStream inputStream) throws BundleException
    {
        try
        {
            File archiveFile = new File(bundleRoot, ARCHIVE_JAR_NAME);
            OutputStream outputStream = new FileOutputStream(archiveFile);

            Util.copy(inputStream, outputStream);

            outputStream.close();

            File archiveDir = new File(bundleRoot, ARCHIVE_NAME);
            archiveDir.mkdirs();

            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(new File(bundleRoot, ARCHIVE_JAR_NAME)));

            return jarInputStream.getManifest().getMainAttributes();
        }
        catch (IOException ioe)
        {
            throw new BundleException("Problems with the bundle archive", ioe);
        }
    }
}
