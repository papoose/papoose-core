/**
 *
 * Copyright 2010 (C) The original author or authors
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
package org.papoose.core.mock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;


/**
 * @version $Revision: $ $Date: $
 */
public class MockBundle implements Bundle
{
    public int getState()
    {
        return 0;
    }

    public void start(int options) throws BundleException
    {
    }

    public void start() throws BundleException
    {
    }

    public void stop(int options) throws BundleException
    {
    }

    public void stop() throws BundleException
    {
    }

    public void update(InputStream input) throws BundleException
    {
    }

    public void update() throws BundleException
    {
    }

    public void uninstall() throws BundleException
    {
    }

    public Dictionary getHeaders()
    {
        return null;
    }

    public long getBundleId()
    {
        return 0;
    }

    public String getLocation()
    {
        return null;
    }

    public ServiceReference[] getRegisteredServices()
    {
        return new ServiceReference[0];
    }

    public ServiceReference[] getServicesInUse()
    {
        return new ServiceReference[0];
    }

    public boolean hasPermission(Object permission)
    {
        return false;
    }

    public URL getResource(String name)
    {
        return null;
    }

    public Dictionary getHeaders(String locale)
    {
        return null;
    }

    public String getSymbolicName()
    {
        return null;
    }

    public Class loadClass(String name) throws ClassNotFoundException
    {
        return null;
    }

    public Enumeration getResources(String name) throws IOException
    {
        return null;
    }

    public Enumeration getEntryPaths(String path)
    {
        return null;
    }

    public URL getEntry(String path)
    {
        return null;
    }

    public long getLastModified()
    {
        return 0;
    }

    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        return null;
    }

    public BundleContext getBundleContext()
    {
        return null;
    }

    public Map getSignerCertificates(int signersType)
    {
        return null;
    }

    public Version getVersion()
    {
        return null;
    }
}
