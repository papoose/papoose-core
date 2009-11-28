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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
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
 * @version $Revision$ $Date$
 */
public class BundleProxy implements Bundle
{
    private final WeakReference<Bundle> reference;

    public BundleProxy(Bundle bundle)
    {
        assert bundle != null;

        reference = new WeakReference<Bundle>(bundle);
    }

    private Bundle getBundle()
    {
        Bundle pinnedBundle = reference.get();
        if (pinnedBundle == null) throw new IllegalStateException("");
        return pinnedBundle;
    }

    public int getState() { return getBundle().getState(); }

    public void start(int options) throws BundleException { getBundle().start(options); }

    public void start() throws BundleException { getBundle().start(); }

    public void stop(int options) throws BundleException { getBundle().stop(options); }

    public void stop() throws BundleException { getBundle().stop(); }

    public void update(InputStream input) throws BundleException { getBundle().update(input); }

    public void update() throws BundleException { getBundle().update(); }

    public void uninstall() throws BundleException { getBundle().uninstall(); }

    public Dictionary getHeaders() { return getBundle().getHeaders(); }

    public long getBundleId() { return getBundle().getBundleId(); }

    public String getLocation() { return getBundle().getLocation(); }

    public ServiceReference[] getRegisteredServices() { return getBundle().getRegisteredServices(); }

    public ServiceReference[] getServicesInUse() { return getBundle().getServicesInUse(); }

    public boolean hasPermission(Object permission) { return getBundle().hasPermission(permission); }

    public URL getResource(String name) { return getBundle().getResource(name); }

    public Dictionary getHeaders(String locale) { return getBundle().getHeaders(locale); }

    public String getSymbolicName() { return getBundle().getSymbolicName(); }

    public Class loadClass(String name) throws ClassNotFoundException { return getBundle().loadClass(name); }

    public Enumeration getResources(String name) throws IOException { return getBundle().getResources(name); }

    public Enumeration getEntryPaths(String path) { return getBundle().getEntryPaths(path); }

    public URL getEntry(String path) { return getBundle().getEntry(path); }

    public long getLastModified() { return getBundle().getLastModified(); }

    public Enumeration findEntries(String path, String filePattern, boolean recurse) { return getBundle().findEntries(path, filePattern, recurse); }

    public BundleContext getBundleContext() { return getBundle().getBundleContext(); }

    public Map getSignerCertificates(int signersType) { return getBundle().getSignerCertificates(signersType); }

    public Version getVersion() { return getBundle().getVersion(); }
}
