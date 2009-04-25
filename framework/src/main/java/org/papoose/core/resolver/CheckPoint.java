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

import org.papoose.core.BundleClassLoader;
import org.papoose.core.BundleGeneration;
import org.papoose.core.FragmentGeneration;
import org.papoose.core.Generation;
import org.papoose.core.Wire;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.descriptions.RequireDescription;
import org.papoose.core.util.ToStringCreator;


/**
 * @version $Revision$ $Date$
 */
public class CheckPoint
{
    private BoundHost resolving;
    private final List<BoundHost> resolved = new ArrayList<BoundHost>();
    private final List<UnBound> unResolved = new ArrayList<UnBound>();

    private final Set<CandidateBundle> used = new HashSet<CandidateBundle>();
    private final Set<Candidate> unused = new HashSet<Candidate>();


    public CheckPoint(Generation generation, Set<Candidate> canonicalSet)
    {
        assert generation != null;
        assert canonicalSet != null;

        unResolved.add(new UnBound(generation));
        unused.addAll(canonicalSet);
    }

    public CheckPoint(BundleGeneration bundleGeneration, ImportDescription importDescription, Set<Candidate> canonicalSet)
    {
        assert bundleGeneration != null;
        assert importDescription != null;
        assert canonicalSet != null;

        unused.addAll(canonicalSet);

        try
        {
            resolving = new BoundHost(bundleGeneration, importDescription);

            BundleClassLoader bundleClassLoader = bundleGeneration.getClassLoader();
            for (Wire wire : bundleClassLoader.getWires())
            {
                Candidate candidateBundle = findCandidate(wire.getBundleGeneration());
                ExportDescription exportDescription = wire.getExportDescription();

                for (String packageName : exportDescription.getPackageNames())
                {
                    resolving.addWiring(new CandidateWiring(packageName, exportDescription, candidateBundle));
                }
            }
        }
        catch (IncompatibleException neverHappens)
        {
            assert false;
        }

        used.add(resolving);

        unused.remove(resolving);
        for (FragmentGeneration fragmentGeneration : resolving.getFragments()) unused.remove(new Candidate(fragmentGeneration));
    }

    private Candidate findCandidate(BundleGeneration bundleGeneration)
    {
        for (Candidate candidate : unused) if (candidate.getGeneration() == bundleGeneration) return candidate;

        assert false;

        return null;
    }

    CheckPoint(CheckPoint checkPoint)
    {
        resolved.addAll(checkPoint.resolved);
        unResolved.addAll(checkPoint.unResolved);
        used.addAll(checkPoint.used);
        unused.addAll(checkPoint.unused);

        if (checkPoint.resolving != null)
        {
            resolving = new BoundHost(checkPoint.resolving);
            assert used.remove(resolving);
            assert used.add(resolving);
            for (CandidateBundle candidateBundle : used)
            {
                Set<CandidateWiring> replace = new HashSet<CandidateWiring>();

                for (CandidateWiring wiring : candidateBundle.getWirings())
                {
                    if (wiring.getCandidate().equals(resolving))
                    {
                        replace.add(new CandidateWiring(wiring, resolving));
                    }
                }

                for (CandidateWiring wiring : replace)
                {
                    candidateBundle.replaceCandidateWiring(wiring);
                }
            }
        }
    }

    public BoundHost getResolving()
    {
        return resolving;
    }

    public List<BoundHost> getResolved()
    {
        return resolved;
    }

    public List<UnBound> getUnResolved()
    {
        return unResolved;
    }

    public Set<CandidateBundle> getUsed()
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

    public CheckPoint nextBundle()
    {
        UnBound next = unResolved.get(0);
        //TODO NOT DONE HERE!!
//        resolving = new BoundHost(next.)

        return this;
    }

    public void resolveCompleted()
    {
        if (resolving != null)
        {
            resolved.add(resolving);
            resolving = null;
        }
    }

    public CheckPoint newCheckPoint(Resolved resolvedHost, RequireDescription requiredBundle)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        return checkPoint;  //Todo: change body of created methods use File | Settings | File Templates.
    }

    public CheckPoint newCheckPoint(BoundHost bound, RequireDescription requiredBundle)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        return checkPoint;  //Todo: change body of created methods use File | Settings | File Templates.
    }

    public CheckPoint newCheckPoint(UnBound unBound, RequireDescription requiredBundle)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        return checkPoint;  //Todo: change body of created methods use File | Settings | File Templates.
    }

    // add a wire to the bundle being resolved
    public CheckPoint newCheckPoint(CandidateWiring candidateWiring)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        assert checkPoint.resolving.addCandidateWiring(candidateWiring);

        return checkPoint;
    }

    // add a wire to the bundle being resolved
    // move resolved to used
    public CheckPoint newCheckPoint(CandidateWiring candidateWiring, Resolved resolved)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        assert checkPoint.resolving.addCandidateWiring(candidateWiring);
        assert checkPoint.used.add(resolved);
        assert checkPoint.unused.remove(resolved);

        return checkPoint;
    }

    // add a wire to the bundle being resolved
    // move the unbound to to be resolved
    // add the unbound to used
    public CheckPoint newCheckPoint(CandidateWiring candidateWiring, UnBound unBound)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        assert checkPoint.resolving.addCandidateWiring(candidateWiring);
        assert checkPoint.unResolved.add(unBound);
        assert checkPoint.unused.remove(unBound);

        return checkPoint;
    }

    public CheckPoint newCheckPoint(BundleGeneration host, List<FragmentGeneration> fragments) throws IncompatibleException
    {
        CheckPoint checkPoint = new CheckPoint(this);

        assert host != null;
        assert fragments != null;

        if (checkPoint.resolving != null)
        {
            checkPoint.resolved.add(checkPoint.resolving);
        }

        checkPoint.resolving = new BoundHost(host, fragments);

        checkPoint.used.add(checkPoint.resolving);

        checkPoint.unused.remove(checkPoint.resolving);
        for (FragmentGeneration fragmentGeneration : fragments) checkPoint.unused.remove(new Candidate(fragmentGeneration));

        return checkPoint;
    }

    // remove overlapping import
    public CheckPoint newCheckPoint(ImportDescriptionWrapper targetImport)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        assert checkPoint.getResolving().removeImport(targetImport.getPackageName());

        return checkPoint;
    }

    public CheckPoint newCheckPoint(Resolved resolvedHost, FragmentGeneration fragmentGeneration) throws IncompatibleException
    {
        CheckPoint checkPoint = new CheckPoint(this);

        return checkPoint;  //Todo: change body of created methods use File | Settings | File Templates.
    }

    public CheckPoint newCheckPoint(UnBound unBound, FragmentGeneration fragmentGeneration) throws IncompatibleException
    {
        CheckPoint checkPoint = new CheckPoint(this);

        return checkPoint;  //Todo: change body of created methods use File | Settings | File Templates.
    }

    public CheckPoint newCheckPoint(BoundHost bound, FragmentGeneration fragmentGeneration) throws IncompatibleException
    {
        CheckPoint checkPoint = new CheckPoint(this);

        return checkPoint;  //Todo: change body of created methods use File | Settings | File Templates.
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
