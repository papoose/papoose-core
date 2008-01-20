/**
 *
 * Copyright 2007 (C) The original author or authors
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
package org.papoose.core.framework;

import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;


/**
 * @version $Revision$ $Date$
 */
class ServiceReferenceImpl implements ServiceReference
{
    private final Dictionary<String, Object> properties;
    private final Bundle bundle;
    private final Bundle[] usingBundles;

    public ServiceReferenceImpl(Dictionary<String, Object> properties, Bundle bundle, Bundle[] usingBundles)
    {
        this.properties = properties;
        this.bundle = bundle;
        this.usingBundles = usingBundles;
    }

    public Object getProperty(String key)
    {
        return properties.get(key);
    }

    public String[] getPropertyKeys()
    {
        String[] keys = new String[properties.size()];
        int pointer = 0;
        Enumeration<String> enumeration = properties.keys();
        while (enumeration.hasMoreElements()) keys[pointer++] = enumeration.nextElement();
        return keys;
    }

    public Bundle getBundle()
    {
        return bundle;
    }

    public Bundle[] getUsingBundles()
    {
        return usingBundles;
    }

    public boolean isAssignableTo(Bundle bundle, String string)
    {
        return false;  //todo: consider this autogenerated code
    }

    Dictionary<String, Object> getProperties()
    {
        return properties;
    }

    public int compareTo(Object o)
    {
        return 0;  //todo: consider this autogenerated code
    }
}
