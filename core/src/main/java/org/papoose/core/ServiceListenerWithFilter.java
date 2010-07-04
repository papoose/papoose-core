package org.papoose.core;

/**
 *
 * Copyright 2007-2010 (C) The original author or authors
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

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;


/**
 */
public class ServiceListenerWithFilter implements AllServiceListener
{
    private final ServiceListener delegate;
    private final Filter filter;

    ServiceListenerWithFilter(ServiceListener delegate)
    {
        this(delegate, DefaultFilter.TRUE);
    }

    ServiceListenerWithFilter(ServiceListener delegate, Filter filter)
    {
        assert delegate != null;
        assert filter != null;

        this.delegate = delegate;
        this.filter = filter;
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void serviceChanged(ServiceEvent event)
    {
        delegate.serviceChanged(event);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceListenerWithFilter that = (ServiceListenerWithFilter) o;

        return delegate == that.delegate;
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }
}
