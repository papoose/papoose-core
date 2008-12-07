/**
 *
 * Copyright 2008 (C) The original author or authors
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
package org.papoose.core.framework.protocols.bundle;

import java.net.URLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;

import org.papoose.core.framework.BundleManager;


/**
 * @version $Revision$ $Date$
 */
public class BundleUrlConnection extends URLConnection
{
    private final BundleManager bundleManager;
    private final int bundleId;
    private final int location;
    private InputStream inputStream;

    public BundleUrlConnection(URL url, BundleManager bundleManager, int bundleId, int location)
    {
        super(url);
        this.bundleManager = bundleManager;
        this.bundleId = bundleId;
        this.location = location;
    }

    public synchronized void connect() throws IOException
    {
        if (connected) return;

        inputStream = bundleManager.getInputStream(bundleId, location);

        connected = true;
    }

    public synchronized InputStream getInputStream() throws IOException
    {
        if (connected)
        {
            return inputStream;
        }
        else
        {
            connect();
            return inputStream;
        }
    }
}
