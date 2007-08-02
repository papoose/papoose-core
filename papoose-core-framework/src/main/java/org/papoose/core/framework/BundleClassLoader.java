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
import java.util.Collections;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

import org.apache.xbean.classloader.JarFileClassLoader;
import org.apache.xbean.classloader.NamedClassLoader;

import org.papoose.core.framework.spi.BundleStore;


/**
 * @version $Revision$ $Date$
 */
public class BundleClassLoader extends NamedClassLoader
{
    private static final URL[] EMPTY_URLS = new URL[0];
    private final String[] bootDelegates;
    private final Wire[] requiredBundles;
    private final JarFileClassLoader bundleClasspathClassloader;
    private final JarFileClassLoader fragmentsClasspathClassloader;
    private final String[] exportedPackages;
    private final Set<ImportDescription> dynamicImports;
    private final Papoose papoose;
    private final SortedSet<BundleStore> bundleStores;
    private Set<Wire> wires;

    BundleClassLoader(String name, URL[] bundleClasspath, ClassLoader parent,
                      String[] bootDelegates,
                      Wire[] requiredBundles,
                      URL[] fragmentsClasspath,
                      String[] exportedPackages,
                      Set<ImportDescription> dynamicImports, Papoose papoose,
                      SortedSet<BundleStore> stores)
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
        this.bundleStores = stores;
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
        for (BundleStore store : bundleStores)
        {
            if ((path = store.loadLibrary(libname)) != null) break;
        }
        return path;
    }

    @SuppressWarnings({"EmptyCatchBlock"})
    protected Class<?> delegateLoadClass(String className) throws ClassNotFoundException
    {
        int packageIndex = className.lastIndexOf('.');
        String packageName = className.substring(0, (packageIndex < 0 ? 0 : packageIndex));

        for (Wire wire : wires)
        {
            if (wire.validFor(className)) return wire.getBundleClassLoader().delegateLoadClass(className);
        }

        for (Wire wire : requiredBundles)
        {
            try
            {
                if (wire.validFor(className)) return wire.getBundleClassLoader().delegateLoadClass(className);
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

        try
        {
            return fragmentsClasspathClassloader.loadClass(className);
        }
        catch (ClassNotFoundException doNothing)
        {
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
                return wire.getBundleClassLoader().loadClass(className);
            }
        }

        throw new ClassNotFoundException();
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
