/**
 *
 * Copyright 2009 (C) The original author or authors
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

import org.osgi.service.url.URLStreamHandlerService;


/**
 *
 */
class UrlStreamHandlerHolder implements Comparable<UrlStreamHandlerHolder>
{
    private final URLStreamHandlerService service;
    private final Integer rank;

    UrlStreamHandlerHolder(URLStreamHandlerService service)
    {
        this(service, Integer.MIN_VALUE);
    }

    UrlStreamHandlerHolder(URLStreamHandlerService service, int rank)
    {
        assert service != null;

        this.service = service;
        this.rank = rank;
    }

    public URLStreamHandlerService getService()
    {
        return service;
    }

    public int compareTo(UrlStreamHandlerHolder holder)
    {
        if (service instanceof URLStreamHandlerNotFound) return -1;
        return rank.compareTo(holder.rank);
    }
}
