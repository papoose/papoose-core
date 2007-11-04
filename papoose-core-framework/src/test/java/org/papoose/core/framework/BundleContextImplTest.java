/**
 *  Copyright 2007 Picateers Inc., 1720 S. Amphlett Boulevard  Suite 320, San Mateo, CA 94402 U.S.A. All rights reserved.
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
import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.core.framework.spi.BundleStore;

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
            final int bundleId = 0;
            FileStore fileStore = new FileStore(fileStoreRoot);
            Papoose papoose = new Papoose("org.acme.osgi.0", fileStore, new MockThreadPool(), new Properties());
            File testBundle = new File("./target/bundle.jar");
            String location = testBundle.toURL().toURI().normalize().toString();

            BundleStore bundleStore = fileStore.allocateBundleStore(bundleId, testBundle.toURL().toString());
            ArchiveStore archiveStore = fileStore.allocateArchiveStore(papoose, bundleId, 0, testBundle.toURL().openStream());

            MockURLStreamHandlerFactory.add(archiveStore);

            archiveStore.refreshClassPath(archiveStore.getBundleClassPath());

            BundleImpl bootstrap = new BundleImpl(papoose, bundleId, location, bundleStore, archiveStore);
            BundleContextImpl context = new BundleContextImpl(bootstrap);

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
