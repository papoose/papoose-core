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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerSetter;


/**
 * @version $Revision$ $Date$
 */
class URLStreamHandlerNotFound implements URLStreamHandlerService
{
    public URLConnection openConnection(URL u) throws IOException
    {
        throw new MalformedURLException("No service registered to handle protocol");
    }

    public String toExternalForm(URL u)
    {
        throw new IllegalStateException("No service registered to handle protocol");
    }

    public boolean hostsEqual(URL u1, URL u2)
    {
        throw new IllegalStateException("No service registered to handle protocol");
    }

    public InetAddress getHostAddress(URL u)
    {
        throw new IllegalStateException("No service registered to handle protocol");
    }

    public boolean sameFile(URL u1, URL u2)
    {
        throw new IllegalStateException("No service registered to handle protocol");
    }

    public int hashCode(URL u)
    {
        throw new IllegalStateException("No service registered to handle protocol");
    }

    public boolean equals(URL u1, URL u2)
    {
        throw new IllegalStateException("No service registered to handle protocol");
    }

    public int getDefaultPort()
    {
        throw new IllegalStateException("No service registered to handle protocol");
    }

    public void parseURL(URLStreamHandlerSetter urlStreamHandlerSetter, URL url, String s, int i, int i1)
    {
        throw new IllegalStateException("No service registered to handle protocol");
    }
}
