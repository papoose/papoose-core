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

import net.jcip.annotations.ThreadSafe;

import org.papoose.core.Generation;
import org.papoose.core.util.ToStringCreator;

/**
 * Base wrapper for bundle generations as the resolver searches for a
 * compatible solution in the bundle space.  Extensions of this class will
 * contain the necessary intermediate data to assist in this search and will
 * contain the information needed to assemble resolved bundles and their class
 * loaders if a consistent solution set is found.
 *
 * @version $Revision$ $Date$
 */
@ThreadSafe
public class Candidate
{
    /**
     * The generation that is to be used to identify this instance of
     * candidate.  This could either be a resolved host, a host bound to its
     * fragments that needs to be resolved, or a bundle or a bundle fragment
     * that is unresolved but not yet bound.
     */
    private final Generation generation;

    public Candidate(Generation generation)
    {
        assert generation != null;

        this.generation = generation;
    }

    /**
     * TODO: Not sure if code should have access to this
     * @return the generation that is to be used to identify this instance
     */
    public Generation getGeneration()
    {
        return generation;
    }

    /**
     * All candidates refer to a specific bundle generation and so no two
     * candidates are equal unless the bundle that they wrap are equal.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    @Override
    public final boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof Candidate)) return false;

        Candidate candidate = (Candidate) obj;

        return generation.equals(candidate.generation);
    }

    /**
     * All candidates refer to a specific bundle generation and so no two
     * candidates are equal unless the bundle that they wrap are equal.
     * Therefore we override the <code>hashCode()</code> method to accomplish
     * these special semantics.
     *
     * @return a hash code value for this object.
     */
    @Override
    public final int hashCode()
    {
        return generation.hashCode();
    }


    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("symbolicName", getGeneration().getSymbolicName());
        creator.append("version", getGeneration().getVersion());
        creator.append("generation", getGeneration().getGeneration());

        return creator.toString();
    }
}
