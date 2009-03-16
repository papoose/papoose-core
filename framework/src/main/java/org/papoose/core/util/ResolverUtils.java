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
package org.papoose.core.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import org.papoose.core.BundleGeneration;
import org.papoose.core.FragmentGeneration;
import org.papoose.core.Generation;
import org.papoose.core.descriptions.FragmentDescription;
import org.papoose.core.resolver.BoundFragment;
import org.papoose.core.resolver.Candidate;
import org.papoose.core.resolver.CheckPoint;
import org.papoose.core.resolver.Resolved;
import org.papoose.core.resolver.UnBound;
import org.papoose.core.resolver.UnResolved;

/**
 * @version $Revision$ $Date$
 */
public class ResolverUtils
{
    /**
     * Returns a set of resolved and unresolved bundles.  If a fragment is
     * resolved to its host it is not included in this set.  Extensions are
     * also not included in this set.
     *
     * @param bundles the original, full, set of bundles
     * @return the "cleaned" set of bundles
     */
    public static Set<Candidate> collectCanonicalSet(Set<Generation> bundles)
    {
        Set<Candidate> result = new HashSet<Candidate>();

        for (Generation generation : bundles)
        {
            if (generation instanceof BundleGeneration)
            {
                BundleGeneration bundleGeneration = (BundleGeneration) generation;

                if (generation.getState() != Bundle.INSTALLED)
                {
                    result.add(new UnBound(generation));
                }
                else
                {
                    result.add(new Resolved(bundleGeneration));
                }
            }
            else if (generation instanceof FragmentGeneration && generation.getState() == Bundle.INSTALLED)
            {
                result.add(new UnBound(generation));
            }
        }

        return result;
    }

    public static List<BoundFragment> collectAvailableFragments(BundleGeneration host, CheckPoint checkPoint)
    {
        List<BoundFragment> result = new ArrayList<BoundFragment>();

        String hostSymbolName = host.getSymbolicName();
        Version hostVersion = host.getVersion();

        for (Candidate candidate : checkPoint.getUsed())
        {
            Generation generation;

            if (candidate instanceof UnResolved)
            {
                generation = ((UnResolved) candidate).getToBeResolved();

                if (generation instanceof FragmentGeneration)
                {
                    FragmentGeneration fragmentGeneration = (FragmentGeneration) generation;
                    FragmentDescription description = fragmentGeneration.getArchiveStore().getFragmentDescription();

                    if (fragmentGeneration.getState() == Bundle.INSTALLED
                        && description.getSymbolName().equals(hostSymbolName)
                        && description.getVersionRange().includes(hostVersion))
                    {
                        result.add(new BoundFragment(((UnResolved) candidate).getToBeResolved(), host));
                    }
                }
            }
        }

        for (Candidate candidate : checkPoint.getUnused())
        {
            Generation generation;

            if (candidate instanceof UnResolved)
            {
                generation = ((UnResolved) candidate).getToBeResolved();

                if (generation instanceof FragmentGeneration)
                {
                    FragmentGeneration fragmentGeneration = (FragmentGeneration) generation;
                    FragmentDescription description = fragmentGeneration.getArchiveStore().getFragmentDescription();

                    if (fragmentGeneration.getState() == Bundle.INSTALLED
                        && description.getSymbolName().equals(hostSymbolName)
                        && description.getVersionRange().includes(hostVersion))
                    {
                        result.add(new BoundFragment(((UnResolved) candidate).getToBeResolved(), host));
                    }
                }
            }
        }

        return result;
    }

    private ResolverUtils() { }
}
