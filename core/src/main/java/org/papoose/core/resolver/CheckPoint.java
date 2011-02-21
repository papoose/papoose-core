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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.papoose.core.BundleClassLoader;
import org.papoose.core.BundleGeneration;
import org.papoose.core.FragmentGeneration;
import org.papoose.core.Generation;
import org.papoose.core.Wire;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.descriptions.RequireDescription;
import org.papoose.core.descriptions.Visibility;
import static org.papoose.core.util.Assert.assertTrue;
import org.papoose.core.util.ToStringCreator;


/**
 *
 */
public class CheckPoint
{
    private BoundHost resolving;
    private final List<BoundHost> resolved = new ArrayList<BoundHost>();
    private final List<UnBound> unResolved = new ArrayList<UnBound>();

    private final Set<CandidateBundle> used = new HashSet<CandidateBundle>();
    private final Set<Candidate> unused = new HashSet<Candidate>();
    private final Map<String, Candidate> singletons = new HashMap<String, Candidate>();


    public CheckPoint(Generation generation, Set<Candidate> canonicalSet) throws IncompatibleException
    {
        assert generation != null;
        assert canonicalSet != null;

        assertTrue(unResolved.add(new UnBound(generation)));
        assertTrue(unused.addAll(canonicalSet));

        for (Candidate candidate : unused)
        {
            Generation g = candidate.getGeneration();
            if (g.getArchiveStore().isSingleton())
            {
                assertTrue(singletons.put(g.getSymbolicName(), candidate) == null);
            }
        }
    }

    public CheckPoint(BundleGeneration bundleGeneration, ImportDescription importDescription, Set<Candidate> canonicalSet) throws IncompatibleException
    {
        assert bundleGeneration != null;
        assert importDescription != null;
        assert canonicalSet != null;

        assertTrue(unused.addAll(canonicalSet));

        for (Candidate candidate : unused)
        {
            Generation g = candidate.getGeneration();
            if (g.getArchiveStore().isSingleton())
            {
                assertTrue(singletons.put(g.getSymbolicName(), candidate) == null);
            }
        }

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
                    assertTrue(resolving.addWiring(new CandidateWiring(packageName, exportDescription, candidateBundle)));
                }
            }
        }
        catch (IncompatibleException neverHappens)
        {
            assert false;
        }

        assertTrue(used.add(resolving));

        assertTrue(unused.remove(resolving));
        for (FragmentGeneration fragmentGeneration : resolving.getFragments()) assertTrue(unused.remove(new Candidate(fragmentGeneration)));

        checkSingltonViolation(bundleGeneration);
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
        singletons.putAll(checkPoint.singletons);

        if (checkPoint.resolving != null)
        {
            resolving = new BoundHost(checkPoint.resolving);
            assertTrue(used.remove(resolving));
            assertTrue(used.add(resolving));

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

    public CheckPoint newCheckPointUsed(Resolved resolvedHost, RequireDescription requireDescription) throws IncompatibleException
    {
        CheckPoint checkPoint = new CheckPoint(this);

        checkPoint.getResolving().addCandidateRequiredBundle(new RequiredBundleWrapper(resolvedHost, requireDescription.getVisibility() == Visibility.REEXPORT));

        return checkPoint;
    }

    public CheckPoint newCheckPointUnused(Resolved resolvedHost, RequireDescription requireDescription) throws IncompatibleException
    {
        CheckPoint checkPoint = new CheckPoint(this);

        checkPoint.getResolving().addCandidateRequiredBundle(new RequiredBundleWrapper(resolvedHost, requireDescription.getVisibility() == Visibility.REEXPORT));
        assertTrue(checkPoint.getUsed().add(resolvedHost));
        assertTrue(checkPoint.getUnused().remove(resolvedHost));

        return checkPoint;
    }

    public CheckPoint newCheckPoint(BoundHost bound, RequireDescription requireDescription) throws IncompatibleException
    {
        CheckPoint checkPoint = new CheckPoint(this);

        for (CandidateBundle candidateBundle : checkPoint.getUsed())
        {
            if (candidateBundle.equals(bound))
            {
                BoundHost boundHost = (BoundHost) candidateBundle;
                boundHost.addCandidateRequiredBundle(new RequiredBundleWrapper(boundHost, requireDescription.getVisibility() == Visibility.REEXPORT));
            }
        }

        return checkPoint;
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

        assertTrue(checkPoint.resolving.addCandidateWiring(candidateWiring));

        return checkPoint;
    }

    // add a wire to the bundle being resolved
    // move resolved to used
    public CheckPoint newCheckPoint(CandidateWiring candidateWiring, Resolved resolved)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        assertTrue(checkPoint.resolving.addCandidateWiring(candidateWiring));
        assertTrue(checkPoint.used.add(resolved));
        assertTrue(checkPoint.unused.remove(resolved));

        return checkPoint;
    }

    // add a wire to the bundle being resolved
    // move the unbound to to be resolved
    // add the unbound to used
    public CheckPoint newCheckPoint(CandidateWiring candidateWiring, UnBound unBound)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        assertTrue(checkPoint.resolving.addCandidateWiring(candidateWiring));
        assertTrue(checkPoint.unResolved.add(unBound));
        assertTrue(checkPoint.unused.remove(unBound));

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

        assertTrue(checkPoint.used.add(checkPoint.resolving));

        assertTrue(checkPoint.unused.remove(checkPoint.resolving));
        for (FragmentGeneration fragmentGeneration : fragments) assertTrue(checkPoint.unused.remove(new Candidate(fragmentGeneration)));

        checkSingltonViolation(host);

        return checkPoint;
    }

    // remove overlapping import
    public CheckPoint newCheckPoint(ImportDescriptionWrapper targetImport)
    {
        CheckPoint checkPoint = new CheckPoint(this);

        assertTrue(checkPoint.getResolving().removeImport(targetImport.getPackageName()));

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

    private void checkSingltonViolation(BundleGeneration bundleGeneration) throws IncompatibleException
    {
        if (bundleGeneration.getArchiveStore().isSingleton() && singletons.containsKey(bundleGeneration.getSymbolicName())) throw new IncompatibleException("Singleton already exists for symbolic name " + bundleGeneration.getSymbolicName());
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
