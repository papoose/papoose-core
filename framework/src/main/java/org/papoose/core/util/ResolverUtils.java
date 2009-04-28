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
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.papoose.core.BundleGeneration;
import org.papoose.core.FragmentGeneration;
import org.papoose.core.Generation;
import org.papoose.core.VersionRange;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.FragmentDescription;
import org.papoose.core.descriptions.RequireDescription;
import org.papoose.core.resolver.BoundHost;
import org.papoose.core.resolver.Candidate;
import org.papoose.core.resolver.CandidateBundle;
import org.papoose.core.resolver.CheckPoint;
import org.papoose.core.resolver.ExportDescriptionWrapper;
import org.papoose.core.resolver.ImportDescriptionWrapper;
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
                    result.add(new Resolved(bundleGeneration));
                }
                else
                {
                    result.add(new UnBound(generation));
                }
            }
            else if (generation instanceof FragmentGeneration && generation.getState() == Bundle.INSTALLED)
            {
                result.add(new UnBound(generation));
            }
        }

        return result;
    }

    public static List<FragmentGeneration> collectAvailableFragments(BundleGeneration host, Set<Candidate> canonicalSet)
    {
        List<FragmentGeneration> result = new ArrayList<FragmentGeneration>();

        String hostSymbolName = host.getSymbolicName();
        Version hostVersion = host.getVersion();

        for (Candidate candidate : canonicalSet)
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
                        result.add(fragmentGeneration);
                    }
                }
            }
        }

        return result;
    }

    public static List<Candidate> collectAvailableHosts(FragmentGeneration fragmentBundle)
    {
        return null;  //todo: consider this autogenerated code
    }

    public static List<CandidateBundle> collectEligibleBundlesFromUsed(RequireDescription requireDescription, CheckPoint checkPoint)
    {
        List<CandidateBundle> result = new ArrayList<CandidateBundle>();
        Map<String, Object> prameters = requireDescription.getParameters();

        for (CandidateBundle candidate : checkPoint.getUsed())
        {
            if (candidate instanceof Resolved)
            {
                Resolved resolvedHost = (Resolved) candidate;
                BundleGeneration bundleGeneration = resolvedHost.getBundleGeneration();

                if (requireDescription.getSymbolName().equals(bundleGeneration.getSymbolicName()))
                {
                    if (prameters.containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE))
                    {
                        VersionRange range = (VersionRange) prameters.get(Constants.BUNDLE_VERSION_ATTRIBUTE);

                        if (range.includes(bundleGeneration.getVersion()))
                        {
                            result.add(candidate);
                        }
                    }
                    else
                    {
                        result.add(candidate);
                    }
                }
            }
            else if (candidate instanceof UnBound)
            {
                assert false;
            }
            else
            {
                BoundHost bound = (BoundHost) candidate;
                BundleGeneration bundleGeneration = bound.getBundleGeneration();

                if (requireDescription.getSymbolName().equals(bundleGeneration.getSymbolicName()))
                {
                    if (prameters.containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE))
                    {
                        VersionRange range = (VersionRange) prameters.get(Constants.BUNDLE_VERSION_ATTRIBUTE);

                        if (range.includes(bundleGeneration.getVersion()))
                        {
                            result.add(candidate);
                        }
                    }
                    else
                    {
                        result.add(candidate);
                    }
                }
            }
        }

        return result;
    }

    public static List<Candidate> collectEligibleBundlesFromUnused(RequireDescription requireDescription, CheckPoint checkPoint)
    {
        List<Candidate> result = new ArrayList<Candidate>();
        Map<String, Object> prameters = requireDescription.getParameters();

        for (Candidate candidate : checkPoint.getUnused())
        {
            if (candidate instanceof Resolved)
            {
                Resolved resolvedHost = (Resolved) candidate;
                BundleGeneration bundleGeneration = resolvedHost.getBundleGeneration();

                if (requireDescription.getSymbolName().equals(bundleGeneration.getSymbolicName()))
                {
                    if (prameters.containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE))
                    {
                        VersionRange range = (VersionRange) prameters.get(Constants.BUNDLE_VERSION_ATTRIBUTE);

                        if (range.includes(bundleGeneration.getVersion()))
                        {
                            result.add(candidate);
                        }
                    }
                    else
                    {
                        result.add(candidate);
                    }
                }
            }
            else if (candidate instanceof UnBound)
            {
                UnBound unBound = (UnBound) candidate;
                Generation bundleGeneration = unBound.getToBeResolved();

                if (requireDescription.getSymbolName().equals(bundleGeneration.getSymbolicName()))
                {
                    if (prameters.containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE))
                    {
                        VersionRange range = (VersionRange) prameters.get(Constants.BUNDLE_VERSION_ATTRIBUTE);

                        if (range.includes(bundleGeneration.getVersion()))
                        {
                            result.add(candidate);
                        }
                    }
                    else
                    {
                        result.add(candidate);
                    }
                }
            }
            else
            {
                assert false;
            }
        }

        return result;
    }

    public static List<ExportDescriptionWrapper> collectEligibleExportsFromUsed(ImportDescriptionWrapper targetImport, CheckPoint checkPoint)
    {
        BoundHost resolving = checkPoint.getResolving();
        String importPackage = targetImport.getPackageName();
        List<ExportDescriptionWrapper> results = new ArrayList<ExportDescriptionWrapper>();

        for (CandidateBundle candidateBundle : checkPoint.getUsed())
        {
            if (!candidateBundle.equals(resolving))
            {
                for (ExportDescriptionWrapper exportDescriptionWrapper : candidateBundle.getExports())
                {
                    for (String exportPackage : exportDescriptionWrapper.getExportDescription().getPackageNames())
                    {
                        if (importPackage.equals(exportPackage))
                        {
                            results.add(exportDescriptionWrapper);
                            break;
                        }
                    }
                }
            }
        }
        return results;
    }

    public static List<ExportDescriptionWrapper> collectEligibleExportsFromUnused(ImportDescriptionWrapper targetImport, CheckPoint checkPoint)
    {
        String importPackage = targetImport.getPackageName();
        List<ExportDescriptionWrapper> results = new ArrayList<ExportDescriptionWrapper>();

        for (Candidate candidate : checkPoint.getUnused())
        {
            if (candidate instanceof Resolved)
            {
                Resolved resolved = (Resolved) candidate;
                Generation generation = resolved.getBundleGeneration();

                for (ExportDescription exportDescription : generation.getArchiveStore().getExportDescriptions())
                {
                    for (String exportPackage : exportDescription.getPackageNames())
                    {
                        if (importPackage.equals(exportPackage))
                        {
                            results.add(new ExportDescriptionWrapper(exportDescription, candidate));
                            break;
                        }
                    }
                }
            }
            else
            {
                UnBound unBound = (UnBound) candidate;
                Generation generation = unBound.getToBeResolved();

                for (ExportDescription exportDescription : generation.getArchiveStore().getExportDescriptions())
                {
                    for (String exportPackage : exportDescription.getPackageNames())
                    {
                        if (importPackage.equals(exportPackage))
                        {
                            results.add(new ExportDescriptionWrapper(exportDescription, candidate));
                            break;
                        }
                    }
                }
            }
        }

        return results;
    }

    public static List<ExportDescriptionWrapper> collectEligibleExportsFromUnresolved(ImportDescriptionWrapper targetImport, CheckPoint checkPoint)
    {
        String importPackage = targetImport.getPackageName();
        List<ExportDescriptionWrapper> results = new ArrayList<ExportDescriptionWrapper>();

        for (UnBound unBound : checkPoint.getUnResolved())
        {
            Generation generation = unBound.getToBeResolved();

            for (ExportDescription exportDescription : generation.getArchiveStore().getExportDescriptions())
            {
                for (String exportPackage : exportDescription.getPackageNames())
                {
                    if (importPackage.equals(exportPackage))
                    {
                        results.add(new ExportDescriptionWrapper(exportDescription, unBound));
                        break;
                    }
                }
            }
        }

        return results;
    }

    private ResolverUtils() { }
}
