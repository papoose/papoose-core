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
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.xbean.classloader.NamedClassLoader;
import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import org.papoose.core.framework.spi.ArchiveStore;


/**
 * @version $Revision$ $Date$
 */
public class BundleClassLoader extends NamedClassLoader
{
    private final static ThreadLocal<Set<BundleClassLoader>> LOADER_VISITED = new ThreadLocal<Set<BundleClassLoader>>()
    {
        protected Set<BundleClassLoader> initialValue()
        {
            return new HashSet<BundleClassLoader>();
        }
    };
    private final static URL[] EMPTY_URLS = new URL[0];
    private final Papoose framework;
    private final BundleImpl bundle;
    private final String[] bootDelegates;
    private final List<Wire> requiredBundles;
    private final String[] exportedPackages;
    private final Set<DynamicDescription> dynamicImports;
    private final SortedSet<ArchiveStore> archiveStores = new TreeSet<ArchiveStore>();
    private final List<String> classPath = new ArrayList<String>();
    private Set<Wire> wires;

    BundleClassLoader(String name, ClassLoader parent,
                      Papoose framework,
                      BundleImpl bundle,
                      List<Wire> requiredBundles,
                      String[] bootDelegates,
                      String[] exportedPackages,
                      Set<DynamicDescription> dynamicImports,
                      final List<ArchiveStore> archiveStores)
    {
        super(name, EMPTY_URLS, parent);

        assert name != null;
        assert framework != null;
        assert bundle != null;
        assert requiredBundles != null;

        this.framework = framework;
        this.bundle = bundle;
        this.requiredBundles = requiredBundles;
        this.bootDelegates = bootDelegates;
        this.exportedPackages = exportedPackages;
        this.dynamicImports = dynamicImports;


        ArchiveStore store = bundle.getCurrentStore();
        List<RequireDescription> list = store.getBundleRequireBundle();
        for (RequireDescription description : list)
        {
//            Wire wire = new Wire(description.)
        }

        addArchiveStore(new BundleArchiveStore(archiveStores.get(0)));

        for (int i = 1; i < archiveStores.size(); i++) addArchiveStore(archiveStores.get(i));
    }


    BundleImpl getBundle()
    {
        return bundle;
    }

    public Set<Wire> getWires()
    {
        return wires;
    }

    void setWires(Set<Wire> wires)
    {
        assert this.wires == null;
        this.wires = Collections.unmodifiableSet(wires);
    }

    /**
     * Add an archive store to a sorted set of archive store and rebuild the
     * bundle classpath after adding the new store's own classpath.
     *
     * @param archiveStore the store to be added
     */
    void addArchiveStore(ArchiveStore archiveStore)
    {
        archiveStores.add(archiveStore);

        classPath.clear();
        for (ArchiveStore a : archiveStores)
        {
            for (String path : a.getBundleClassPath())
            {
                if (!classPath.contains(path)) classPath.add(path);
            }
        }
    }

    @SuppressWarnings({ "EmptyCatchBlock" })
    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        if (className.startsWith("java.")) return getParent().loadClass(className);

        int packageIndex = className.lastIndexOf('.');
        String packageName = className.substring(0, (packageIndex < 0 ? 0 : packageIndex));

        for (String delegate : bootDelegates)
        {
            if ((delegate.endsWith(".") && packageName.regionMatches(0, delegate, 0, delegate.length() - 1)) || packageName.equals(delegate))
            {
                try
                {
                    return getParent().loadClass(className);
                }
                catch (ClassNotFoundException doNothing)
                {
                }
            }
        }

