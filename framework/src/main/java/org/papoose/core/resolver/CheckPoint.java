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

import org.osgi.framework.Bundle;

import org.papoose.core.Generation;
import org.papoose.core.util.ToStringCreator;


/**
 * @version $Revision$ $Date$
 */
public class CheckPoint
{
    private UnResolved resolving;
    private final List<BoundHost> resolved = new ArrayList<BoundHost>();
    private final List<UnResolved> unResolved = new ArrayList<UnResolved>();
    private final Set<Candidate> used = new HashSet<Candidate>();
    private final Set<Candidate> unused = new HashSet<Candidate>();


    public CheckPoint(Generation toBeResolved, Set<Candidate> canonicalSet)
    {
        unused.addAll(canonicalSet);

        resolving = new UnBound(toBeResolved);
        used.add(resolving);
        unused.remove(toBeResolved);
    }

    CheckPoint(BoundHost resolving, Set<Candidate> unused)
    {
        this.resolving = resolving;
        this.used.add(resolving);
        this.unused.addAll(unused);
    }

    CheckPoint(CheckPoint checkPoint)
    {
        try
        {
            resolving = (UnResolved) checkPoint.resolving.clone();
            unResolved.addAll(checkPoint.unResolved);
            used.addAll(checkPoint.used);
            unused.addAll(checkPoint.unused);
        }
        catch (CloneNotSupportedException notPossible)
        {
        }
    }

    public CheckPoint(Resolved resolving, Set<Candidate> unused)
    {
        this.resolving = null;
        this.used.add(resolving);
        this.unused.addAll(unused);
    }

    public UnResolved getResolving()
    {
        return resolving;
    }

    public List<BoundHost> getResolved()
    {
        return resolved;
    }

    public Set<Candidate> getUsed()
    {
        return used;
    }

    public Set<Candidate> getUnused()
    {
        return unused;
    }

    public boolean isDone()
    {
        return unResolved.isEmpty();
    }

    public void resolveCompleted()
    {
        resolved.add((BoundHost) resolving);
        resolving = null;
    }

    public CheckPoint nextBundle()
    {
        resolving = unResolved.get(0);
        return this;
    }

    /**
     * Create a new checkpoint by replacing the currently resolving candidate
     * with the new one that is being passed.  This is used by methods that are
     * searching for variants of a candidate, e.g. looking for a good set of
     * fragments to link with a host.
     *
     * @param boundHost the replacing candidate bundle
     * @return a new checkpoint w/ the new canidate bundle
     */
    public CheckPoint newCheckPoint(BoundHost boundHost)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        UnResolved oldCandidate = checkPoint.getResolving();

        assert oldCandidate instanceof UnBound;

        checkPoint.resolving = boundHost;

        checkPoint.getUsed().remove(oldCandidate);
        checkPoint.getUsed().add(boundHost);

        for (Candidate candidate : boundHost.getFragments())
        {
            if (!checkPoint.getUnused().contains(candidate))
            {
                checkPoint.getUsed().add(candidate);
                checkPoint.getUnused().remove(candidate);
            }
        }

        return checkPoint;
    }

    public CheckPoint newCheckPoint(RequiredBundleWrapper candidate)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        ((BoundHost) checkPoint.resolving).getCandidateRequiredBundles().add(candidate);

        return checkPoint;
    }

    public CheckPoint newCheckPoint(CandidateWiring newWiring)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        ((BoundHost) checkPoint.resolving).getCandidateWirings().add(newWiring);

        return checkPoint;
    }

    public CheckPoint newCheckPoint(BoundHost newBundle, CandidateWiring newWiring)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        ((BoundHost) checkPoint.resolving).getCandidateWirings().add(newWiring);

        if (newBundle.getBundleGeneration().getState() == Bundle.INSTALLED) unResolved.add(newBundle);

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
