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
package org.papoose.core.mock;

import java.net.URLStreamHandlerFactory;
import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;

/**
 *
 */
public class MockURLStreamHandlerFactory implements URLStreamHandlerFactory
{
    private final static String PREFIX = "sun.net.www.protocol";
    private final static Set<MockURLStreamHandlerProvider> providers = new HashSet<MockURLStreamHandlerProvider>();

    public static void addProvider(MockURLStreamHandlerProvider archiveStore)
    {
        synchronized (providers)
        {
            providers.add(archiveStore);
        }
    }

    public static void removeProvider(MockURLStreamHandlerProvider archiveStore)
    {
        synchronized (providers)
        {
            providers.remove(archiveStore);
        }
    }

    public URLStreamHandler createURLStreamHandler(String protocol)
    {
        if ("resource".equals(protocol)) return new MockURLStreamHandler();
        if ("codesource".equals(protocol)) return new MockURLStreamHandler();
        if ("entry".equals(protocol)) return new MockURLStreamHandler();

        String name = PREFIX + "." + protocol + ".Handler";
        try
        {
            Class clazz = Class.forName(name);
            return (URLStreamHandler) clazz.newInstance();
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

    private class MockURLStreamHandler extends URLStreamHandler
    {
        protected URLConnection openConnection(URL url) throws IOException
        {
            synchronized (providers)
            {
                for (MockURLStreamHandlerProvider provider : providers)
                {
                    URLConnection connection = provider.openConnection(url);
                    if (connection != null) return connection;

                }
                throw new IOException("No providers could handle the URL " + url);
            }
        }
    }
}