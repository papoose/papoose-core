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

import java.net.ContentHandler;


/**
 *
 */
class ContentHandlerHolder implements Comparable<ContentHandlerHolder>
{
    private final ContentHandler handler;
    private final Integer rank;

    ContentHandlerHolder(ContentHandler handler)
    {
        this(handler, Integer.MIN_VALUE);
    }

    ContentHandlerHolder(ContentHandler handler, int rank)
    {
        this.handler = handler;
        this.rank = rank;
    }

    public ContentHandler getHandler()
    {
        return handler;
    }

    public int compareTo(ContentHandlerHolder holder)
    {
        if (handler instanceof ContentHandlerNotFound) return -1;
        return rank.compareTo(holder.rank);
    }
}
