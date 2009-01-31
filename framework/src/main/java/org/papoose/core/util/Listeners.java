/**
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
package org.papoose.core.util;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * @version $Rev$ $Date$
 */
public class Listeners<L, E>
{
    private final Set<L> listeners = new CopyOnWriteArraySet<L>();
    private final Functor<L, E> functor;

    public Listeners(Functor<L, E> functor)
    {
        this.functor = functor;
    }

    public void addListener(L listener)
    {
        if (listeners.contains(listener)) return;
        listeners.add(listener);
    }

    public void removeListener(L listener)
    {
        listeners.remove(listener);
    }

    public void clear()
    {
        listeners.clear();
    }

    public void fireEvent(E event)
    {
        for (L listener : listeners) functor.fire(listener, event);
    }

    public interface Functor<T, E>
    {
        public void fire(T listener, E event);
    }
}
