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

import java.io.IOException;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.xbean.classloader.JarFileClassLoader;
import org.apache.xbean.classloader.NamedClassLoader;
import org.apache.xbean.classloader.ResourceHandle;

import org.papoose.core.framework.ArchiveStore;


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
    private final AccessControlContext acc = AccessController.getContext();
    private final static URL[] EMPTY_URLS = new URL[0];
    private final String[] bootDelegates;
    private final Wire[] requiredBundles;
    private final JarFileClassLoader bundleClasspathClassloader;
    private final JarFileClassLoader fragmentsClasspathClassloader;
    private final String[] exportedPackages;
    private final Set<ImportDescription> dynamicImports;
    private final Papoose papoose;
    private final SortedSet<ArchiveStore> archiveStores;
    private Set<Wire> wires;

    BundleClassLoader(String name, URL[] bundleClasspath, ClassLoader parent,
                      String[] bootDelegates,
                      Wire[] requiredBundles,
                      URL[] fragmentsClasspath,
                      String[] exportedPackages,
                      Set<ImportDescription> dynamicImports, Papoose papoose,
                      SortedSet<ArchiveStore> stores)
    {
        super(name, EMPTY_URLS, parent);

        assert name != null;
        assert bootDelegates != null;
        assert requiredBundles != null;

        this.bootDelegates = bootDelegates;
        this.requiredBundles = requiredBundles;
        this.bundleClasspathClassloader = new JarFileClassLoader("bundleClasspath." + name, bundleClasspath, DO_NOTHING);
        this.fragmentsClasspathClassloader = new JarFileClassLoader("fragmentsClasspath." + name, fragmentsClasspath, DO_NOTHING);
        this.exportedPackages = exportedPackages;
        this.dynamicImports = dynamicImports;
        this.papoose = papoose;
        this.archiveStores = stores;
    }


    public Set<Wire> getWires()
    {
        return wires;
    }

    void setWires(Set<Wire> wires)
    {
        this.wires = Collections.unmodifiableSet(wires);
    }

    @SuppressWarnings({"EmptyCatchBlock"})
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
        URL url = bundleClasspathClassloader.getResource(resourceName);

        if (url == null) url = fragmentsClasspathClassloader.getResource(resourceName);

        return url;
    }

    public Enumeration<URL> findResources(final String resourceName) throws IOException
    {
        URL url = bundleClasspathClassloader.getResource(resourceName);

        if (url == null) url = fragmentsClasspathClassloader.getResource(resourceName);

        return null;
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

    @SuppressWarnings({"EmptyCatchBlock"})
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

            try
            {
                return bundleClasspathClassloader.loadClass(className);
            }
            catch (ClassNotFoundException doNothing)
            {
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

            for (ImportDescription importDescription : dynamicImports)
            {
                Wire wire = papoose.resolve(importDescription);
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
            }, acc);
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
        private final ArchiveStore archiveStore;

        public BundleCodeSource(URL url, Certificate certs[], ArchiveStore archiveStore)
        {
            super(url, certs);
            this.archiveStore = archiveStore;
        }

        public BundleCodeSource(URL url, CodeSigner[] signers, ArchiveStore archiveStore)
        {
            super(url, signers);
            this.archiveStore = archiveStore;
        }

        public ArchiveStore getArchiveStore()
        {
            return archiveStore;
        }
    }

    private final static ClassLoader DO_NOTHING = new ClassLoader()
    {
        public Class<?> loadClass(String name) throws ClassNotFoundException
        {
            throw new ClassNotFoundException();
        }

        public URL getResource(String name)
        {
            return null;
        }

        public Enumeration<URL> getResources(String name)
        {
            return new Enumeration<URL>()
            {
                public URL nextElement()
                {
                    throw new NoSuchElementException();
                }

                public boolean hasMoreElements()
                {
                    return false;
                }
            };
        }
    };
}
