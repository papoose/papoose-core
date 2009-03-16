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
package org.papoose.core.resolver;

import net.jcip.annotations.Immutable;

import org.papoose.core.Generation;

/**
 * @version $Revision$ $Date$
 */
@Immutable
public abstract class UnResolved extends Candidate implements Cloneable
{
    private final Generation toBeResolved;

    public UnResolved(Generation toBeResolved)
    {
        assert toBeResolved != null;

        this.toBeResolved = toBeResolved;
    }

    public Generation getToBeResolved()
    {
        return toBeResolved;
    }

    @Override
    public abstract Object clone() throws CloneNotSupportedException;

    /**
     * This slightly odd equals implementation allows various instances of
     * subclasses of <code>UnResolved</code> to all be identified by the
     * unresolved instance that is being wrapped.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    @Override
    @SuppressWarnings({ "EqualsWhichDoesntCheckParameterClass" })
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!UnResolved.class.isInstance(obj)) return false;

        UnResolved that = (UnResolved) obj;

        return toBeResolved.equals(that.toBeResolved);
    }

    @Override
    public int hashCode()
    {
        return toBeResolved.hashCode();
    }
}
