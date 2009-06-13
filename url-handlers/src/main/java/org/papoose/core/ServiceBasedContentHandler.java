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
import java.net.ContentHandler;
import java.net.URLConnection;


/**
 * @version $Revision$ $Date$
 */
class ServiceBasedContentHandler extends ContentHandler
{
    private final URLStreamHandlerServiceImpl owner;
    private final String mimetype;

    ServiceBasedContentHandler(URLStreamHandlerServiceImpl owner, String mimetype)
    {
        assert owner != null;
        assert mimetype != null;

        this.owner = owner;
        this.mimetype = mimetype;
    }

    public Object getContent(URLConnection urlc) throws IOException
    {
        return owner.lookupContentHandler(mimetype).getContent(urlc);
    }
}
