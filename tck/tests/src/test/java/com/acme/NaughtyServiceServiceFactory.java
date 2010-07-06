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
package com.acme;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;


/**
 * @version $Revision: $ $Date: $
 */
public class NaughtyServiceServiceFactory implements ServiceFactory
{
    public final Map<String, Object> data = new HashMap<String, Object>();
    public final Map<Bundle, Tuple> tuples = new HashMap<Bundle, Tuple>();

    public Object getService(Bundle bundle, ServiceRegistration registration)
    {
        Object service = new NaughtyService();

        data.put("GET", registration);

        tuples.put(bundle, new Tuple(bundle, registration, service));

        return service;
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
    {
        data.put("UNGET", service);

        boolean test = false;
        Tuple tuple = tuples.remove(bundle);
        if (tuple != null)
        {
            test = (tuple.bundle == bundle);
            test = test && (tuple.registration == registration);
            test = test && (tuple.service == service);
        }
        data.put("TEST", test);
    }

    private static class Tuple
    {
        Bundle bundle;
        ServiceRegistration registration;
        Object service;

        private Tuple(Bundle bundle, ServiceRegistration registration, Object service)
        {
            this.bundle = bundle;
            this.registration = registration;
            this.service = service;
        }
    }
}