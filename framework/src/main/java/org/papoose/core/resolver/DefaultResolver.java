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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.papoose.core.BundleGeneration;
import org.papoose.core.ExportDescription;
import org.papoose.core.FragmentDescription;
import org.papoose.core.FragmentGeneration;
import org.papoose.core.FrameworkExtensionGeneration;
import org.papoose.core.Generation;
import org.papoose.core.ImportDescription;
import org.papoose.core.Papoose;
import org.papoose.core.PapooseException;
import org.papoose.core.Util;
import org.papoose.core.VersionRange;
import org.papoose.core.Wire;
import org.papoose.core.spi.Resolver;
import org.papoose.core.spi.Solution;


/**
 * @version $Revision$ $Date$
 */
@ThreadSafe
public class DefaultResolver implements Resolver
{
    private final static String CLASS_NAME = DefaultResolver.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Object lock = new Object();
    @GuardedBy("lock") private final Set<Generation> bundles = new HashSet<Generation>();
    @GuardedBy("lock") private final Map<String, List<BundleGeneration>> indexByPackage = new HashMap<String, List<BundleGeneration>>();
    @GuardedBy("lock") private Papoose framework;

    /**
     * {@inheritDoc}
     */
    public void start(Papoose framework) throws PapooseException
    {
        LOGGER.entering(CLASS_NAME, "start", framework);

        if (framework == null)
        {
            LOGGER.warning("Framework cannot be null");
            throw new IllegalArgumentException("Framework cannot be null");
        }

        synchronized (lock)
        {
            if (this.framework != null)
            {
                IllegalStateException ise = new IllegalStateException("Framework has already started");
                LOGGER.throwing(CLASS_NAME, "start", ise);
                throw ise;
            }

            this.framework = framework;
        }

        LOGGER.exiting(CLASS_NAME, "start");
    }

    /**
     * {@inheritDoc}
     */
    public void stop()
    {
        LOGGER.entering(CLASS_NAME, "stop");

        synchronized (lock)
        {
            if (this.framework == null)
            {
                IllegalStateException ise = new IllegalStateException("Framework has already stopped");
                LOGGER.warning("Framework has already stopped");
                LOGGER.throwing(CLASS_NAME, "stop", ise);
                throw ise;
            }
            framework = null;
        }

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    /**
     * {@inheritDoc}
     */
    public void added(Generation generation)
    {
        LOGGER.entering(CLASS_NAME, "added", generation);

        synchronized (lock)
        {
            if (this.framework == null)
            {
                IllegalStateException ise = new IllegalStateException("Framework has not started");
                LOGGER.warning("Framework has not started");
                LOGGER.throwing(CLASS_NAME, "stop", ise);
                throw ise;
            }

            bundles.add(generation);

            if (generation instanceof BundleGeneration)
            {
                LOGGER.finest("Bundle is an regular bundleGeneration");

                for (ExportDescription exportDescription : generation.getArchiveStore().getBundleExportList())
                {
                    LOGGER.log(Level.FINEST, "Indexing", exportDescription);

                    for (String packageName : exportDescription.getPackages())
                    {
                        List<BundleGeneration> bundleList = indexByPackage.get(packageName);
                        if (bundleList == null) indexByPackage.put(packageName, bundleList = new ArrayList<BundleGeneration>());

                        bundleList.add((BundleGeneration) generation);
                    }
                }
            }
        }

        LOGGER.exiting(CLASS_NAME, "added");
    }

    /**
     * {@inheritDoc}
     */
    public void removed(Generation generation)
    {
        LOGGER.entering(CLASS_NAME, "removed", generation);

        synchronized (lock)
        {
            if (this.framework == null)
            {
                IllegalStateException ise = new IllegalStateException("Framework has not started");
                LOGGER.warning("Framework has not started");
                LOGGER.throwing(CLASS_NAME, "stop", ise);
                throw ise;
            }

            bundles.remove(generation);

            if (generation instanceof BundleGeneration)
            {
                LOGGER.finest("Bundle is an regular bundleGeneration");

                BundleGeneration bundle = (BundleGeneration) generation;
                for (ExportDescription exportDescription : bundle.getArchiveStore().getBundleExportList())
                {
                    for (String packageName : exportDescription.getPackages())
                    {
                        List<BundleGeneration> bundleList = indexByPackage.get(packageName);
                        if (bundleList == null) continue;

                        bundleList.remove(bundle);

                        if (bundleList.isEmpty()) indexByPackage.remove(packageName);
                    }
                }
            }
        }

        LOGGER.exiting(CLASS_NAME, "removed");
    }

    /**
     * {@inheritDoc}
     */
    public Set<Solution> resolve(Generation bundle) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "resolve", bundle);

