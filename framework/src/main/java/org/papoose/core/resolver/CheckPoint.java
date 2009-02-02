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
package org.papoose.core.resolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.papoose.core.Generation;
import org.papoose.core.util.ToStringCreator;

/**
 * @version $Revision$ $Date$
 */
class CheckPoint
{
    private CandidateBundle resolving;
    private final List<CandidateBundle> resolved = new ArrayList<CandidateBundle>();
    private final List<CandidateBundle> unResolved = new ArrayList<CandidateBundle>();
    private final Set<CandidateBundle> used = new HashSet<CandidateBundle>();
    private final Set<Generation> unused = new HashSet<Generation>();


    CheckPoint(CandidateBundle resolving, Set<Generation> unused)
    {
        this.resolving = resolving;
        this.used.add(resolving);
        this.unused.addAll(unused);
    }

    CheckPoint(CheckPoint checkPoint)
    {
        resolving = new CandidateBundle(checkPoint.resolving);
        unResolved.addAll(checkPoint.unResolved);
        used.addAll(checkPoint.used);
        unused.addAll(checkPoint.unused);
    }

    public CandidateBundle getResolving()
    {
        return resolving;
    }

    public List<CandidateBundle> getResolved()
    {
        return resolved;
    }

    public Set<CandidateBundle> getUsed()
    {
        return used;
    }

    public Set<Generation> getUnused()
    {
        return unused;
    }

    public boolean isDone()
    {
        return unResolved.isEmpty();
    }

    public void resolveCompleted()
    {
        resolved.add(resolving);
        resolving = null;
    }

    public CheckPoint nextBundle()
    {
        resolving = unResolved.get(0);
        return this;
    }

    public CheckPoint newCheckPoint(CandidateRequiredBundle candidate)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        checkPoint.resolving.getCandidateRequiredBundles().add(candidate);

        return checkPoint;
    }

    public CheckPoint newCheckPoint(CandidateWiring newWiring)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        checkPoint.resolving.getCandidateWirings().add(newWiring);

        return checkPoint;
    }

    public CheckPoint newCheckPoint(CandidateBundle newBundle, CandidateWiring newWiring)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        checkPoint.resolving.getCandidateWirings().add(newWiring);
        checkPoint.used.add(newBundle);
        checkPoint.unused.remove(newBundle.getBundleGeneration());

        return checkPoint;
    }

    @Override
    public String toString()
    {
        ToStringCreator creator = new ToStringCreator(this);

        creator.append("resolving", resolving);
        creator.append("resolved", resolved);
        creator.append("unResolved", unResolved);
        creator.append("used", used);
        creator.append("unused", unused);

        return creator.toString();
    }
}