        return delegateLoadClass(className);
    }

    public URL getResource(String resourceName)
    {
        for (ArchiveStore archiveStore : archiveStores)
        {
            ResourceHandle handle = archiveStore.getResource(resourceName);
            if (handle != null) return handle.getUrl();
        }

        return null;
    }

    public Enumeration<URL> findResources(final String resourceName) throws IOException
    {
        List<URL> urls = new ArrayList<URL>();

        for (ArchiveStore archiveStore : archiveStores)
        {
            for (ResourceHandle handle : archiveStore.findResources(resourceName))
            {
                urls.add(handle.getUrl());
            }
        }

        return Collections.enumeration(urls);
    }

    protected String findLibrary(String libname)
    {
        String path = null;
        for (ArchiveStore store : archiveStores)
        {
            if ((path = store.loadLibrary(libname)) != null) break;
        }
        return path;
    }

    protected PermissionCollection getPermissions(CodeSource codesource)
    {
        PermissionCollection collection = super.getPermissions(codesource);

        if (codesource instanceof BundleCodeSource)
        {
            for (Permission permission : ((BundleCodeSource) codesource).getArchiveStore().getPermissionCollection())
            {
                collection.add(permission);
            }
        }

        return collection;
    }

    @SuppressWarnings({ "EmptyCatchBlock" })
    protected Class<?> delegateLoadClass(String className) throws ClassNotFoundException
    {
        assert !inSet(this);

        register(this);

        try
        {
            int packageIndex = className.lastIndexOf('.');
            String packageName = className.substring(0, (packageIndex < 0 ? 0 : packageIndex));

            for (Wire wire : wires)
            {
                if (wire.validFor(className) && !inSet(wire.getBundleClassLoader())) return wire.getBundleClassLoader().delegateLoadClass(className);
            }

            for (Wire wire : requiredBundles)
            {
                try
                {
                    if (wire.validFor(className) && !inSet(wire.getBundleClassLoader())) return wire.getBundleClassLoader().delegateLoadClass(className);
                }
                catch (ClassNotFoundException doNothing)
                {
                }
            }

            for (ArchiveStore archiveStore : archiveStores)
            {
                try
                {
                    return findClass(archiveStore, className);
                }
                catch (ClassNotFoundException doNothing)
                {
                }
            }

            for (String exportedPackage : exportedPackages)
            {
                if (exportedPackage.equals(packageName)) throw new ClassNotFoundException();
            }

            for (DynamicDescription dynamicDescription : dynamicImports)
            {
                Wire wire = framework.resolve(dynamicDescription);
                if (wire != null)
                {
                    wires.add(wire);
                    if (!inSet(wire.getBundleClassLoader())) return wire.getBundleClassLoader().delegateLoadClass(className);
                }
            }

            throw new ClassNotFoundException();
        }
        finally
        {
            unregister(this);
        }
    }

    protected Class<?> findClass(final ArchiveStore archiveStore, final String className) throws ClassNotFoundException
    {
        try
        {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class>()
            {
                public Class run() throws ClassNotFoundException
                {
                    // first think check if we are allowed to define the package
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null)
                    {
                        String packageName;
                        int packageEnd = className.lastIndexOf('.');
                        if (packageEnd >= 0)
                        {
                            packageName = className.substring(0, packageEnd);
                            securityManager.checkPackageDefinition(packageName);
                        }
                    }

                    // convert the class name to a file name
                    String resourceName = className.replace('.', '/') + ".class";

                    // find the class file resource
                    ResourceHandle resourceHandle = archiveStore.getResource(resourceName);
                    if (resourceHandle == null)
                    {
                        throw new ClassNotFoundException(className);
                    }

                    byte[] bytes;
                    Manifest manifest;
                    try
                    {
                        // get the bytes from the class file
                        bytes = resourceHandle.getBytes();

                        // get the manifest for defining the packages
                        manifest = resourceHandle.getManifest();
                    }
                    catch (IOException e)
                    {
                        throw new ClassNotFoundException(className, e);
                    }

                    // get the certificates for the code source
                    Certificate[] certificates = resourceHandle.getCertificates();

                    // the code source url is used to define the package and as the security context for the class
                    URL codeSourceUrl = resourceHandle.getCodeSourceUrl();

                    // define the package (required for security)
                    definePackage(className, codeSourceUrl, manifest);

                    // this is the security context of the class
                    CodeSource codeSource = new CodeSource(codeSourceUrl, certificates);

                    // load the class into the vm
                    return defineClass(className, bytes, 0, bytes.length, codeSource);
                }
            }, bundle.getFramework().getAcc());
        }
        catch (PrivilegedActionException e)
        {
            throw (ClassNotFoundException) e.getException();
        }
    }

    private void definePackage(String className, URL jarUrl, Manifest manifest)
    {
        int packageEnd = className.lastIndexOf('.');
        if (packageEnd < 0)
        {
            return;
        }

        String packageName = className.substring(0, packageEnd);
        String packagePath = packageName.replace('.', '/') + "/";

        Attributes packageAttributes = null;
        Attributes mainAttributes = null;
        if (manifest != null)
        {
            packageAttributes = manifest.getAttributes(packagePath);
            mainAttributes = manifest.getMainAttributes();
        }
        Package pkg = getPackage(packageName);
        if (pkg != null)
        {
            if (pkg.isSealed())
            {
                if (!pkg.isSealed(jarUrl))
                {
                    throw new SecurityException("Package was already sealed with another URL: package=" + packageName + ", url=" + jarUrl);
                }
            }
            else
            {
                if (isSealed(packageAttributes, mainAttributes))
                {
                    throw new SecurityException("Package was already been loaded and not sealed: package=" + packageName + ", url=" + jarUrl);
                }
            }
        }
        else
        {
            String specTitle = getAttribute(Attributes.Name.SPECIFICATION_TITLE, packageAttributes, mainAttributes);
            String specVendor = getAttribute(Attributes.Name.SPECIFICATION_VENDOR, packageAttributes, mainAttributes);
            String specVersion = getAttribute(Attributes.Name.SPECIFICATION_VERSION, packageAttributes, mainAttributes);
            String implTitle = getAttribute(Attributes.Name.IMPLEMENTATION_TITLE, packageAttributes, mainAttributes);
            String implVendor = getAttribute(Attributes.Name.IMPLEMENTATION_VENDOR, packageAttributes, mainAttributes);
            String implVersion = getAttribute(Attributes.Name.IMPLEMENTATION_VERSION, packageAttributes, mainAttributes);

            URL sealBase = null;
            if (isSealed(packageAttributes, mainAttributes))
            {
                sealBase = jarUrl;
            }

            definePackage(packageName, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor, sealBase);
        }
    }

    private String getAttribute(Attributes.Name name, Attributes packageAttributes, Attributes mainAttributes)
    {
        if (packageAttributes != null)
        {
            String value = packageAttributes.getValue(name);
            if (value != null)
            {
                return value;
            }
        }
        if (mainAttributes != null)
        {
            return mainAttributes.getValue(name);
        }
        return null;
    }

    private boolean isSealed(Attributes packageAttributes, Attributes mainAttributes)
    {
        String sealed = getAttribute(Attributes.Name.SEALED, packageAttributes, mainAttributes);
        return sealed != null && "true".equalsIgnoreCase(sealed);
    }


    private static boolean inSet(BundleClassLoader bundleClassLoader)
    {
        return LOADER_VISITED.get().contains(bundleClassLoader);
    }

    private static void register(BundleClassLoader bundleClassLoader)
    {
        LOADER_VISITED.get().add(bundleClassLoader);
    }

    private static void unregister(BundleClassLoader bundleClassLoader)
    {
        LOADER_VISITED.get().remove(bundleClassLoader);
    }

    private final static class BundleCodeSource extends CodeSource
    {
        private final AbstractStore archiveStore;

        public BundleCodeSource(URL url, Certificate certs[], AbstractStore archiveStore)
        {
            super(url, certs);
            this.archiveStore = archiveStore;
        }

        public BundleCodeSource(URL url, CodeSigner[] signers, AbstractStore archiveStore)
        {
            super(url, signers);
            this.archiveStore = archiveStore;
        }

        public AbstractStore getArchiveStore()
        {
            return archiveStore;
        }
    }

    /**
     * A nice wrapper that makes sure that this particular archive is first in
     * the list, i.e. the bundle archive.
     */
    static class BundleArchiveStore implements ArchiveStore
    {
        private final ArchiveStore delegate;

        public BundleArchiveStore(ArchiveStore delegate) { this.delegate = delegate; }

        public String getFrameworkName() { return delegate.getFrameworkName(); }

        public long getBundleId() { return delegate.getBundleId(); }

        public int getGeneration() { return delegate.getGeneration(); }

        public Attributes getAttributes() { return delegate.getAttributes(); }

        public String getBundleActivatorClass() { return delegate.getBundleActivatorClass(); }

        public String getBundleSymbolicName() { return delegate.getBundleSymbolicName(); }

        public Version getBundleVersion() { return delegate.getBundleVersion(); }

        public List<String> getBundleClassPath() { return delegate.getBundleClassPath(); }

        public List<ExportDescription> getBundleExportList() { return delegate.getBundleExportList(); }

        public List<ImportDescription> getBundleImportList() { return delegate.getBundleImportList(); }

        public List<RequireDescription> getBundleRequireBundle() { return delegate.getBundleRequireBundle(); }

        public Set<DynamicDescription> getDynamicImportSet() {return delegate.getDynamicImportSet();}

        public void refreshClassPath(List<String> classPath) throws BundleException { delegate.refreshClassPath(classPath); }

        public String loadLibrary(String libname) { return delegate.loadLibrary(libname); }

        public Permission[] getPermissionCollection() { return delegate.getPermissionCollection(); }

        public ResourceHandle getEntry(String name) { return delegate.getResource(name); }

        public Enumeration getEntryPaths(String path) { return delegate.getEntryPaths(path); }

        public Enumeration findEntries(String path, String filePattern, boolean recurse) { return delegate.findEntries(path, filePattern, recurse); }

        public ResourceHandle getResource(String resourceName) { return delegate.getResource(resourceName); }

        public ResourceHandle getResource(String resourceName, int location) { return delegate.getResource(resourceName, location); }

        public List<ResourceHandle> findResources(String resourceName) { return delegate.findResources(resourceName); }

        public L18nResourceBundle getResourceBundle(Locale local) { return delegate.getResourceBundle(local); }

        public void close() { delegate.close(); }

        public int compareTo(Object o) { return 1; }
    }
}
