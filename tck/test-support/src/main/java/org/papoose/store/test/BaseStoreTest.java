/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.papoose.store.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.jar.Attributes;

import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.papoose.core.AutostartSetting;
import org.papoose.core.Papoose;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.LazyActivationDescription;
import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.spi.BundleStore;
import org.papoose.core.spi.Store;


/**
 * @version $Revision: $ $Date: $
 */
public abstract class BaseStoreTest
{
    abstract Store createStore();

    @Test
    public void test() throws Exception
    {
        final long earlyTimestamp = System.currentTimeMillis();
        Store fileStore = createStore();
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
        Assert.assertEquals("org.papoose.test.bundles.test-bundle", headers.get("Bundle-SymbOLicName"));

        headers = bundle.getHeaders("en");
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = bundle.getHeaders();
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = bundle.getHeaders(null);
        assertEquals("bundle_en", headers.get("L10N-Bundle"));

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

    @Test
    public void testStartAndStop() throws Exception
    {
        Store store = createStore();
        store.start();
        store.start();
        store.start();

        store.stop();
        store.stop();
        store.stop();
    }

    @Test
    public void testSystemBundleStore() throws Exception
    {
        Store store = createStore();

        store.start();

        List<BundleStore> list = store.loadBundleStores();
        assertTrue(list.isEmpty());

        BundleStore bundleStore = store.obtainSystemBundleStore();
        assertNotNull(bundleStore);
        assertEquals(0, bundleStore.getBundleId());
        assertEquals(Constants.SYSTEM_BUNDLE_LOCATION, bundleStore.getLocation());

        long mark = bundleStore.getLastModified();
        Thread.sleep(10);
        assertTrue(mark < System.currentTimeMillis());
        bundleStore.markModified();
        assertTrue(mark < bundleStore.getLastModified());

        assertEquals(AutostartSetting.STOPPED, bundleStore.getAutostart());
        bundleStore.setAutoStart(AutostartSetting.EAGER);
        assertEquals(AutostartSetting.EAGER, bundleStore.getAutostart());

        File file = new File(bundleStore.getDataRoot(), "test.data");
        assertFalse(file.exists());

        OutputStream out = new FileOutputStream(file);
        out.write(1);
        out.write(2);
        out.write(3);
        out.close();
        assertTrue(file.exists());

        store.stop();

        store = createStore();

        store.start();

        list = store.loadBundleStores();
        assertTrue(list.isEmpty());

        bundleStore = store.obtainSystemBundleStore();

        assertEquals(AutostartSetting.STOPPED, bundleStore.getAutostart());

        file = new File(bundleStore.getDataRoot(), "test.data");
        assertTrue(file.exists());

        InputStream in = new FileInputStream(file);
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(3, in.read());
        in.close();

        assertTrue(file.exists());

        store.stop();

        store.clear();

        store = createStore();

        store.start();

        assertFalse(file.exists());

        store.stop();
    }

    @Test
    public void testAllocateBundleStore() throws Exception
    {
        Store store = createStore();

        store.start();

        try
        {
            store.allocateBundleStore(0, "Test");
            fail("Should not be able to allocate system bundle store using this method");
        }
        catch (BundleException e)
        {
        }

        try
        {
            store.allocateBundleStore(-1, "Test");
            fail("Should not be able to use invalid bundle id");
        }
        catch (BundleException e)
        {
        }

        try
        {
            store.allocateBundleStore(1, null);
            fail("Should not be able to use null location");
        }
        catch (BundleException e)
        {
        }

        BundleStore bundleStore = store.allocateBundleStore(1, "First Bundle");

        try
        {
            store.allocateBundleStore(1, "Another Try");
            fail("Should not be able to re-allocate bundle");
        }
        catch (BundleException e)
        {
        }

        assertNotNull(bundleStore);
        assertEquals(1, bundleStore.getBundleId());
        assertEquals("First Bundle", bundleStore.getLocation());

        long mark = bundleStore.getLastModified();
        Thread.sleep(10);
        assertTrue(mark < System.currentTimeMillis());
        bundleStore.markModified();
        assertTrue(mark < bundleStore.getLastModified());

        assertEquals(AutostartSetting.STOPPED, bundleStore.getAutostart());
        bundleStore.setAutoStart(AutostartSetting.EAGER);
        assertEquals(AutostartSetting.EAGER, bundleStore.getAutostart());

        File file = new File(bundleStore.getDataRoot(), "test.data");
        assertFalse(file.exists());

        OutputStream out = new FileOutputStream(file);
        out.write(1);
        out.write(2);
        out.write(3);
        out.close();
        assertTrue(file.exists());

        store.stop();

        store = createStore();

        store.start();

        List<BundleStore> list = store.loadBundleStores();
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());

        bundleStore = list.get(0);

        assertEquals(1, bundleStore.getBundleId());
        assertEquals("First Bundle", bundleStore.getLocation());
        assertEquals(AutostartSetting.EAGER, bundleStore.getAutostart());

        file = new File(bundleStore.getDataRoot(), "test.data");
        assertTrue(file.exists());

        InputStream in = new FileInputStream(file);
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(3, in.read());
        in.close();

        assertTrue(file.exists());

        store.stop();

        store.clear();

        store = createStore();

        store.start();

        list = store.loadBundleStores();
        assertTrue(list.isEmpty());

        assertFalse(file.exists());

        store.allocateBundleStore(1, "First Bundle");

        list = store.loadBundleStores();
        assertFalse(list.isEmpty());

        store.removeBundleStore(1);

        list = store.loadBundleStores();
        assertTrue(list.isEmpty());

        store.stop();
    }

