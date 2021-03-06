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

import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import org.papoose.core.filter.Expr;


/**
 *
 */
public class DefaultFilter implements Filter
{
    public static final Filter TRUE = new Filter()
    {
        public boolean match(ServiceReference serviceReference)
        {
            return true;
        }

        public boolean match(Dictionary dictionary)
        {
            return true;
        }

        public boolean matchCase(Dictionary dictionary)
        {
            return true;
        }
    };
    public static final Filter FALSE = new Filter()
    {
        public boolean match(ServiceReference serviceReference)
        {
            return false;
        }

        public boolean match(Dictionary dictionary)
        {
            return false;
        }

        public boolean matchCase(Dictionary dictionary)
        {
            return false;
        }
    };
    private final Expr expr;

    public DefaultFilter(Expr expr)
    {
        assert expr != null;

        this.expr = expr;
    }

    public boolean match(ServiceReference serviceReference)
    {
        return match(((ServiceRegistrationImpl.ServiceReferenceImpl) serviceReference).getProperties());
    }

    public boolean match(final Dictionary dictionary)
    {
        Dictionary<String, Object> caseInsensitive = new Dictionary<String, Object>()
        {
            final Map<String, Object> map = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

            {
                for (Enumeration enumeration = dictionary.keys(); enumeration.hasMoreElements();)
                {
                    Object key = enumeration.nextElement();
                    map.put(key.toString(), dictionary.get(key));
                }
            }

            public int size()
            {
                return map.size();
            }

            public boolean isEmpty()
            {
                return map.isEmpty();
            }

            public Enumeration<String> keys()
            {
                return Collections.enumeration(map.keySet());
            }

            public Enumeration<Object> elements()
            {
                return Collections.enumeration(map.values());
            }

            @SuppressWarnings({ "SuspiciousMethodCalls" })
            public Object get(Object key)
            {
                return map.get(key);
            }

            public Object put(String key, Object value)
            {
                throw new UnsupportedOperationException();
            }

            public Object remove(Object key)
            {
                throw new UnsupportedOperationException();
            }
        };

        return expr.match(caseInsensitive);
    }

    @SuppressWarnings({ "unchecked" })
    public boolean matchCase(Dictionary dictionary)
    {
        return expr.match(dictionary);
    }

    @Override
    public String toString()
    {
        return expr.toString();
    }
}
