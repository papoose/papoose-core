/**
 *
 * Copyright 2010 (C) The original author or authors
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
package org.papoose.tck.core;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import org.papoose.core.PapooseFrameworkFactory;


/**
 * @version $Revision$ $Date$
 */
public class PapooseFrameworkFactoryTest
{
    @Test
    public void test() throws Exception
    {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put(Constants.FRAMEWORK_STORAGE, "target/papoose");

        final FrameworkFactory factory = new PapooseFrameworkFactory();
        Framework framework = factory.newFramework(configuration);

        framework.init();

        Bundle systemBundle = framework.getBundleContext().getBundle(0);
        BundleContext context = systemBundle.getBundleContext();

        Bundle testBundle = context.installBundle("mvn:org.papoose.test.bundles/test-bundle/1.1.0");

        long testBundleId = testBundle.getBundleId();

        assertTrue(testBundleId > 0);

        try
        {
            framework.uninstall();
            fail("Should have thrown an exception");
        }
        catch (BundleException e)
        {
        }

        framework.stop();

        framework = factory.newFramework(configuration);
        framework.init();

        systemBundle = framework.getBundleContext().getBundle(0);
        context = systemBundle.getBundleContext();

        testBundle = context.getBundle(testBundleId);
        assertNotNull(testBundleId);

        framework.start();
        testBundle.start();

        assertEquals(Bundle.ACTIVE, framework.getState());
        assertEquals(Bundle.ACTIVE, testBundle.getState());

        testBundle.uninstall();

        Dictionary headers = testBundle.getHeaders("en");
        Assert.assertEquals("org.papoose.test.bundles.test-bundle", headers.get("Bundle-SymbOLicName"));

        headers = testBundle.getHeaders("en");
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = testBundle.getHeaders();
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = testBundle.getHeaders(null);
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = testBundle.getHeaders("en_US");
        Assert.assertEquals("bundle_en", headers.get("L10N-Bundle"));

        headers = testBundle.getHeaders("fr");
        Assert.assertEquals("bundle_fr", headers.get("L10N-Bundle"));

        headers = testBundle.getHeaders("fr_FR");
        Assert.assertEquals("bundle_fr_FR", headers.get("L10N-Bundle"));

        headers = testBundle.getHeaders("");
        Assert.assertEquals("%bundle", headers.get("L10N-Bundle"));

        headers = testBundle.getHeaders("en");
        Assert.assertEquals("no translation for this entry", headers.get("L10N-NoTranslation"));

        try
        {
            testBundle.start();
            fail("Cannot start an uninstalled bundle");
        }
        catch (IllegalStateException e)
        {
        }

        testBundle = context.getBundle(testBundleId);
        assertNull(testBundle);

        framework.stop();

        framework.init();

        systemBundle = framework.getBundleContext().getBundle(0);
        context = systemBundle.getBundleContext();

        testBundle = context.getBundle(testBundleId);
        assertNull(testBundle);

        framework.stop();
    }
}
