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
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashSet;
import java.util.Set;

import org.apache.xbean.classloader.ResourceHandle;

import org.papoose.core.framework.spi.ArchiveStore;

/**
 * @version $Revision$ $Date$
 */
public class MockURLStreamHandlerFactory implements URLStreamHandlerFactory
{
    private final static String PREFIX = "sun.net.www.protocol";
    private final static Set<ArchiveStore> stores = new HashSet<ArchiveStore>();

    public static void add(ArchiveStore archiveStore)
    {
        stores.add(archiveStore);
    }

    public URLStreamHandler createURLStreamHandler(String protocol)
    {
        if ("papoose".equals(protocol)) return new Handler();

        String name = PREFIX + "." + protocol + ".Handler";
        try
        {
            Class c = Class.forName(name);
            return (URLStreamHandler) c.newInstance();
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        throw new InternalError("could not load " + protocol + "system protocol handler");
    }

    private class Handler extends URLStreamHandler
    {
        protected URLConnection openConnection(final URL url) throws IOException
        {
            return new URLConnection(url)
            {
                private ResourceHandle handle;

                {
                    try
                    {
                        String frameworkId = url.getHost();
                        String[] tokens = url.getUserInfo().split(":");
                        long bundleId = Long.valueOf(tokens[0]);
                        int location = Integer.valueOf(tokens[1]);
                        String path = url.getPath().substring(1);
                        if (path.contains("!/")) path = path.split("!")[1].substring(1);

                        for (ArchiveStore archiveStore : stores)
                        {
                            if (archiveStore.getFrameworkName().equals(frameworkId) && archiveStore.getBundleId() == bundleId)
                            {
                                handle = archiveStore.getResource(path, location);
                                break;
                            }
                        }
                    }
                    catch (NumberFormatException nfe)
                    {
                        throw new IOException("Invalid id " + url);
                    }
                    if (handle == null) throw new IOException("Missing matching archive store " + url);
                }

                public void connect() throws IOException
                {
                }

                public InputStream getInputStream() throws IOException
                {
                    return handle.getInputStream();
                }
            };
        }
    }
}