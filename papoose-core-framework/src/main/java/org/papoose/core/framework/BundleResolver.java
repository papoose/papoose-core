/**
 *
 * Copyright 2007 (C) The original author or authors
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
package org.papoose.core.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
public class BundleResolver
{
    Set<Wire> resolve(List<ImportDescription> bundleImportList, Set<BundleImpl> bundles)
    {
        return resolve(collectPackages(bundleImportList), bundles, new HashSet<Candidate>(), new HashSet<Candidate>());
    }

    private static Set<Wire> resolve(List<Package> packages, Set<BundleImpl> bundles, Set<Candidate> candidates, Set<Candidate> impliedSet)
    {
        packages = new ArrayList<Package>(packages);

        Package pkg = packages.remove(0);

        for (ExportDescriptionWrapper candidate : collectEligibleExports(pkg, bundles))
        {
            if (match(pkg.getPackageName(), pkg.getImportDescription(), candidate.getExportDescription()))
            {
                Set<Candidate> implied = collectImpliedConstraints(candidate.getExportDescription().getUses(), candidate.getBundle());

                assert implied != null;

                if (isConsistent(impliedSet, implied))
                {
                    Set<Candidate> intersection = new HashSet<Candidate>(implied);
                    intersection.retainAll(candidates);

                    candidates = new HashSet<Candidate>(candidates);
                    candidates.add(new Candidate(pkg.getPackageName(), candidate.getExportDescription(), candidate.getBundle()));

                    impliedSet = new HashSet<Candidate>(impliedSet);
                    impliedSet.addAll(implied);

                    if (packages.isEmpty())
                    {
                        return collectWires(candidates);
                    }
                    else
                    {
                        Set<Wire> result = resolve(packages, bundles, candidates, impliedSet);
                        if (!result.isEmpty()) return result;
                    }
                }
            }
        }

        return Collections.emptySet();
    }

    private static SortedSet<ExportDescriptionWrapper> collectEligibleExports(Package pkg, Set<BundleImpl> bundles)
    {
        String bundleName = (String) pkg.getParameters().get("bundle-symbolic-name");
        VersionRange bundleVersionRange = (VersionRange) pkg.getParameters().get("bundle-version");

        SortedSet<ExportDescriptionWrapper> sorted = new TreeSet<ExportDescriptionWrapper>();

        for (BundleImpl bundle : bundles)
        {
            boolean nameMatch = (bundleName == null || bundleName.equals(bundle.getCurrentStore().getBundleSymbolicName()));
            boolean versionMatch = (bundleVersionRange == null || bundleVersionRange.includes(bundle.getCurrentStore().getBundleVersion()));

            if (nameMatch && versionMatch)
            {
                for (ExportDescription exportDescription : bundle.getCurrentStore().getBundleExportList())
                {
                    sorted.add(new ExportDescriptionWrapper(exportDescription, bundle));
                }
            }
        }

        return sorted;
    }

    protected static Set<Candidate> collectImpliedConstraints(Set<String> uses, BundleImpl bundle)
    {
        Set<Candidate> result = new HashSet<Candidate>();

        nextPackage:
        for (String packageName : uses)
        {
            for (Wire wire : bundle.getClassLoader().getWires())
            {
                if (wire.getPackageName().equals(packageName))
                {
                    ExportDescription exportDescription = wire.getExportDescription();

                    result.addAll(collectImpliedConstraints(exportDescription.getUses(), wire.getBundle()));
                    result.add(new Candidate(packageName, exportDescription, wire.getBundle()));

                    continue nextPackage;
                }
            }
        }
        return result;
    }

    private static boolean isConsistent(Set<Candidate> usedSet, Set<Candidate> used)
    {
        Set<Candidate> intersection = new HashSet<Candidate>(usedSet);

        intersection.retainAll(used);

        for (Candidate candidate : intersection)
        {
            ExportDescription version = candidate.getExportDescription();
            for (Candidate c : used)
            {
                if (!version.equals(c.getExportDescription())) return false;
            }

        }
        return true;
    }

    private static Set<Wire> collectWires(Set<Candidate> candidates)
    {
        Set<Wire> wires = new HashSet<Wire>();

        for (Candidate candidate : candidates)
        {
            wires.add(new Wire(candidate.getPackageName(), candidate.getExportDescription(), candidate.getBundle()));
        }

        return wires;
    }

    private static boolean match(String importPackage, ImportDescription importDescription, ExportDescription exportDescription)
    {
        for (String exportPackage : exportDescription.getPackages())
        {
            if (importPackage.equals(exportPackage))
            {
                VersionRange importVersionRange = (VersionRange) importDescription.getParameters().get("version");
                if (importVersionRange.includes((Version) exportDescription.getParameters().get("version")))
                {
                    for (String key : exportDescription.getMandatory())
                    {
                        if (!exportDescription.getParameters().get(key).equals(importDescription.getParameters().get(key))) return false;
                    }

                    for (String key : importDescription.getParameters().keySet())
                    {
                        if ("version".equals(key) || "bundle-version".equals(key)) continue;
                        if (!importDescription.getParameters().get(key).equals(exportDescription.getParameters().get(key))) return false;
                    }

                    return true;
                }
                break;
            }
        }

        return false;
    }

    private static List<Package> collectPackages(List<ImportDescription> importDescriptions)
    {
        List<Package> work = new ArrayList<Package>();

        for (ImportDescription importDescription : importDescriptions)
        {
            for (String packageName : importDescription.getPackageNames())
            {
                work.add(new Package(packageName, importDescription));
            }
        }

        return work;
    }

    private static class Package
    {
        private final String packageName;
        private final ImportDescription importDescription;

        public Package(String packageName, ImportDescription importDescription)
        {
            this.packageName = packageName;
            this.importDescription = importDescription;
        }

        public String getPackageName()
        {
            return packageName;
        }

        public Map<String, Object> getParameters()
        {
            return importDescription.getParameters();
        }

        public ImportDescription getImportDescription()
        {
            return importDescription;
        }

        public String toString()
        {
            return packageName;
        }
    }

    protected static class Candidate
    {
        private final String packageName;
        private final ExportDescription exportDescription;
        private final BundleImpl bundle;

        public Candidate(String packageName, ExportDescription exportDescription, BundleImpl bundle)
        {
            assert packageName != null;
            assert exportDescription != null;
            assert bundle != null;

            this.packageName = packageName;
            this.exportDescription = exportDescription;
            this.bundle = bundle;
        }

        public String getPackageName()
        {
            return packageName;
        }

        public ExportDescription getExportDescription()
        {
            return exportDescription;
        }

        public BundleImpl getBundle()
        {
            return bundle;
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Candidate candidate = (Candidate) o;

            return packageName.equals(candidate.packageName);
        }

        public int hashCode()
        {
            return packageName.hashCode();
        }
    }

    /**
     * A simple wrapper to make sure that export descriptions are searched in
     * the proper order.  This wrapper assumes that <code>BundleImpl</code>
     * classes initially sort by their resolution status, i.e. resolved bundles
     * appear before un-resolved bundles.
     */
    private static class ExportDescriptionWrapper implements Comparable<ExportDescriptionWrapper>
    {
        private final ExportDescription exportDescription;
        private final BundleImpl bundle;
        private final long bundleId;
        private final Version version;

        public ExportDescriptionWrapper(ExportDescription exportDescription, BundleImpl bundle)
        {
            this.exportDescription = exportDescription;
            this.bundle = bundle;
            this.bundleId = bundle.getBundleId();
            this.version = (Version) exportDescription.getParameters().get("version");
        }

        public ExportDescription getExportDescription()
        {
            return exportDescription;
        }

        public BundleImpl getBundle()
        {
            return bundle;
        }

        public int compareTo(ExportDescriptionWrapper o)
        {
            int result = version.compareTo(o.version);
            if (result == 0) result = (int) (bundleId - o.bundleId);
            return result;
        }
    }
}
