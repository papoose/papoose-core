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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.papoose;
import static org.ops4j.pax.exam.CoreOptions.provision;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.compendiumProfile;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


/**
 * @version $Revision: $ $Date: $
 */
@RunWith(JUnit4TestRunner.class)
public class FrameworkTest
{
    @Inject
    protected BundleContext context = null;

    @Configuration
    public static Option[] configure()
    {
        return options(
                // equinox(),
                // felix(),
                papoose(),
                compendiumProfile(),
                // org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption("-Xmx1024M -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005"),
                // this is necessary to let junit runner not timeout the remote process before attaching debugger
                // setting timeout to 0 means wait as long as the remote service comes available.
                // starting with version 0.5.0 of PAX Exam this is no longer required as by default the framework tests
                // will not be triggered till the framework is not started
                // waitForFrameworkStartupFor(Constants.WAIT_5_MINUTES),

                provision(
                        mavenBundle().groupId("org.papoose.test.bundles").artifactId("test-share").version("1.1.0"),
                        mavenBundle().groupId("org.papoose.core.tck.bundles").artifactId("service-a").version("1.0.0"),
                        mavenBundle().groupId("org.papoose.core.tck.bundles").artifactId("service-b").version("2.0.0"),
                        mavenBundle().groupId("org.papoose.core.tck.bundles").artifactId("service-consumer").version("1.0.0"),
                        mavenBundle().groupId("org.papoose.test.bundles").artifactId("test-bundle").version("1.1.0").noStart()
                )
        );
    }

    @Test
    public void testServices() throws Exception
    {
        Bundle bundleA = findBundle("org.papoose.core.tck.bundles.service-a");
        assertNotNull(bundleA);

        Bundle bundleB = findBundle("org.papoose.core.tck.bundles.service-b");
        assertNotNull(bundleB);

        Bundle consumer = findBundle("org.papoose.core.tck.bundles.service-consumer");
        assertNotNull(consumer);

        Bundle test = findBundle("org.papoose.test.bundles.test-bundle");
        assertNotNull(test);

        assertNull(test.getBundleContext());
        test.start();
        assertNotNull(test.getBundleContext());

        ServiceReference sreference = context.getBundle(0).getBundleContext().getServiceReference("com.acme.svc.Service");
        ServiceReference areference = bundleA.getBundleContext().getServiceReference("com.acme.svc.Service");
        ServiceReference breference = bundleB.getBundleContext().getServiceReference("com.acme.svc.Service");
        ServiceReference creference = consumer.getBundleContext().getServiceReference("com.acme.svc.Service");
        ServiceReference treference = test.getBundleContext().getServiceReference("com.acme.svc.Service");

        assertNotNull(sreference);
        assertNotNull(areference);
        assertNotNull(breference);
        assertNotNull(creference);
        assertNotNull(treference);

        assertEquals(areference, sreference);
        assertEquals(breference, creference);

        ServiceReference[] references = context.getBundle(0).getBundleContext().getAllServiceReferences("com.acme.svc.Service", null);

        assertEquals(2, references.length);

        references = context.getBundle(0).getBundleContext().getServiceReferences("com.acme.svc.Service", null);

        assertEquals(2, references.length);

        references = bundleA.getBundleContext().getServiceReferences("com.acme.svc.Service", null);

        assertEquals(1, references.length);

        references = bundleB.getBundleContext().getServiceReferences("com.acme.svc.Service", null);

        assertEquals(1, references.length);

        Object sservice = context.getService(sreference);
        Object aservice = bundleA.getBundleContext().getService(areference);
        Object bservice = bundleB.getBundleContext().getService(breference);
        Object cservice = consumer.getBundleContext().getService(creference);

        assertNotNull(sservice);
        assertNotNull(aservice);
        assertNotNull(bservice);
        assertNotNull(cservice);
        assertNotSame(aservice, bservice);
    }

    private Bundle findBundle(String name)
    {
        for (Bundle bundle : context.getBundles())
        {
            if (bundle.getSymbolicName().equals(name)) return bundle;
        }
        return null;
    }
}
