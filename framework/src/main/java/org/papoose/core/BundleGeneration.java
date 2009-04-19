/**
 *
 * Copyright 2009 (C) The original author or authors
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Semaphore;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import org.papoose.core.descriptions.NativeCodeDescription;
import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.util.ToStringCreator;


/**
 * @version $Revision$ $Date$
 */
public class BundleGeneration extends Generation
{
    private BundleClassLoader classLoader;
    private final List<FragmentGeneration> fragments = new ArrayList<FragmentGeneration>();
    private final List<BundleGeneration> requiredBundles = new ArrayList<BundleGeneration>();
    private final Semaphore starting = new Semaphore(1, true);

    public BundleGeneration(BundleController bundleController, ArchiveStore archiveStore)
    {
        super(bundleController, archiveStore);
    }

    public boolean isResolved()
    {
        return (getState() & (Bundle.UNINSTALLED | Bundle.INSTALLED)) != 0;
    }

    public BundleClassLoader getClassLoader()
    {
        return classLoader;
    }

    public void setClassLoader(BundleClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    public List<FragmentGeneration> getFragments()
    {
        return fragments;
    }

    public List<BundleGeneration> getRequiredBundles()
    {
        return requiredBundles;
    }

    public URL getResource(String name)
    {
        return null;  //Todo change body of created methods use File | Settings | File Templates.
    }

    public Semaphore getStarting()
    {
        return starting;
    }

    public Enumeration<URL> getResources(String name)
    {
        return null;  //Todo change body of implemented methods use File | Settings | File Templates.
    }

    public Class loadClass(String name)
    {
        return null;  //Todo change body of implemented methods use File | Settings | File Templates.
    }

    public boolean hasPermission(Object object)
    {
        return true;  //Todo change body of implemented methods use File | Settings | File Templates.
    }

    public void setNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException
    {
        //Todo change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("bundleId", getBundleId());
        creator.append("generation", getGeneration());
        creator.append("symbolicName", getSymbolicName());
        creator.append("version", getVersion());

        return creator.toString();
    }
}
