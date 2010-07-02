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

import java.util.concurrent.atomic.AtomicReference;

import com.acme.Service;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * @version $Revision: $ $Date: $
 */
public class ServiceTest extends BaseTest
{
    @Test
    public void testRegistration()
    {
        BundleContext context = framework.getBundleContext();

        final AtomicReference<String> storedMessage = new AtomicReference<String>();
        ServiceRegistration registration = context.registerService(Service.class.getName(), new Service()
        {
            public void hello(String message)
            {
                storedMessage.set(message);
            }
        }, null);

        assertNotNull(registration);

        ServiceReference reference = context.getServiceReference(Service.class.getName());

        assertNotNull(reference);

        Service service = (Service) context.getService(reference);

        assertNotNull(service);

        service.hello("Hello World!");

        assertEquals("Hello World!", storedMessage.get());

        registration.unregister();

        reference = context.getServiceReference(Service.class.getName());

        assertNull(reference);
    }

    @Test
    public void testRegistrations()
    {
        BundleContext context = framework.getBundleContext();

        final AtomicReference<String> storedMessage1 = new AtomicReference<String>();
        ServiceRegistration registration1 = context.registerService(Service.class.getName(), new Service()
        {
            public void hello(String message)
            {
                storedMessage1.set(message);
            }
        }, null);

        assertNotNull(registration1);

        final AtomicReference<String> storedMessage2 = new AtomicReference<String>();
        ServiceRegistration registration2 = context.registerService(Service.class.getName(), new Service()
        {
            public void hello(String message)
            {
                storedMessage2.set(message);
            }
        }, null);

        assertNotNull(registration2);

        ServiceReference reference = context.getServiceReference(Service.class.getName());

        assertNotNull(reference);

        Service service = (Service) context.getService(reference);

        assertNotNull(service);

        service.hello("Hello World!");

        assertEquals("Hello World!", storedMessage1.get());
        assertNull(storedMessage2.get());

        registration1.unregister();

        reference = context.getServiceReference(Service.class.getName());

        assertNotNull(reference);

        service = (Service) context.getService(reference);

        assertNotNull(service);

        service.hello("Hello World!");

        assertEquals("Hello World!", storedMessage2.get());

        registration2.unregister();

        reference = context.getServiceReference(Service.class.getName());

        assertNull(reference);
    }
}
