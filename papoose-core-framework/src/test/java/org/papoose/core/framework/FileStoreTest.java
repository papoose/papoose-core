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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.xbean.classloader.ResourceHandle;

import org.papoose.core.framework.mock.MockThreadPool;
import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.test.PapooseTestCase;

/**
 * @version $Revision$ $Date$
 */
public class FileStoreTest extends PapooseTestCase
{
    public void test() throws Exception
    {
        File fileStoreRoot = new File("./target/store");
        try
        {
            FileStore fileStore = new FileStore(fileStoreRoot);
            Papoose papoose = new Papoose(fileStore, new MockThreadPool(), new Properties());
            File testBundle = new File("./target/bundle.jar");

            ArchiveStore archiveStore = fileStore.allocateArchiveStore(papoose, 1, 0, testBundle.toURL().openStream());

            MockURLStreamHandlerFactory.add(archiveStore);

            archiveStore.refreshClassPath(archiveStore.getBundleClassPath());

            String name = archiveStore.getBundleName();
            String activator = archiveStore.getBundleActivatorClass();

            ResourceHandle handle = archiveStore.getResource("com/acme/anvil.xml");

            int length = handle.getContentLength();
            BufferedReader in = new BufferedReader(new InputStreamReader(handle.getInputStream()));
            String line = in.readLine();

            handle = archiveStore.getResource("com/acme/fuse/dynamite.xml");

            length = handle.getContentLength();
            in = new BufferedReader(new InputStreamReader(handle.getInputStream()));
            line = in.readLine();

            List<URL> urls = new ArrayList<URL>();
            for (ResourceHandle h : archiveStore.findResources("com/acme/resource/camera.xml"))
            {
                URL url = h.getUrl();
                assertNotNull(url.openStream());
                urls.add(url);
            }

            fileStore.removeBundleStore(1);
        }
        finally
        {
            Util.delete(fileStoreRoot);
        }
    }

    public void setUp() throws Exception
    {
        super.setUp();

        URL.setURLStreamHandlerFactory(new MockURLStreamHandlerFactory());
    }
}
