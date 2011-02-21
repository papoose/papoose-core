/**
 * Copyright 2007-2009 (C) The original author or authors
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.papoose.core.mock.MockStore;
import org.papoose.core.mock.MockURLStreamHandlerFactory;
import org.papoose.core.mock.MockURLStreamHandlerProvider;
import org.papoose.core.spi.Store;

/**
 *
 */
public class BundleContextImplTest
{
    private MockURLStreamHandlerProvider provider;
    private Locale savedLocale;

    @Test
    public void test() throws Exception
    {
        final long earlyTimestamp = System.currentTimeMillis();
        Store fileStore = new MockStore();
        Papoose papoose = new Papoose("org.acme.osgi.0", fileStore, new ScheduledThreadPoolExecutor(10), new Properties());

        papoose.start();

        File testBundle = new File("./target/bundle.jar");
        String location = testBundle.toURI().normalize().toString();

        BundleContext context = papoose.getSystemBundleContext();

        Bundle bundle = context.installBundle(location);

        Assert.assertEquals(1, bundle.getBundleId());

        Assert.assertEquals(location, bundle.getLocation());

        Assert.assertTrue(earlyTimestamp < bundle.getLastModified());

        Dictionary headers = bundle.getHeaders("en");
        Assert.assertEquals("org.papoose.test.papoose-test-bundle", headers.get("Bundle-SymbOLicName"));

        headers = bundle.getHeaders("en");
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = bundle.getHeaders();
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = bundle.getHeaders(null);
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = bundle.getHeaders("en_US");
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = bundle.getHeaders("fr");
        Assert.assertEquals("bundle_fr", headers.get("L10N-Bundle"));

        headers = bundle.getHeaders("fr_FR");
        Assert.assertEquals("bundle_fr_FR", headers.get("L10N-Bundle"));

        headers = bundle.getHeaders("");
        Assert.assertEquals("%bundle", headers.get("L10N-Bundle"));

        headers = bundle.getHeaders("en");
        Assert.assertEquals("no translation for this entry", headers.get("L10N-NoTranslation"));

        papoose.getBundleManager().resolve(bundle);

        papoose.stop();

        fileStore.removeBundleStore(1);
    }

    @Before
    @SuppressWarnings({ "EmptyCatchBlock" })
    public void setUp() throws Exception
    {
        savedLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en", "US"));

        try
        {
            URL.setURLStreamHandlerFactory(new MockURLStreamHandlerFactory());
            MockURLStreamHandlerFactory.addProvider(provider = new MockURLStreamHandlerProvider()
            {
                public URLConnection openConnection(URL url) throws IOException
                {
                    return new URLConnection(url)
                    {
                        private final byte[] bytes;

                        {
                            if ("/com/acme/resource/camera.xml".equals(url.getFile()))
                            {
                                bytes = "<status>good</status>".getBytes();
                            }
                            else
                            {
                                bytes = new byte[0];
                            }
                        }

                        public void connect() throws IOException
                        {
                        }

                        public InputStream getInputStream() throws IOException
                        {
                            return new ByteArrayInputStream(bytes);
                        }
                    };
                }
            });
        }
        catch (Throwable t)
        {
        }
    }

    @After
    public void tearDown() throws Exception
    {
        MockURLStreamHandlerFactory.removeProvider(provider);
        provider = null;

        Locale.setDefault(savedLocale);
        savedLocale = null;
    }
}
