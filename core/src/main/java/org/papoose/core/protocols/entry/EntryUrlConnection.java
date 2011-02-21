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
package org.papoose.core.protocols.entry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.papoose.core.BundleManager;


/**
 *
 */
public class EntryUrlConnection extends URLConnection
{
    private final BundleManager bundleManager;
    private final long bundleId;
    private final int generation;
    private InputStream inputStream;

    public EntryUrlConnection(URL url, BundleManager bundleManager, long bundleId, int generation)
    {
        super(url);

        assert url != null;
        assert bundleManager != null;
        assert bundleId >= 0;
        assert generation >= 0;

        this.bundleManager = bundleManager;
        this.bundleId = bundleId;
        this.generation = generation;
    }

    public synchronized void connect() throws IOException
    {
        if (connected) return;

        inputStream = bundleManager.getInputStreamForEntry(bundleId, generation, url.getPath().substring(1));

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
