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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;


/**
 * @version $Revision: $ $Date: $
 */
public class BogusServiceReference implements ServiceReference
{
    public Object getProperty(String key)
    {
        throw new UnsupportedOperationException();
    }

    public String[] getPropertyKeys()
    {
        throw new UnsupportedOperationException();
    }

    public Bundle getBundle()
    {
        throw new UnsupportedOperationException();
    }

    public Bundle[] getUsingBundles()
    {
        throw new UnsupportedOperationException();
    }

    public boolean isAssignableTo(Bundle bundle, String className)
    {
        throw new UnsupportedOperationException();
    }

    public int compareTo(Object reference)
    {
        throw new UnsupportedOperationException();
    }
}
