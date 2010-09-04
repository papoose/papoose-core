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
package com.acme.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import com.acme.svc.Service;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.papoose.test.bundles.share.Share;


/**
 * @version $Revision: $ $Date: $
 */
public class Activator implements BundleActivator
{
    private volatile ServiceRegistration registration;

    public void start(final BundleContext context) throws Exception
    {
        ServiceReference shareReference = context.getServiceReference(Share.class.getName());
        final Share share = (Share)context.getService(shareReference);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("SERVICE", "B");

        registration = context.registerService(Service.class.getName(), new Service()
        {
            public void hello(String message)
            {
                share.put("MSG", message);
            }

            @Override
            public String toString()
            {
                return "B service";
            }
        }, properties);
    }

    public void stop(BundleContext context) throws Exception
    {
        registration.unregister();
    }
}
