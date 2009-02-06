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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.papoose.core.spi.Store;
import org.papoose.store.file.NonCachingFileStore;


/**
 * @version $Revision$ $Date$
 */
public class BundleContextImplTest
{
    @Test
    public void test() throws Exception
    {
        File fileStoreRoot = new File("./target/store");

        Util.delete(fileStoreRoot);

        try
        {
            final long earlyTimestamp = System.currentTimeMillis();
            Store fileStore = new NonCachingFileStore(fileStoreRoot);
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

            URL url = bundle.getEntry("com/acme/resource/camera.xml");

            Assert.assertNotNull(url);

            BufferedReader in = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
            String line = in.readLine();

            Assert.assertEquals("<status>Canon</status>", line);

            int count = 0;
            Enumeration enumeration = bundle.getEntryPaths("com/acme");
            while (enumeration.hasMoreElements())
            {
                String path = (String) enumeration.nextElement();
                count++;
            }

            Assert.assertEquals(7, count);

            count = 0;
            enumeration = bundle.getEntryPaths("/");
            while (enumeration.hasMoreElements())
            {
                String path = (String) enumeration.nextElement();
                count++;
            }

            Assert.assertEquals(4, count);

            count = 0;
            enumeration = bundle.findEntries("com/acme", "*.xml", false);
            while (enumeration.hasMoreElements())
            {
                URL path = (URL) enumeration.nextElement();
                count++;
            }

            Assert.assertEquals(1, count);

            count = 0;
            enumeration = bundle.findEntries("", "*.class", true);
            while (enumeration.hasMoreElements())
            {
                URL path = (URL) enumeration.nextElement();
                count++;
            }

            Assert.assertEquals(5, count);
            
            url = bundle.getResource("com/acme/fuse/dynamite.xml");

            Assert.assertNotNull(url);

            in = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
            line = in.readLine();

            Assert.assertEquals("<box>BANG</box>", line);

            url = bundle.getEntry("com/acme/anvil.xml");

            Assert.assertNotNull(url);

            in = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
            line = in.readLine();

            Assert.assertEquals("<anvil>How now brown cow.</anvil>", line);

            papoose.stop();

            fileStore.removeBundleStore(1);
        }
        finally
        {
            Util.delete(fileStoreRoot);
        }
    }
}
