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
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.xbean.classloader.JarFileClassLoader;
import org.apache.xbean.classloader.NamedClassLoader;


/**
 * @version $Revision$ $Date$
 */
class BundleClassLoader extends NamedClassLoader
{
    private static final URL[] EMPTY_URLS = new URL[0];
    private final Set<Wire> wires;
    private final String[] bootDelegates;
    private final Wire[] requiredBundles;
    private final JarFileClassLoader bundleClasspathClassloader;
    private final JarFileClassLoader fragmentsClasspathClassloader;
    private final String[] exportedPackages;
    private final Set<ImportDescription> dynamicImports;
    private final Papoose papoose;

    public BundleClassLoader(String name, URL[] bundleClasspath, ClassLoader parent,
                             Set<Wire> wires,
                             String[] bootDelegates,
                             Wire[] requiredBundles,
                             URL[] fragmentsClasspath,
                             String[] exportedPackages,
                             Set<ImportDescription> dynamicImports, Papoose papoose)
    {
        super(name, EMPTY_URLS, parent);

        assert name != null;
        assert wires != null;
        assert bootDelegates != null;
        assert requiredBundles != null;

        this.wires = wires;
        this.bootDelegates = bootDelegates;
        this.requiredBundles = requiredBundles;
        this.bundleClasspathClassloader = new JarFileClassLoader("bundleClasspath." + name, bundleClasspath, DO_NOTHING);
        this.fragmentsClasspathClassloader = new JarFileClassLoader("fragmentsClasspath." + name, fragmentsClasspath, DO_NOTHING);
        this.exportedPackages = exportedPackages;
        this.dynamicImports = dynamicImports;
        this.papoose = papoose;
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException
    {
        if (className.startsWith("java.")) return getParent().loadClass(className);

        int packageIndex = className.lastIndexOf('.');
        String packageName = className.substring(0, (packageIndex < 0 ? 0 : packageIndex));
        String resourceName = className.replace('.', '/') + ".class";

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

        for (Wire wire : wires)
        {
            if (packageName.equals(wire.getPackageName())) return wire.getBundleClassLoader().loadClass(className);
        }

        for (Wire wire : requiredBundles)
        {
            try
            {
                return wire.getBundleClassLoader().loadClass(className);
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