    @Test
    public void testArchiveStore() throws Exception
    {
        Store store = createStore();

        Papoose framework = new Papoose(store, new ScheduledThreadPoolExecutor(10));

        store.start();

        ArchiveStore archiveStore = store.allocateArchiveStore(framework, 1, new FileInputStream(new File("./target/bundle.jar")));

        checkArchiveStore(archiveStore, framework);

        store.stop();

        store = createStore();

        store.start();

        checkArchiveStore(store.loadArchiveStore(framework, 1), framework);

        store.clear();

        try
        {
            store.loadArchiveStore(framework, 1);
            fail("The archive should no longer exist");
        }
        catch (BundleException e)
        {
        }

        store.stop();

        store = createStore();

        store.start();

        try
        {
            store.loadArchiveStore(framework, 1);
            fail("The archive should no longer exist");
        }
        catch (BundleException e)
        {
        }

        store.stop();
    }

    protected void checkArchiveStore(ArchiveStore archiveStore, Papoose framework) throws Exception
    {
        assertNotNull(archiveStore);
        assertEquals(1, archiveStore.getBundleId());
        assertEquals(0, archiveStore.getGeneration());
        assertEquals("org.papoose.test.bundles.test-bundle", archiveStore.getBundleSymbolicName());
        assertEquals("com.acme.impl.Activator", archiveStore.getBundleActivatorClass());
        assertEquals(new Version(1, 0, 0), archiveStore.getBundleVersion());

        List<String> classPath = archiveStore.getBundleClassPath();
        assertNotNull(classPath);
        assertEquals(1, classPath.size());
        assertEquals(".", classPath.get(0));

        Attributes attributes = archiveStore.getAttributes();
        assertNotNull(attributes);

        assertNotNull(archiveStore.getBundleNativeCodeList());
        assertEquals(0, archiveStore.getBundleNativeCodeList().size());
        assertNotNull(archiveStore.getBundleRequiredExecutionEnvironment());
        assertEquals(0, archiveStore.getBundleRequiredExecutionEnvironment().size());
        assertNull(archiveStore.getBundleUpdateLocation());
        assertNull(archiveStore.getCertificates());
        assertEquals(framework.getFrameworkName(), archiveStore.getFrameworkName());
        assertNotNull(archiveStore.getDynamicDescriptions());
        assertEquals(0, archiveStore.getDynamicDescriptions().size());

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("bundle-version", new Version(1, 0, 0));
        map.put("bundle-symbolic-name", "org.papoose.test.bundles.test-bundle");
        map.put("version", new Version(0, 0, 0));

        List<ExportDescription> exportDescriptions = archiveStore.getExportDescriptions();
        assertNotNull(exportDescriptions);
        assertEquals(1, exportDescriptions.size());
        assertEquals(new ExportDescription(Collections.singleton("com.acme.api"), map), exportDescriptions.get(0));

        assertNull(archiveStore.getFragmentDescription());
        assertEquals(new LazyActivationDescription(false), archiveStore.getLazyActivationDescription());
        assertNotNull(archiveStore.getRequireDescriptions());
        assertEquals(0, archiveStore.getRequireDescriptions().size());
        assertFalse(archiveStore.isLazyActivationPolicy());
        assertFalse(archiveStore.isSingleton());

        Enumeration<URL> entries = archiveStore.findEntries("com/acme/pvt", "*.class", false, true);
        int count = 0;
        while (entries.hasMoreElements())
        {
            entries.nextElement();
            count++;
        }
        assertEquals(2, count);

        entries = archiveStore.findEntries("com", "*.class", false, true);
        count = 0;
        while (entries.hasMoreElements())
        {
            entries.nextElement();
            count++;
        }
        assertEquals(6, count);

        entries = archiveStore.findEntries("/", "*.class", false, true);
        count = 0;
        while (entries.hasMoreElements())
        {
            entries.nextElement();
            count++;
        }
        assertEquals(6, count);

        entries = archiveStore.findEntries("com/acme", "*", false, false);
        count = 0;
        while (entries.hasMoreElements())
        {
            entries.nextElement();
            count++;
        }
        assertEquals(1, count);

        entries = archiveStore.findEntries("", "*", false, true);
        count = 0;
        while (entries.hasMoreElements())
        {
            entries.nextElement();
            count++;
        }
        assertEquals(17, count);

        entries = archiveStore.findEntries("/", "*", true, true);
        count = 0;
        while (entries.hasMoreElements())
        {
            entries.nextElement();
            count++;
        }
        assertEquals(30, count);

        entries = archiveStore.findEntries("com/acme", "*", true, true);
        count = 0;
        while (entries.hasMoreElements())
        {
            entries.nextElement();
            count++;
        }
        assertEquals(13, count);

        assertTrue(compare(new FileInputStream(new File("./target/bundle.jar")),
                           archiveStore.getInputStreamForCodeSource()));

        assertEquals(22, count(archiveStore.getInputStreamForResource(0, "com/acme/resource/camera.xml")));
        assertEquals(22, count(archiveStore.getInputStreamForEntry("com/acme/resource/camera.xml")));
    }

    protected boolean compare(InputStream first, InputStream second)
    {
        try
        {
            int i1, i2;
            while (true)
            {
                i1 = first.read();
                i2 = second.read();

                if (i1 != i2) return false;
                if (i1 < 0) return true;
            }
        }
        catch (IOException e)
        {
            return false;
        }
    }

    protected int count(InputStream in)
    {
        int count = 0;

        try
        {
            while (in.read() >= 0) count++;
        }
        catch (IOException ignore)
        {
        }

        return count;
    }
}
