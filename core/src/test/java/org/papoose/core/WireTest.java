/**
 *
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.mock.MockArchiveStore;
import org.papoose.core.mock.MockBundleStore;
import org.papoose.core.mock.MockStore;


/**
 * @version $Revision$ $Date$
 */
public class WireTest
{
    private ExecutorService executorService;
    private Papoose mockFramework;

    @Test
    public void testMatch() throws Exception
    {
        Set<String> packages = new HashSet<String>();
        packages.add("com.acme");

        ExportDescription description = new ExportDescription(packages, Collections.<String, Object>emptyMap());

        BundleController mockBundleController = new BundleController(mockFramework, new MockBundleStore(1, "mock:location"));
        Wire test = new Wire("com.acme", description, new BundleGeneration(mockBundleController, new MockArchiveStore()));

        Assert.assertTrue(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertTrue(test.validFor("com/acme/Anvil.class"));

        description.setInclude(Collections.singletonList(new String[]{ "Dynamite.class" }));

        Assert.assertTrue(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/Anvil.class"));

        description.setInclude(Collections.singletonList(new String[]{ "Dynam", "" }));

        Assert.assertTrue(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/Anvil.class"));

        description.setInclude(Collections.singletonList(new String[]{ "", "nam", "" }));

        Assert.assertTrue(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/Anvil.class"));

        description.setInclude(Collections.<String[]>emptyList());
        description.setExclude(Collections.singletonList(new String[]{ "Dynamite.class" }));

        Assert.assertFalse(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertTrue(test.validFor("com/acme/Anvil.class"));

        description.setExclude(Collections.singletonList(new String[]{ "Dynam", "" }));

        Assert.assertFalse(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertTrue(test.validFor("com/acme/Anvil.class"));

        description.setExclude(Collections.singletonList(new String[]{ "", "nam", "" }));

        Assert.assertFalse(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertTrue(test.validFor("com/acme/Anvil.class"));

        description.setInclude(Collections.singletonList(new String[]{ "Dynamite" }));

        Assert.assertFalse(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/Anvil.class"));

        description.setInclude(Collections.singletonList(new String[]{ "Dynam", "" }));

        Assert.assertFalse(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/Anvil.class"));

        description.setInclude(Collections.singletonList(new String[]{ "", "nam", "" }));

        Assert.assertFalse(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops.Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/Anvil.class"));

        List<String[]> included = new ArrayList<String[]>();
        included.add(new String[]{ "", "nam", "" });
        included.add(new String[]{ "", "vi", "" });
        description.setInclude(included);

        Assert.assertFalse(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertTrue(test.validFor("com/acme/Anvil.class"));

        List<String[]> excluded = new ArrayList<String[]>();
        excluded.add(new String[]{ "", "nam", "" });
        excluded.add(new String[]{ "", "vi", "" });
        description.setExclude(excluded);

        Assert.assertFalse(test.validFor("com/acme/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/oops/Dynamite.class"));
        Assert.assertFalse(test.validFor("com/acme/Anvil.class"));
        Assert.assertFalse(test.validFor("com/acme/Spring.class"));
    }

    @Before
    public void setUp()
    {
        executorService = new ThreadPoolExecutor(5, 5, 60, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>());
        mockFramework = new Papoose(new MockStore(), executorService, new Properties());
    }

    @After
    public void tearDown()
    {
        executorService.shutdownNow();

        executorService = null;
        mockFramework = null;
    }
}
