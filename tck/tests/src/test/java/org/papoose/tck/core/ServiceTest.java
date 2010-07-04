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

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import com.acme.Service;
import com.acme.ServiceServiceFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
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
    public void testRegistrationWithProperties()
    {
        BundleContext context = framework.getBundleContext();

        Properties properties = new Properties();
        properties.put("TEST", "PASS");
        final AtomicReference<String> storedMessage = new AtomicReference<String>();
        ServiceRegistration registration = context.registerService(Service.class.getName(), new Service()
        {
            public void hello(String message)
            {
                storedMessage.set(message);
            }
        }, properties);

        assertNotNull(registration);

        ServiceReference reference = context.getServiceReference(Service.class.getName());

        assertNotNull(reference);
        assertEquals("PASS", reference.getProperty("TEST"));
        assertTrue((Long) reference.getProperty(Constants.SERVICE_ID) > 0);
        assertEquals(0, reference.getProperty(Constants.SERVICE_RANKING));
        assertEquals(Service.class.getName(), ((String[]) reference.getProperty(Constants.OBJECTCLASS))[0]);

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

    @Test
    public void testRegistrationsWithServiceRanking()
    {
        BundleContext context = framework.getBundleContext();

        Properties properties = new Properties();
        properties.put(Constants.SERVICE_RANKING, 1);
        final AtomicReference<String> storedMessage1 = new AtomicReference<String>();
        ServiceRegistration registration1 = context.registerService(Service.class.getName(), new Service()
        {
            public void hello(String message)
            {
                storedMessage1.set(message);
            }
        }, properties);

        assertNotNull(registration1);

        properties.put(Constants.SERVICE_RANKING, 2);
        final AtomicReference<String> storedMessage2 = new AtomicReference<String>();
        ServiceRegistration registration2 = context.registerService(Service.class.getName(), new Service()
        {
            public void hello(String message)
            {
                storedMessage2.set(message);
            }
        }, properties);

        assertNotNull(registration2);

        ServiceReference reference = context.getServiceReference(Service.class.getName());

        assertNotNull(reference);

        Service service = (Service) context.getService(reference);

        assertNotNull(service);

        service.hello("Hello World!");

        assertEquals("Hello World!", storedMessage2.get());
        assertNull(storedMessage1.get());

        registration2.unregister();

        reference = context.getServiceReference(Service.class.getName());

        assertNotNull(reference);

        service = (Service) context.getService(reference);

        assertNotNull(service);

        service.hello("Hello World!");

        assertEquals("Hello World!", storedMessage1.get());

        registration1.unregister();

        reference = context.getServiceReference(Service.class.getName());

        assertNull(reference);
    }

    @Test
    public void testServiceFactory()
    {
        BundleContext context = framework.getBundleContext();

        ServiceServiceFactory serviceFactory = new ServiceServiceFactory();
        ServiceRegistration registration = context.registerService(Service.class.getName(), serviceFactory, null);

        assertNotNull(registration);

        ServiceReference reference = context.getServiceReference(Service.class.getName());

        assertNotNull(reference);

        Service service = (Service) context.getService(reference);

        assertNotNull(service);

        service.hello("Hello World!");

        assertEquals("Hello World!", serviceFactory.data.get("MESSAGE"));

        registration.unregister();

        assertSame(service, serviceFactory.data.get("UNGET"));

        reference = context.getServiceReference(Service.class.getName());

        assertNull(reference);
    }

    @Test
    public void testServiceFactoryUnGet() throws Exception
    {
        BundleContext context = framework.getBundleContext();

        ServiceServiceFactory serviceFactory = new ServiceServiceFactory();
        ServiceRegistration registration = context.registerService(Service.class.getName(), serviceFactory, null);

        assertNotNull(registration);

        Bundle testBundle = context.installBundle("mvn:org.papoose.test.bundles/test-bundle/1.1.0");

        ServiceReference sreference = context.getServiceReference(Service.class.getName());
        ServiceReference treference = testBundle.getBundleContext().getServiceReference(Service.class.getName());

        assertNotNull(sreference);
        assertNotNull(treference);

        assertSame(context.getBundle(), sreference.getBundle());
        assertSame(context.getBundle(), treference.getBundle());

        Service sservice = (Service) context.getService(sreference);
        Service tservice = (Service) testBundle.getBundleContext().getService(treference);

        assertNotNull(sservice);
        assertNotNull(tservice);

        boolean sfound = false;
        boolean tfound = false;
        for (Bundle bundle : sreference.getUsingBundles())
        {
            if (context.getBundle() == bundle) sfound = true;
            if (testBundle == bundle) tfound = true;
        }
        assertTrue("System bundle is using", sfound);
        assertTrue("Test bundle is using", tfound);

        sfound = false;
        tfound = false;
        for (Bundle bundle : treference.getUsingBundles())
        {
            if (context.getBundle() == bundle) sfound = true;
            if (testBundle == bundle) tfound = true;
        }
        assertTrue("System bundle is using", sfound);
        assertTrue("Test bundle is using", tfound);

        tservice.hello("Hello World!");

        assertEquals("Hello World!", serviceFactory.data.get("MESSAGE"));

        testBundle.uninstall();

        assertSame(tservice, serviceFactory.data.get("UNGET"));
        assertTrue((Boolean) serviceFactory.data.get("TEST"));

        serviceFactory.data.remove("UNGET");
        registration.unregister();

        assertSame(sservice, serviceFactory.data.get("UNGET"));
        assertTrue((Boolean) serviceFactory.data.get("TEST"));

        sreference = context.getServiceReference(Service.class.getName());

        assertNull(sreference);
    }

    @Test
    public void testMultiGetServiceFactory()
    {
        BundleContext context = framework.getBundleContext();

        ServiceServiceFactory serviceFactory = new ServiceServiceFactory();
        ServiceRegistration registration = context.registerService(Service.class.getName(), serviceFactory, null);

        assertNotNull(registration);

        ServiceReference reference = context.getServiceReference(Service.class.getName());

        assertNotNull(reference);

        context.getService(reference);
        context.getService(reference);
        Service service = (Service) context.getService(reference);

        assertNotNull(service);

        assertNotNull(reference.getUsingBundles());

        service.hello("Hello World!");

        assertEquals("Hello World!", serviceFactory.data.get("MESSAGE"));

        assertTrue(context.ungetService(reference));
        assertSame(null, serviceFactory.data.get("UNGET"));
        assertTrue(context.ungetService(reference));
        assertSame(null, serviceFactory.data.get("UNGET"));
        assertTrue(context.ungetService(reference));
        assertSame(service, serviceFactory.data.get("UNGET"));
        assertFalse(context.ungetService(reference));

        serviceFactory.data.remove("UNGET");

        registration.unregister();

        assertSame(null, serviceFactory.data.get("UNGET"));

        reference = context.getServiceReference(Service.class.getName());

        assertNull(reference);
    }

    @Test
    public void testServiceListener()
    {
        BundleContext context = framework.getBundleContext();

        final AtomicReference<ServiceEvent> storedEvent = new AtomicReference<ServiceEvent>();
        final AtomicReference<String> storedTest = new AtomicReference<String>();
        context.addServiceListener(new ServiceListener()
        {
            public void serviceChanged(ServiceEvent event)
            {
                storedEvent.set(event);
                storedTest.set((String) event.getServiceReference().getProperty("TEST"));
            }
        });

        Properties properties = new Properties();
        properties.put("TEST", "PASS");
        final AtomicReference<String> storedMessage = new AtomicReference<String>();
        ServiceRegistration registration = context.registerService(Service.class.getName(), new Service()
        {
            public void hello(String message)
            {
                storedMessage.set(message);
            }
        }, properties);

        assertNotNull(registration);
        assertEquals(ServiceEvent.REGISTERED, storedEvent.get().getType());
        assertEquals("PASS", storedTest.get());

        properties.put("TEST", "MODIFIED");
        registration.setProperties(properties);

        assertEquals(ServiceEvent.MODIFIED, storedEvent.get().getType());
        assertEquals("MODIFIED", storedTest.get());

        registration.unregister();

        assertEquals(ServiceEvent.UNREGISTERING, storedEvent.get().getType());
    }

    @Test
    public void testServiceListenerWithFilter() throws Exception
    {
        BundleContext context = framework.getBundleContext();

        final AtomicReference<ServiceEvent> storedEvent = new AtomicReference<ServiceEvent>();
        final AtomicReference<String> storedTest = new AtomicReference<String>();
        context.addServiceListener(new ServiceListener()
        {
            public void serviceChanged(ServiceEvent event)
            {
                storedEvent.set(event);
                storedTest.set((String) event.getServiceReference().getProperty("TE ST"));
            }
        }, "( Te St=*)");

        final AtomicReference<Boolean> noEvent = new AtomicReference<Boolean>(true);
        context.addServiceListener(new ServiceListener()
        {
            public void serviceChanged(ServiceEvent event)
            {
                noEvent.set(false);
            }
        }, "(NO-MATCH=*)");

        Properties properties = new Properties();
        properties.put("TE ST", "PASS");
        final AtomicReference<String> storedMessage = new AtomicReference<String>();
        ServiceRegistration registration = context.registerService(Service.class.getName(), new Service()
        {
            public void hello(String message)
            {
                storedMessage.set(message);
            }
        }, properties);


        assertNotNull(registration);
        assertEquals(ServiceEvent.REGISTERED, storedEvent.get().getType());
        assertEquals("PASS", storedTest.get());
        assertTrue(noEvent.get());

        properties.put("TE ST", "MODIFIED");
        registration.setProperties(properties);

        assertEquals(ServiceEvent.MODIFIED, storedEvent.get().getType());
        assertEquals("MODIFIED", storedTest.get());

        properties.clear();
        properties.put("TEST", "END");
        registration.setProperties(properties);

        assertEquals(ServiceEvent.MODIFIED_ENDMATCH, storedEvent.get().getType());
        assertNull(storedTest.get());

        registration.unregister();
    }

}
