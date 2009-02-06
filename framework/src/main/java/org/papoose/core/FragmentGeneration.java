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
import java.util.Enumeration;
import java.util.SortedSet;

import org.osgi.framework.BundleException;

import org.papoose.core.spi.ArchiveStore;


/**
 * @version $Revision$ $Date$
 */
public class FragmentGeneration extends Generation
{
    private BundleGeneration host;

    public FragmentGeneration(BundleController bundleController, ArchiveStore archiveStore)
    {
        super(bundleController, archiveStore);
    }

    public void setNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public BundleGeneration getHost()
    {
        return host;
    }

    public void setHost(BundleGeneration host)
    {
        this.host = host;
    }

    public URL getResource(String name)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration<URL> getResources(String name)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class loadClass(String name)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean hasPermission(Object object)
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
