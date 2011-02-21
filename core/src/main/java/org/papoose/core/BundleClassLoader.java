/**
 *
 * Copyright 2007-2009 (C) The original author or authors
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
import java.net.URL;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.xbean.classloader.NamedClassLoader;
import org.apache.xbean.classloader.ResourceHandle;
import org.apache.xbean.classloader.ResourceLocation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.FrameworkEvent;

import org.papoose.core.descriptions.DynamicDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.spi.ArchiveStore;


/**
 *
 */
public class BundleClassLoader extends NamedClassLoader implements BundleReference
{
    private final static URL[] EMPTY_URLS = new URL[0];
    private final Papoose framework;
    private final BundleGeneration bundleGeneration;
    private final Set<Wire> wires;
    private final List<Wire> requiredBundles;
    private final String[] exportedPackages;
    private final List<DynamicDescription> dynamicImports;
    private final List<ResourceLocation> boundClassPath;
    private final Set<ArchiveStore> archiveStores;
    private volatile boolean lazyActivation;

    public BundleClassLoader(Papoose framework,
                             BundleGeneration bundleGeneration,
                             Set<Wire> wires,
                             List<Wire> requiredBundles,
                             String[] exportedPackages,
                             List<DynamicDescription> dynamicImports,
                             List<ResourceLocation> boundClassPath,
                             Set<ArchiveStore> archiveStores) throws BundleException
    {
        super(bundleGeneration.getBundleController().getLocation(), EMPTY_URLS, framework.getClassLoader());

        assert wires != null;
        assert requiredBundles != null;
        assert boundClassPath != null;

        this.framework = framework;
        this.bundleGeneration = bundleGeneration;
        this.wires = new HashSet<Wire>(wires);
        this.requiredBundles = requiredBundles;
        this.exportedPackages = exportedPackages;
        this.dynamicImports = Collections.unmodifiableList(dynamicImports);
        this.boundClassPath = boundClassPath;
        this.archiveStores = archiveStores;
    }

    /**
     * {@inheritDoc}
     */
    public Bundle getBundle()
    {
        return bundleGeneration.getBundleController();
    }

    BundleGeneration getBundleGeneration()
    {
        return bundleGeneration;
    }

    public Set<Wire> getWires()
    {
        return Collections.unmodifiableSet(wires);
    }

    public String[] getExportedPackages()
    {
        String[] result = new String[exportedPackages.length];

        System.arraycopy(exportedPackages, 0, result, 0, result.length);

        return result;
    }

    boolean isLazyActivation()
    {
        return lazyActivation;
    }

    void setLazyActivation(boolean lazyActivation)
    {
        this.lazyActivation = lazyActivation;
    }

    @SuppressWarnings({ "EmptyCatchBlock" })
    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        if (className.startsWith("java.")) return getParent().loadClass(className);

        String packageName = className.substring(0, Math.max(0, className.lastIndexOf('.')));

