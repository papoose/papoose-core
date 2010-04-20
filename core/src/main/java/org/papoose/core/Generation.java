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
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.SortedSet;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import org.papoose.core.descriptions.NativeCodeDescription;
import org.papoose.core.spi.ArchiveStore;


/**
 * @version $Revision$ $Date$
 */
public abstract class Generation
{
    private final BundleController bundleController;
    private final ArchiveStore archiveStore;
    private volatile int state = Bundle.INSTALLED;
    private volatile ProtectionDomain protectionDomain;

    protected Generation(BundleController bundleController, ArchiveStore archiveStore)
    {
        this.bundleController = bundleController;
        this.archiveStore = archiveStore;
    }

    public BundleController getBundleController()
    {
        return bundleController;
    }

    public long getBundleId()
    {
        return bundleController.getBundleId();
    }

    public int getGeneration()
    {
        return archiveStore.getGeneration();
    }

    public ArchiveStore getArchiveStore()
    {
        return archiveStore;
    }

    public int getState()
    {
        return state;
    }

    public void setState(int state)
    {
        this.state = state;
    }

    public ProtectionDomain getProtectionDomain()
    {
        return protectionDomain;
    }

    public void setProtectionDomain(ProtectionDomain protectionDomain)
    {
        this.protectionDomain = protectionDomain;
    }

    public String getSymbolicName()
    {
        return getArchiveStore().getBundleSymbolicName();
    }

    public final Version getVersion()
    {
        return getArchiveStore().getBundleVersion();
    }

    /**
     * Set the native code descriptions that the bundle store is to use
     * when loading native code libraries.
     *
     * @param nativeCodeDescriptions the sorted set of native code descriptions
     * @throws BundleException if the set of native code descriptions is empty
     */
    public abstract void setNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException;

    public abstract URL getResource(String name);

    public abstract Enumeration getResources(String name);

    public abstract Class loadClass(String name);

    public abstract boolean hasPermission(Object object);
}