        if (bundle == null) throw new IllegalArgumentException("Bundle cannot be null");
        if (bundle.getState() != Bundle.INSTALLED) throw new BundleException("Bundle not in INSTALLED STATE");
        if (framework == null) throw new IllegalStateException("Framework has not started");

        synchronized (lock)
        {
            CheckPoint result = null;

            if (bundle instanceof BundleGeneration)
            {
                List<FragmentGeneration> availableFragments = collectAvailableFragments((BundleGeneration) bundle);

                for (List<FragmentGeneration> fragments : Util.combinations(availableFragments))
                {
                    CandidateBundle candidate = new CandidateBundle(bundle, (BundleGeneration) bundle, fragments);

                    CheckPoint checkPoint = new CheckPoint(candidate, generateBundleSet(bundle, bundles));

                    result = doResolveBundle(checkPoint);

                    if (result != null) break;
                }
            }
            else if (bundle instanceof FragmentGeneration)
            {
                BundleGeneration host = collectAvailableHost((FragmentGeneration) bundle);

                if (host == null) throw new BundleException("No consistent solution set found");

                List<FragmentGeneration> fragments = new ArrayList<FragmentGeneration>(host.getFragments());
                fragments.add((FragmentGeneration) bundle);

                CandidateBundle candidate = new CandidateBundle(bundle, host, fragments);

                CheckPoint checkPoint = new CheckPoint(candidate, generateBundleSet(bundle, bundles));

                result = doResolveBundle(checkPoint);
            }
            else if (bundle instanceof FrameworkExtensionGeneration)
            {
            }

            if (result == null) throw new BundleException("No consistent solution set found");

            return extractSolutions(result);
        }
    }

    private Set<Solution> extractSolutions(CheckPoint result)
    {
        Set<Solution> solutions = new HashSet<Solution>();

        for (CandidateBundle candidateBundle : result.getResolved())
        {
            Set<Wire> wires = new HashSet<Wire>();

            for (CandidateWiring candidateWiring : candidateBundle.getCandidateWirings())
            {
                Wire wire = new Wire(candidateWiring.getPackageName(), candidateWiring.getExportDescription(), candidateWiring.getBundleGeneration());
                wires.add(wire);
            }

            Solution solution = new Solution(candidateBundle.getBundleGeneration(), candidateBundle.getFragments(), wires, Collections.<Wire>emptyList());
            solutions.add(solution);
        }

        return solutions;  //Todo change body of created methods use File | Settings | File Templates.
    }

    private Set<Generation> generateBundleSet(Generation removeGeneration, Set<Generation> bundles)
    {
        assert Thread.holdsLock(lock);

        Set<Generation> result = new HashSet<Generation>();

        for (Generation generation : bundles)
        {
            if (generation.getState() != Bundle.UNINSTALLED)
            {
                result.add(generation);
            }
        }

        result.remove(removeGeneration);

        return result;
    }


    private List<FragmentGeneration> collectAvailableFragments(BundleGeneration bundle)
    {
        assert Thread.holdsLock(lock);

        List<FragmentGeneration> result = new ArrayList<FragmentGeneration>();

        String hostSymbolName = bundle.getSymbolicName();
        Version hostVersion = bundle.getVersion();
        for (Generation generation : bundles)
        {
            if (generation instanceof FragmentGeneration)
            {
                FragmentGeneration fragmentGeneration = (FragmentGeneration) generation;
                FragmentDescription description = fragmentGeneration.getArchiveStore().getBundleFragmentHost();

                if (fragmentGeneration.getState() == Bundle.INSTALLED
                    && description.getSymbolName().equals(hostSymbolName)
                    && description.getVersionRange().includes(hostVersion))
                {
                    result.add(fragmentGeneration);
                }
            }
        }

        return result;
    }

    private BundleGeneration collectAvailableHost(FragmentGeneration fragmentBundle)
    {
        assert Thread.holdsLock(lock);

        return null;  //todo: consider this autogenerated code
    }

    private CheckPoint doResolveBundle(CheckPoint checkPoint)
    {
        assert Thread.holdsLock(lock);
        assert checkPoint != null;
        assert checkPoint.getResolving() != null;

        while ((checkPoint = resolveWires(checkPoint)) != null && !checkPoint.isDone())
        {
            checkPoint = checkPoint.nextBundle();
        }

        return checkPoint;
    }

    private CheckPoint resolveWires(CheckPoint checkPoint)
    {
        assert Thread.holdsLock(lock);
        assert checkPoint != null;
        assert checkPoint.getResolving() != null;

        List<ImportDescriptionWrapper> imports = checkPoint.getResolving().getImports();

        if (!imports.isEmpty())
        {
            ImportDescriptionWrapper targetImport = imports.remove(0);

            for (ExportDescriptionWrapper candidateExport : collectEligibleExportsFromUsed(targetImport, checkPoint.getUsed()))
            {
                if (matches(targetImport, candidateExport))
                {
                    Set<Wire> impliedCandidateWirings = collectImpliedConstraints(candidateExport.getExportDescription(), candidateExport.getBundleGeneration());

                    if (isConsistent(checkPoint.getUsed(), impliedCandidateWirings))
                    {
                        CandidateWiring newWiring = new CandidateWiring(targetImport.getPackageName(), candidateExport.getExportDescription(), candidateExport.getBundleGeneration());

                        CheckPoint result = resolveWires(checkPoint.newCheckPoint(newWiring));
                        if (result != null) return result;
                    }
                }
            }

            for (ExportDescriptionWrapper candidateExport : collectEligibleExportsFromUnused(targetImport, checkPoint.getUnused()))
            {
                if (matches(targetImport, candidateExport))
                {
                    Set<Wire> impliedCandidateWirings = collectImpliedConstraints(candidateExport.getExportDescription(), candidateExport.getBundleGeneration());

                    if (isConsistent(checkPoint.getUsed(), impliedCandidateWirings))
                    {
                        CandidateBundle newBundle = generateCandidatePairing(candidateExport);
                        CandidateWiring newWiring = new CandidateWiring(targetImport.getPackageName(), candidateExport.getExportDescription(), candidateExport.getBundleGeneration());

                        CheckPoint result = resolveWires(checkPoint.newCheckPoint(newBundle, newWiring));
                        if (result != null) return result;
                    }
                }
            }

            if (targetImport.isMandatory()) return null;
        }
        else if (!checkPoint.isDone())
        {
            return doResolveBundle(checkPoint);
        }
        else
        {
            checkPoint.resolveCompleted();
        }

        return checkPoint;
    }

    private CandidateBundle generateCandidatePairing(ExportDescriptionWrapper candidateExport)
    {
        CandidateBundle candidateBundle = new CandidateBundle(candidateExport.getBundleGeneration(), candidateExport.getBundleGeneration(), Collections.<FragmentGeneration>emptyList());
        return candidateBundle;  //Todo change body of created methods use File | Settings | File Templates.
    }


    static final Set<BundleGeneration> BUNDLES_SCANNED = new HashSet<BundleGeneration>();

    /**
     * Collect the implied constraints for a particular export for a particular
     * bundle.  The implied constraints are obtained by following the "uses"
     * directive.
     *
     * @param exportDescription
     * @param bundleGeneration
     * @return
     */
    private Set<Wire> collectImpliedConstraints(ExportDescription exportDescription, BundleGeneration bundleGeneration)
    {
        assert Thread.holdsLock(lock);

        try
        {
            return doCollectImpliedConstraints(exportDescription, bundleGeneration);
        }
        finally
        {
            BUNDLES_SCANNED.clear();
        }
    }

    private Set<Wire> doCollectImpliedConstraints(ExportDescription exportDescription, BundleGeneration bundleGeneration)
    {
        assert Thread.holdsLock(lock);

        Set<Wire> result = new HashSet<Wire>();

        if (BUNDLES_SCANNED.contains(bundleGeneration))
        {
            return result;
        }
        else
        {
            BUNDLES_SCANNED.add(bundleGeneration);
        }

        for (String packageName : exportDescription.getUses())
        {
            for (Wire wire : bundleGeneration.getClassLoader().getWires())
            {
                if (packageName.equals(wire.getPackageName()))
                {
                    result.add(wire);

                    result.addAll(doCollectImpliedConstraints(wire.getExportDescription(), wire.getBundleGeneration()));
                }
            }
        }
        return result;
    }

    private boolean isConsistent(Set<CandidateBundle> used, Set<Wire> impliedCandidateWirings)
    {
        Map<String, Wire> implied = new HashMap<String, Wire>(impliedCandidateWirings.size());

        for (Wire wire : impliedCandidateWirings) implied.put(wire.getPackageName(), wire);

        for (CandidateBundle candidateBundle : used)
        {
            for (CandidateWiring candidateWiring : candidateBundle.getCandidateWirings())
            {
                String packageName = candidateWiring.getPackageName();
                if (implied.containsKey(packageName) && candidateBundle.getBundleGeneration() != implied.get(packageName).getBundleGeneration()) return false;
            }
        }
        return true;
    }

    private boolean matches(ImportDescriptionWrapper targetImport, ExportDescriptionWrapper candidateExport)
    {
        String importPackage = targetImport.getPackageName();
        Map<String, Object> importParameters = targetImport.getImportDescription().getParameters();
        ExportDescription exportDescription = candidateExport.getExportDescription();
        Map<String, Object> exportParameters = exportDescription.getParameters();

        for (String exportPackage : candidateExport.getExportDescription().getPackages())
        {
            if (importPackage.equals(exportPackage))
            {
                if (importParameters.containsKey(Constants.VERSION_ATTRIBUTE))
                {
                    VersionRange range = (VersionRange) importParameters.get(Constants.VERSION_ATTRIBUTE);
                    if (!range.includes((Version) exportParameters.get(Constants.VERSION_ATTRIBUTE))) return false;
                }
                if (importParameters.containsKey(Constants.PACKAGE_SPECIFICATION_VERSION))
                {
                    VersionRange range = (VersionRange) importParameters.get(Constants.PACKAGE_SPECIFICATION_VERSION);
                    if (!range.includes((Version) exportParameters.get(Constants.VERSION_ATTRIBUTE))) return false;
                }
                if (importParameters.containsKey(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE))
                {
                    String symbolicName = (String) importParameters.get(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE);
                    if (!symbolicName.equals(candidateExport.getBundleGeneration().getSymbolicName())) return false;
                }
                if (importParameters.containsKey(Constants.BUNDLE_VERSION_ATTRIBUTE))
                {
                    VersionRange range = (VersionRange) importParameters.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
                    if (!range.includes(candidateExport.getBundleGeneration().getVersion())) return false;
                }

                Set<String> importKeys = new HashSet<String>(importParameters.keySet());
                importKeys.removeAll(Arrays.asList(Constants.VERSION_ATTRIBUTE, Constants.PACKAGE_SPECIFICATION_VERSION, Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, Constants.BUNDLE_VERSION_ATTRIBUTE, Constants.MANDATORY_DIRECTIVE));
                importKeys.removeAll(exportDescription.getMandatory());

                for (String importKey : importKeys)
                {
                    if (exportParameters.containsKey(importKey) && !importParameters.get(importKey).equals(exportParameters.get(importKey))) return false;
                }

                for (String mandatoryExportKey : exportDescription.getMandatory())
                {
                    if (!importParameters.containsKey(mandatoryExportKey)) return false;

                    if (!exportParameters.get(mandatoryExportKey).equals(importParameters.get(mandatoryExportKey))) return false;
                }

                return true;
            }

        }
        return false;
    }

    private List<ExportDescriptionWrapper> collectEligibleExportsFromUsed(ImportDescriptionWrapper targetImport, Set<CandidateBundle> used)
    {
        String importPackage = targetImport.getPackageName();
        List<ExportDescriptionWrapper> results = new ArrayList<ExportDescriptionWrapper>();

        for (CandidateBundle candidateBundle : used)
        {
            for (ExportDescriptionWrapper exportDescriptionWrapper : candidateBundle.getExports())
            {
                for (String exportPackage : exportDescriptionWrapper.getExportDescription().getPackages())
                {
                    if (importPackage.equals(exportPackage))
                    {
                        results.add(exportDescriptionWrapper);
                        break;
                    }
                }
            }
        }
        return results;
    }

    private List<ExportDescriptionWrapper> collectEligibleExportsFromUnused(ImportDescriptionWrapper targetImport, Set<Generation> unused)
    {
        String importPackage = targetImport.getPackageName();
        List<ExportDescriptionWrapper> results = new ArrayList<ExportDescriptionWrapper>();

        for (Generation generation : unused)
        {
            if (generation instanceof BundleGeneration)
            {
                for (ExportDescription exportDescription : generation.getArchiveStore().getBundleExportList())
                {
                    for (String exportPackage : exportDescription.getPackages())
                    {
                        if (importPackage.equals(exportPackage))
                        {
                            results.add(new ExportDescriptionWrapper(exportDescription, (BundleGeneration) generation));
                            break;
                        }
                    }
                }
            }
        }
        return results;
    }

    /**
     * Return a culled set of bundles that export the package indicated in the
     * import statement.
     *
     * @param importWrapper A description of the import
     * @return a sorted set of bundles that export the package indicated in the import statement
     */
    private SortedSet<ExportDescriptionWrapper> collectEligibleExports(ImportDescriptionWrapper importWrapper)
    {
        SortedSet<ExportDescriptionWrapper> sorted = new TreeSet<ExportDescriptionWrapper>();
        for (BundleGeneration bundle : indexByPackage.get(importWrapper.getPackageName()))
        {
            for (ExportDescription exportDescription : bundle.getArchiveStore().getBundleExportList())
            {
                sorted.add(new ExportDescriptionWrapper(exportDescription, bundle));
            }
        }

        return sorted;
    }

    protected static Set<CandidateWiring> collectImpliedConstraints(Set<String> uses, BundleGeneration bundle)
    {
        Set<CandidateWiring> result = new HashSet<CandidateWiring>();

        nextPackage:
        for (String packageName : uses)
        {
            for (Wire wire : bundle.getClassLoader().getWires())
            {
                if (packageName.equals(wire.getPackageName()))
                {
                    ExportDescription exportDescription = wire.getExportDescription();

                    result.addAll(collectImpliedConstraints(exportDescription.getUses(), wire.getBundleGeneration()));
                    result.add(new CandidateWiring(packageName, exportDescription, wire.getBundleGeneration()));

                    continue nextPackage;
                }
            }
        }
        return result;
    }

    private static Set<Wire> collectWires(Set<CandidateWiring> candidateWirings)
    {
        Set<Wire> wires = new HashSet<Wire>();

        for (CandidateWiring candidateWiring : candidateWirings)
        {
            wires.add(new Wire(candidateWiring.getPackageName(), candidateWiring.getExportDescription(), candidateWiring.getBundleGeneration()));
        }

        return wires;
    }

    /**
     * Import descriptions can contain many packages.  We need the individual packages.
     *
     * @param importDescriptions a list of import descriptions
     * @return the list of packages contained in the list of import descriptions
     */
    private static List<ImportDescriptionWrapper> collectPackages(List<ImportDescription> importDescriptions)
    {
        List<ImportDescriptionWrapper> work = new ArrayList<ImportDescriptionWrapper>();

        for (ImportDescription importDescription : importDescriptions)
        {
            for (String packageName : importDescription.getPackages())
            {
                work.add(new ImportDescriptionWrapper(packageName, importDescription));
            }
        }

        return work;
    }

}
