/**
 *  Copyright 2007 Picateers Inc., 1720 S. Amphlett Boulevard  Suite 320, San Mateo, CA 94402 U.S.A. All rights reserved.
 */
package org.papoose.core.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Dictionary;
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
    public void test() throws Exception
    {
        File fileStoreRoot = new File("./target/store");
        try
        {
            final int bundleId = 1;
            FileStore fileStore = new FileStore(fileStoreRoot);
            Papoose papoose = new Papoose("org.acme.osgi.0", fileStore, new MockThreadPool(), new Properties());
            File testBundle = new File("./target/bundle.jar");

            BundleStore bundleStore = fileStore.allocateBundleStore(bundleId, testBundle.toURL().toString());
            ArchiveStore archiveStore = fileStore.allocateArchiveStore(papoose, bundleId, 0, testBundle.toURL().openStream());

            MockURLStreamHandlerFactory.add(archiveStore);

            archiveStore.refreshClassPath(archiveStore.getBundleClassPath());

            BundleImpl bundle = new BundleImpl(papoose, bundleId, bundleStore, archiveStore);
            BundleContextImpl context = new BundleContextImpl(bundle);

            Bundle b = context.installBundle(testBundle.toURL().toURI().normalize().toString());
            Dictionary headers = b.getHeaders("en");
            String symbolicName = (String) headers.get("Bundle-SymbOLicName");
            URL url = b.getEntry("com/acme/fuse/dynamite.xml");
//            BufferedReader in = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
//            String line = in.readLine();

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

        try
        {
            URL.setURLStreamHandlerFactory(new MockURLStreamHandlerFactory());
        }
        catch (Throwable t)
        {
        }
    }
}
