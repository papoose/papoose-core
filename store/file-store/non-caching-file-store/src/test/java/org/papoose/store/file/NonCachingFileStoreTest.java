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
package org.papoose.store.file;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.papoose.core.UrlUtils;
import org.papoose.core.Util;
import org.papoose.framework.mock.MockURLStreamHandlerFactory;
import org.papoose.framework.mock.MockURLStreamHandlerProvider;


/**
 * @version $Revision$ $Date$
 */
public class NonCachingFileStoreTest
{
    private MockURLStreamHandlerProvider provider;

    @Test
    public void testArchiveStore() throws Exception
    {
        File fileStoreRoot = new File("./target/store");
        try
        {
//            Store fileStore = new NonCachingFileStore(fileStoreRoot);
//            Papoose papoose = new Papoose("org.acme.osgi.0", fileStore, new ScheduledThreadPoolExecutor(10), new Properties());
//            File testBundle = new File("./target/bundle.jar");
//
//            BundleStore bundleStore = fileStore.allocateBundleStore(1, "Test Bundle 1");
//
//            ArchiveStore archiveStore = fileStore.allocateArchiveStore(papoose, 1, testBundle.toURL().openStream());
//
//            archiveStore.refreshClassPath(archiveStore.getBundleClassPath());
//
//            Assert.assertEquals("org.papoose.test.papoose-test-bundle", archiveStore.getBundleSymbolicName());
//            Assert.assertEquals("com.acme.impl.Activator", archiveStore.getBundleActivatorClass());
//
//            ResourceBundle bundle = archiveStore.getResourceBundle(null);
//
//            Enumeration<URL> handle = archiveStore.findEntries("com/acme", "anvil.xml", false, false);
//
//            Assert.assertTrue(handle.hasMoreElements());
//
//            URL anvilURL = handle.nextElement();
//
//            Assert.assertFalse(handle.hasMoreElements());
//
//            InputStream inputStream = anvilURL.openStream();
//            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//            Util.copy(inputStream, outputStream);
//
//            String anvil = outputStream.toString();
//
//            Assert.assertEquals(33, handle.getContentLength());
//            BufferedReader in = new BufferedReader(new InputStreamReader(handle.getInputStreamForEntry()));
//            Assert.assertEquals("<anvil>How now brown cow.</anvil>", in.readLine());
//
//            handle = archiveStore.getResource("com/acme/fuse/dynamite.xml");
//
//            Assert.assertEquals(15, handle.getContentLength());
//            in = new BufferedReader(new InputStreamReader(handle.getInputStreamForEntry()));
//            Assert.assertEquals("<box>BANG</box>", in.readLine());
//
//            List<URL> urls = new ArrayList<URL>();
//            for (ResourceHandle h : archiveStore.findResources("com/acme/resource/camera.xml"))
//            {
//                URL url = h.getUrl();
//                String line = new BufferedReader(new InputStreamReader(url.openStream())).readLine();
//                urls.add(url);
//            }
//
//            fileStore.removeBundleStore(1);
        }
        finally
        {
            Util.delete(fileStoreRoot);
        }
    }

    @Before
    public void setUp() throws Exception
    {
        try
        {
            URL.setURLStreamHandlerFactory(new MockURLStreamHandlerFactory());

            MockURLStreamHandlerFactory.addProvider(provider = new MockURLStreamHandlerProvider()
            {
                public URLConnection openConnection(URL url) throws IOException
                {
                    String path = url.getPath();
                    if ("/com/acme/resource/camera.xml".equals(path))
                    {
                        return new URLConnection(url)
                        {
                            public void connect() throws IOException { }

                            @Override
                            public InputStream getInputStream() throws IOException
                            {
                                return new ByteArrayInputStream("<status>Canon</status>".getBytes("UTF-8"));
                            }
                        };
                    }
                    else if ("/lib/test.jar!/com/acme/resource/camera.xml".equals(path))
                    {
                        return new URLConnection(url)
                        {
                            public void connect() throws IOException { }

                            @Override
                            public InputStream getInputStream() throws IOException
                            {
                                return new ByteArrayInputStream("<status>Nikon</status>".getBytes("UTF-8"));
                            }
                        };
                    }
                    return null;
                }
            });

            MockURLStreamHandlerFactory.addProvider(provider = new MockURLStreamHandlerProvider()
            {
                public URLConnection openConnection(URL url) throws IOException
                {
                    if ("entry".equals(url.getProtocol())) return UrlUtils.allocatEntryConnection(url);
                    if ("resource".equals(url.getProtocol())) return UrlUtils.allocateResourceConnection(url);
                    if ("codesource".equals(url.getProtocol())) return UrlUtils.allocateCodesourceConnection(url);
                    return null;
                }
            });
        }
        catch (Error ignore)
        {
        }
    }

    @After
    public void tearDown() throws Exception
    {
        MockURLStreamHandlerFactory.removeProvider(provider);
        provider = null;
    }
}
