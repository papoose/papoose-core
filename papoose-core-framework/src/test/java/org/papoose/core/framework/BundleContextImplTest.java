/**
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

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.Locale;
import java.util.Properties;

import junit.framework.TestCase;
import org.osgi.framework.Bundle;

import org.papoose.core.framework.mock.MockThreadPool;

/**
 * @version $Revision$ $Date$
 */
public class BundleContextImplTest extends TestCase
{
    private Locale savedLocale;

    public void test() throws Exception
    {
        File fileStoreRoot = new File("./target/store");
        try
        {
            final long earlyTimestamp = System.currentTimeMillis();
            final int bundleId = 1;
            FileStore fileStore = new FileStore(fileStoreRoot);
            Papoose papoose = new Papoose("org.acme.osgi.0", fileStore, new MockThreadPool(), new Properties());

            papoose.start();

            File testBundle = new File("./target/bundle.jar");
            String location = testBundle.toURI().normalize().toString();

            BundleContextImpl context = new BundleContextImpl((BundleImpl) papoose.getBundleManager().getBundle(0));

            Bundle bundle = context.installBundle(location.toString());

            assertEquals(1, bundle.getBundleId());

            assertEquals(location, bundle.getLocation());

            assertTrue(earlyTimestamp < bundle.getLastModified());

            Dictionary headers = bundle.getHeaders("en");
            assertEquals("org.papoose.test.papoose-test-bundle", (String) headers.get("Bundle-SymbOLicName"));

            headers = bundle.getHeaders("en");
            assertEquals("bundle_en", (String) headers.get("L10N-Bundle"));

            headers = bundle.getHeaders();
            assertEquals("bundle_en", (String) headers.get("L10N-Bundle"));

            headers = bundle.getHeaders(null);
            assertEquals("bundle_en", (String) headers.get("L10N-Bundle"));

            headers = bundle.getHeaders("en_US");
            assertEquals("bundle_en", (String) headers.get("L10N-Bundle"));

            headers = bundle.getHeaders("fr");
            assertEquals("bundle_fr", (String) headers.get("L10N-Bundle"));

            headers = bundle.getHeaders("fr_FR");
            assertEquals("bundle_fr_FR", (String) headers.get("L10N-Bundle"));

            headers = bundle.getHeaders("");
            assertEquals("%bundle", (String) headers.get("L10N-Bundle"));

            headers = bundle.getHeaders("en");
            assertEquals("no translation for this entry", (String) headers.get("L10N-NoTranslation"));

            papoose.getBundleManager().resolve(bundle);

            URL url = bundle.getEntry("com/acme/fuse/dynamite.xml");

            bundle.getLastModified();
//            BufferedReader in = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
//            String line = in.readLine();

            papoose.stop();

            fileStore.removeBundleStore(1);
        }
        finally
        {
            Util.delete(fileStoreRoot);
        }
    }

    @SuppressWarnings({ "EmptyCatchBlock" })
    public void setUp() throws Exception
    {
        super.setUp();

        savedLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en", "US"));

        try
        {
            URL.setURLStreamHandlerFactory(new MockURLStreamHandlerFactory());
        }
        catch (Throwable t)
        {
        }
    }

    public void tearDown() throws Exception
    {
        Locale.setDefault(savedLocale);
        savedLocale = null;

        super.tearDown();
    }
}