        for (String delegate : framework.getBootDelegates())
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
        try
        {
            Enumeration<URL> enumeration = huntResources(resourceName, true);

            if (enumeration.hasMoreElements()) return enumeration.nextElement();
            return null;
        }
        catch (IOException ioe)
        {
            return null;
        }
    }

    public Enumeration<URL> findResources(String resourceName)
    {
        try
        {
            return huntResources(resourceName, false);
        }
        catch (IOException ioe)
        {
            return Collections.enumeration(Collections.<URL>emptySet());
        }
    }

    private Enumeration<URL> huntResources(String resourceName, boolean stopAtFirst) throws IOException
    {
        if (resourceName.startsWith("/")) resourceName = resourceName.substring(1, resourceName.length());

        if (resourceName.startsWith("java/")) return getParent().getResources(resourceName);

        String packageName = resourceName.substring(0, Math.max(0, resourceName.lastIndexOf('/')));

        for (String delegate : framework.getBootDelegates())
        {
            delegate = delegate.replace(".", "/");

            if ((delegate.endsWith("/") && packageName.regionMatches(0, delegate, 0, delegate.length() - 1)) || packageName.equals(delegate))
            {
                return getParent().getResources(resourceName);
            }
        }

        return delegateHuntResources(resourceName, stopAtFirst);
    }

    private Enumeration<URL> delegateHuntResources(String resourceName, boolean stopAtFirst)
    {
        String packageName = resourceName.substring(0, Math.max(0, resourceName.lastIndexOf('/')));

        for (Wire wire : wires)
        {
            if (wire.validFor(resourceName)) return wire.getBundleClassLoader().delegateHuntResources(resourceName, stopAtFirst);
        }

        for (Wire wire : requiredBundles)
        {
            if (wire.validFor(resourceName)) return wire.getBundleClassLoader().delegateHuntResources(resourceName, stopAtFirst);
        }

        List<URL> urls = new ArrayList<URL>();
        for (ResourceLocation location : boundClassPath)
        {
            ResourceHandle resourceHandle = location.getResourceHandle(resourceName);
            if (resourceHandle != null)
            {
                urls.add(resourceHandle.getUrl());
            }
        }
        if (!urls.isEmpty()) return Collections.enumeration(urls);


        for (String exportedPackage : exportedPackages)
        {
            if (exportedPackage.equals(packageName)) return Collections.enumeration(Collections.<URL>emptySet());
        }

        synchronized (dynamicImports)
        {
            for (DynamicDescription dynamicDescription : dynamicImports)
            {
                for (String packagePattern : dynamicDescription.getPackagePatterns())
                {
                    if (packagePattern.length() == 0
                        || (packagePattern.endsWith("/") && packageName.startsWith(packagePattern))
                        || packageName.equals(packagePattern))
                    {
                        BundleController bundleController = bundleGeneration.getBundleController();
                        ImportDescription importDescription = new ImportDescription(Collections.singleton(packageName), dynamicDescription.getParameters());

                        Wire wire = bundleController.getFramework().getBundleManager().resolve(bundleController, importDescription);

                        if (wire != null)
                        {
                            wires.add(wire);

                            return wire.getBundleClassLoader().delegateHuntResources(resourceName, stopAtFirst);
                        }
                    }
                }
            }
        }

        return Collections.enumeration(Collections.<URL>emptySet());
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

    @SuppressWarnings({ "EmptyCatchBlock" })
    protected synchronized Class<?> delegateLoadClass(String className) throws ClassNotFoundException
    {
        Class clazz = findLoadedClass(className);

        if (clazz != null)
        {
            return clazz;
        }

        String packageName = className.substring(0, Math.max(0, className.lastIndexOf('.')));

        String wireCheck = className.replace('.', '/');
        for (Wire wire : wires)
        {
            if (wire.validFor(wireCheck)) return wire.getBundleClassLoader().delegateLoadClass(className);
        }

        for (Wire wire : requiredBundles)
        {
            try
            {
                if (wire.validFor(wireCheck)) return wire.getBundleClassLoader().delegateLoadClass(className);
            }
            catch (ClassNotFoundException doNothing)
            {
            }
        }

        for (ResourceLocation location : boundClassPath)
        {
            try
            {
                return findClass(location, className);
            }
            catch (ClassNotFoundException doNothing)
            {
            }
        }

        for (String exportedPackage : exportedPackages)
        {
            if (exportedPackage.equals(packageName)) throw new ClassNotFoundException("Package " + packageName + " for " + className);
        }

        for (DynamicDescription dynamicDescription : dynamicImports)
        {
            for (String packagePattern : dynamicDescription.getPackagePatterns())
            {
                if (packagePattern.length() == 0
                    || (packagePattern.endsWith(".") && packageName.startsWith(packagePattern))
                    || packageName.equals(packagePattern))
                {
                    BundleController bundleController = bundleGeneration.getBundleController();
                    ImportDescription importDescription = new ImportDescription(Collections.singleton(packageName), dynamicDescription.getParameters());

                    Wire wire = bundleController.getFramework().getBundleManager().resolve(bundleController, importDescription);

                    if (wire != null)
                    {
                        // todo: should we remove this description if a wire is created?
                        wires.add(wire);

                        return wire.getBundleClassLoader().delegateLoadClass(className);
                    }
                }
            }
        }

        throw new ClassNotFoundException();
    }

    private Class<?> findClass(final ResourceLocation location, final String className) throws ClassNotFoundException
    {
        try
        {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Class>()
            {
                public Class run() throws ClassNotFoundException
                {
                    BundleManager manager = framework.getBundleManager();

                    // first think check if we are allowed to define the package
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null)
                    {
                        int packageEnd = className.lastIndexOf('.');
                        if (packageEnd >= 0)
                        {
                            String packageName = className.substring(0, packageEnd);
                            securityManager.checkPackageDefinition(packageName);
                        }
                    }

                    // convert the class name to a file name
                    String resourceName = className.replace('.', '/') + ".class";

                    // find the class file resource
                    ResourceHandle resourceHandle = location.getResourceHandle(resourceName);

                    if (resourceHandle == null) throw new ClassNotFoundException(className);

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

                    ProtectionDomain protectionDomain = manager.getProtectionDomainFactory().assignProtectionDomain(bundleGeneration, codeSource, getPermissions(codeSource));

                    // load the class into the vm
                    Class result = defineClass(className, bytes, 0, bytes.length, protectionDomain);

                    if (isLazyActivation())
                    {
                        try
                        {
                            manager.performActivation(BundleClassLoader.this.getBundleGeneration());
                        }
                        catch (BundleException be)
                        {
                            manager.fireFrameworkEvent(new FrameworkEvent(FrameworkEvent.ERROR, bundleGeneration.getBundleController(), be));
                        }
                    }

                    return result;
                }
            }, bundleGeneration.getBundleController().getFramework().getAcc());
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

    @Override
    protected PermissionCollection getPermissions(CodeSource codesource)
    {
        return new BundlePermissionCollection(codesource.getLocation().toExternalForm(), super.getPermissions(codesource));
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
        return "true".equalsIgnoreCase(getAttribute(Attributes.Name.SEALED, packageAttributes, mainAttributes));
    }
}
