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
package org.papoose.store.file;

import java.io.File;
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

import org.papoose.core.Papoose;
import org.papoose.core.spi.Store;
import org.papoose.core.util.Util;

/**
 * @version $Revision$ $Date$
 */
public class FileStoreTest
{
    private File testDirectory;
    private Locale savedLocale;

    @Test
    public void test() throws Exception
    {
        final long earlyTimestamp = System.currentTimeMillis();
        Store fileStore = new FileStore(testDirectory);
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

        bundle.start();

        long bundleId = bundle.getBundleId();

        papoose.stop();

        fileStore.removeBundleStore(bundleId);
    }

    @Before
    @SuppressWarnings({ "EmptyCatchBlock", "ResultOfMethodCallIgnored" })
    public void setUp() throws Exception
    {
        testDirectory = File.createTempFile("papoose", "test");
        testDirectory.delete();
        testDirectory.mkdir();

        savedLocale = Locale.getDefault();
        Locale.setDefault(new Locale("en", "US"));
    }

    @After
    public void tearDown() throws Exception
    {
        Locale.setDefault(savedLocale);
        savedLocale = null;

        Util.delete(testDirectory);
    }
}
