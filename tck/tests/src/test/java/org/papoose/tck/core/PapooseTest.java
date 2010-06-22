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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import org.papoose.core.PapooseFrameworkFactory;
import org.papoose.core.util.FileUtils;


/**
 * @version $Revision: $ $Date: $
 */
public class PapooseTest
{
    private Framework framework;

    @Test
    public void testNoSymbolicName()
    {
        try
        {
            framework.getBundleContext().installBundle("mvn:org.papoose.core.tck.bundles/no-symbolic-name");
            fail("Should have failed loading bundle with missing symbolic name");
        }
        catch (BundleException e)
        {
        }
    }

    @Test
    public void testBadActivationPolicy()
    {
        try
        {
            framework.getBundleContext().installBundle("mvn:org.papoose.core.tck.bundles/bad-activation-policy");
            fail("Should have failed loading bundle with bad activation policy");
        }
        catch (BundleException e)
        {
        }
    }

    @Before
    public void before() throws Exception
    {
        Map<String, String> configuration = new HashMap<String, String>();
        configuration.put(Constants.FRAMEWORK_STORAGE, "target/papoose");

        FrameworkFactory factory = new PapooseFrameworkFactory();
        framework = factory.newFramework(configuration);

        framework.init();
    }

    @After
    public void after() throws Exception
    {
        framework.stop();
        framework = null;

        FileUtils.delete(new File("target/papoose"));
    }
}
