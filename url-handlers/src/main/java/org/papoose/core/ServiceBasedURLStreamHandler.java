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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.osgi.service.url.URLStreamHandlerSetter;


/**
 * @version $Revision$ $Date$
 */
class ServiceBasedURLStreamHandler extends URLStreamHandler implements URLStreamHandlerSetter
{
    private final URLStreamHandlerServiceImpl owner;
    private final String protocol;

    ServiceBasedURLStreamHandler(URLStreamHandlerServiceImpl owner, String protocol)
    {
        assert owner != null;
        assert protocol != null;

        this.owner = owner;
        this.protocol = protocol;
    }

    protected URLConnection openConnection(URL u) throws IOException
    {
        return owner.lookupUrlStreamHandler(protocol).openConnection(u);
    }

    @Override
    @SuppressWarnings({ "deprecation" })
    public void setURL(URL u, String protocol, String host, int port, String file, String ref)
    {
        super.setURL(u, protocol, host, port, file, ref);
    }

    @Override
    public void setURL(URL u, String protocol, String host, int port, String authority, String userInfo, String path, String query, String ref)
    {
        super.setURL(u, protocol, host, port, authority, userInfo, path, query, ref);
    }

    @Override
    protected String toExternalForm(URL u)
    {
        return owner.lookupUrlStreamHandler(protocol).toExternalForm(u);
    }

    @Override
    protected boolean hostsEqual(URL u1, URL u2)
    {
        return owner.lookupUrlStreamHandler(protocol).hostsEqual(u1, u2);
    }

    @Override
    protected InetAddress getHostAddress(URL u)
    {
        return owner.lookupUrlStreamHandler(protocol).getHostAddress(u);
    }

    @Override
    protected boolean sameFile(URL u1, URL u2)
    {
        return owner.lookupUrlStreamHandler(protocol).sameFile(u1, u2);
    }

    @Override
    protected int hashCode(URL u)
    {
        return owner.lookupUrlStreamHandler(protocol).hashCode(u);
    }

    @Override
    protected boolean equals(URL u1, URL u2)
    {
        return owner.lookupUrlStreamHandler(protocol).equals(u1, u2);
    }

    @Override
    protected int getDefaultPort()
    {
        return owner.lookupUrlStreamHandler(protocol).getDefaultPort();
    }

    @Override
    protected void parseURL(URL u, String spec, int start, int limit)
    {
        owner.lookupUrlStreamHandler(protocol).parseURL(this, u, spec, start, limit);
    }
}
