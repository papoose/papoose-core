/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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
package org.papoose.core.protocols.resource;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.papoose.core.UrlUtils;


/**
 * This URLStreamHandler will be used if our URLStreamHandlerFactory has not
 * been registered w/ URL.
 *
 * @version $Revision$ $Date$
 * @see org.papoose.core.ServiceURLStreamHandlerFactory
 */
public class Handler extends URLStreamHandler
{
    protected URLConnection openConnection(URL url) throws IOException
    {
        return UrlUtils.allocateResourceConnection(url);
    }
}
