/**
 *
 * Copyright 2007-2008 (C) The original author or authors
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import org.papoose.core.framework.spi.Resolver;
import org.papoose.core.framework.spi.Solution;


/**
 * @version $Revision$ $Date$
 */
@ThreadSafe
class DefaultResolver implements Resolver
{
    private final static String CLASS_NAME = DefaultResolver.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Object lock = new Object();
    @GuardedBy("lock") private final Set<AbstractBundle> bundles = new HashSet<AbstractBundle>();
    @GuardedBy("lock") private final Map<String, List<BundleImpl>> indexByPackage = new HashMap<String, List<BundleImpl>>();
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
    public void added(AbstractBundle abstractBundle)
    {
        LOGGER.entering(CLASS_NAME, "added", abstractBundle);

        synchronized (lock)
        {
            if (this.framework == null)
            {
                IllegalStateException ise = new IllegalStateException("Framework has not started");
                LOGGER.warning("Framework has not started");
                LOGGER.throwing(CLASS_NAME, "stop", ise);
                throw ise;
            }

            bundles.add(abstractBundle);

            if (abstractBundle instanceof BundleImpl)
            {
                LOGGER.finest("Bundle is an regular bundle");

                for (ExportDescription exportDescription : abstractBundle.getCurrentStore().getBundleExportList())
                {
                    LOGGER.log(Level.FINEST, "Indexing", exportDescription);

                    for (String packageName : exportDescription.getPackages())
                    {
                        List<BundleImpl> bundleList = indexByPackage.get(packageName);
                        if (bundleList == null) indexByPackage.put(packageName, bundleList = new ArrayList<BundleImpl>());

                        bundleList.add((BundleImpl) abstractBundle);
                    }
                }
            }
        }

        LOGGER.exiting(CLASS_NAME, "added");
    }

    /**
     * {@inheritDoc}
     */
    public void removed(AbstractBundle abstractBundle)
    {
        LOGGER.entering(CLASS_NAME, "removed", abstractBundle);

        synchronized (lock)
        {
            if (this.framework == null)
            {
                IllegalStateException ise = new IllegalStateException("Framework has not started");
                LOGGER.warning("Framework has not started");
                LOGGER.throwing(CLASS_NAME, "stop", ise);
                throw ise;
            }

            bundles.remove(abstractBundle);

            if (abstractBundle instanceof BundleImpl)
            {
                LOGGER.finest("Bundle is an regular bundle");

                BundleImpl bundle = (BundleImpl) abstractBundle;
                for (ExportDescription exportDescription : bundle.getCurrentStore().getBundleExportList())
                {
                    for (String packageName : exportDescription.getPackages())
                    {
                        List<BundleImpl> bundleList = indexByPackage.get(packageName);
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
    public Set<Solution> resolve(AbstractBundle bundle) throws BundleException
    {
        LOGGER.entering(CLASS_NAME, "resolve", bundle);

        if (bundle == null) throw new IllegalArgumentException("Bundle cannot be null");
        if (bundle.getState() != Bundle.INSTALLED) throw new BundleException("Bundle not in INSTALLED STATE");

        synchronized (lock)
        {
            if (framework == null) throw new IllegalStateException("Framework has not started");
            if (framework != bundle.getFramework()) throw new IllegalArgumentException("Bundle does not belong to the same framework instance");

            CheckPoint result = null;

            if (bundle instanceof BundleImpl)
            {
                List<FragmentBundleImpl> availableFragments = collectAvailableFragments((BundleImpl) bundle);

                for (List<FragmentBundleImpl> fragments : Util.combinations(availableFragments))
                {
                    CandidateBundle candidate = new CandidateBundle(bundle, (BundleImpl) bundle, fragments);

                    CheckPoint checkPoint = new CheckPoint(candidate, generateBundleSet(bundles));

                    result = resolveBundle(checkPoint);
                }
            }
            else if (bundle instanceof FragmentBundleImpl)
            {
                BundleImpl host = collectAvailableHost((FragmentBundleImpl) bundle);

                if (host == null) throw new BundleException("No consistent solution set found");

                List<FragmentBundleImpl> fragments = new ArrayList<FragmentBundleImpl>(host.getFragments());
                fragments.add((FragmentBundleImpl) bundle);

                CandidateBundle candidate = new CandidateBundle(bundle, host, fragments);

                CheckPoint checkPoint = new CheckPoint(candidate, generateBundleSet(bundles));

                result = resolveBundle(checkPoint);
            }
            else if (bundle instanceof ExtensionBundleImpl)
            {
            }

            if (result == null) throw new BundleException("No consistent solution set found");

            return extractSolutions(result);
        }
    }

    private Set<Solution> extractSolutions(CheckPoint result)
    {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    private Set<AbstractBundle> generateBundleSet(Set<AbstractBundle> bundles)
    {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }


    private List<FragmentBundleImpl> collectAvailableFragments(BundleImpl bundle)
    {
        assert Thread.holdsLock(lock);

        List<FragmentBundleImpl> result = new ArrayList<FragmentBundleImpl>();

        return result;  //todo: consider this autogenerated code
    }

    private BundleImpl collectAvailableHost(FragmentBundleImpl fragmentBundle)
    {
        assert Thread.holdsLock(lock);

        return null;  //todo: consider this autogenerated code
    }

    private CheckPoint resolveBundle(CheckPoint checkPoint)
    {
        assert Thread.holdsLock(lock);
        assert checkPoint != null;
        assert !checkPoint.isDone();
        assert checkPoint.resolving != null;

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
        assert checkPoint.resolving != null;

        List<ImportDescriptionWrapper> imports = checkPoint.resolving.getImports();

        if (!imports.isEmpty())
        {
            ImportDescriptionWrapper targetImport = imports.remove(0);

            for (ExportDescriptionWrapper candidateExport : collectEligibleExports(targetImport, checkPoint.used))
            {
                if (matches(targetImport, candidateExport))
                {
                    Set<CandidateWiring> impliedCandidateWirings = collectImpliedConstraints(candidateExport);

                    if (isConsistent(impliedCandidateWirings, checkPoint.used))
                    {
                        CandidateWiring newWiring = new CandidateWiring(targetImport.getPackageName(), candidateExport.getExportDescription(), candidateExport.getBundle());

                        CheckPoint result = resolveWires(checkPoint.newCheckPoint(newWiring));
                        if (result != null) return result;
                    }
                }
            }

            for (ExportDescriptionWrapper candidateExport : collectEligibleExports(checkPoint.unused))
            {
                if (matches(targetImport, candidateExport))
                {
                    Set<CandidateWiring> impliedCandidateWirings = collectImpliedConstraints(targetImport, candidateExport);

                    if (isConsistent(impliedCandidateWirings, checkPoint.used))
                    {
                        CandidateBundle newBundle = generateCandidatePairing(candidateExport);
                        CandidateWiring newWiring = new CandidateWiring(targetImport.getPackageName(), candidateExport.getExportDescription(), candidateExport.getBundle());

                        CheckPoint result = resolveWires(checkPoint.newCheckPoint(newBundle, newWiring));
                        if (result != null) return result;
                    }
                }
            }

            if (targetImport.isMandatory()) return null;
        }

        return checkPoint;
    }

    private CandidateBundle generateCandidatePairing(ExportDescriptionWrapper candidateExport)
    {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    private Set<CandidateWiring> collectImpliedConstraints(ImportDescriptionWrapper targetImport, ExportDescriptionWrapper candidateExport)
    {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    private ExportDescriptionWrapper[] collectEligibleExports(Set<AbstractBundle> unused)
    {
        return new ExportDescriptionWrapper[0];  //To change body of created methods use File | Settings | File Templates.
    }

    private boolean isConsistent(Set<CandidateWiring> impliedCandidateWirings, Set<CandidateBundle> used)
    {
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    private Set<CandidateWiring> collectImpliedConstraints(ExportDescriptionWrapper candidateExport)
    {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    private boolean matches(ImportDescriptionWrapper targetImport, ExportDescriptionWrapper candidateExport)
    {
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    private ExportDescriptionWrapper[] collectEligibleExports(ImportDescriptionWrapper targetImport, Set<CandidateBundle> used)
    {
        return new ExportDescriptionWrapper[0];  //To change body of created methods use File | Settings | File Templates.
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
        for (BundleImpl bundle : indexByPackage.get(importWrapper.getPackageName()))
        {
            for (ExportDescription exportDescription : bundle.getCurrentStore().getBundleExportList())
            {
                sorted.add(new ExportDescriptionWrapper(exportDescription, bundle));
            }
        }

        return sorted;
    }

    protected static Set<CandidateWiring> collectImpliedConstraints(Set<String> uses, BundleImpl bundle)
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

                    result.addAll(collectImpliedConstraints(exportDescription.getUses(), wire.getBundle()));
                    result.add(new CandidateWiring(packageName, exportDescription, wire.getBundle()));

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
            wires.add(new Wire(candidateWiring.getPackageName(), candidateWiring.getExportDescription(), candidateWiring.getBundle()));
        }

        return wires;
    }

    /**
     * TODO: Make sure the bundle symbolic name and bundle version make it into the import and export descriptions
     * TODO: Properly alias SYSTEM_BUNDLE_SYMBOLICNAME
     */
    private static boolean match(ImportDescription importDescription, ExportDescription exportDescription)
    {
        VersionRange bundleVersionRange = (VersionRange) importDescription.getParameters().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
        boolean versionMatch = bundleVersionRange == null || bundleVersionRange.includes((Version) exportDescription.getParameters().get(Constants.BUNDLE_VERSION));
        if (versionMatch) return false;

        VersionRange importVersionRange = (VersionRange) importDescription.getParameters().get("version");
        if (!importVersionRange.includes((Version) exportDescription.getParameters().get("version"))) return false;

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
            for (String packageName : importDescription.getPackageNames())
            {
                work.add(new ImportDescriptionWrapper(packageName, importDescription));
            }
        }

        return work;
    }

    private static class ImportDescriptionWrapper
    {
        private final String packageName;
        private final ImportDescription importDescription;
        private final boolean mandatory;

        public ImportDescriptionWrapper(String packageName, ImportDescription importDescription)
        {
            this.packageName = packageName;
            this.importDescription = importDescription;
            this.mandatory = importDescription.getParameters().containsKey(Constants.MANDATORY_DIRECTIVE) && (Boolean) importDescription.getParameters().get(Constants.MANDATORY_DIRECTIVE);
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

        public boolean isMandatory()
        {
            return mandatory;
        }
    }

    /**
     * A simple wrapper to make sure that export descriptions are searched in
     * the proper order.  This wrapper assumes that <code>BundleImpl</code>
     * classes initially sort by their resolution status, i.e. resolved bundles
     * appear before un-resolved bundles.
     * <p/>
     * TODO: So why don't we include status in compareTo()?
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

    protected static class CandidateWiring
    {
        private final String packageName;
        private final ExportDescription exportDescription;
        private final BundleImpl bundle;

        public CandidateWiring(String packageName, ExportDescription exportDescription, BundleImpl bundle)
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

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CandidateWiring candidateWiring = (CandidateWiring) o;

            return packageName.equals(candidateWiring.packageName);
        }

        @Override
        public int hashCode()
        {
            return packageName.hashCode();
        }
    }

    private static class BundleWrapper
    {
        private final AbstractBundle bundle;

        BundleWrapper(AbstractBundle bundle)
        {
            this.bundle = bundle;
        }
    }

    private static class CheckPoint
    {
        CandidateBundle resolving;
        List<CandidateBundle> toBeResolved = new ArrayList<CandidateBundle>();
        Set<CandidateBundle> used = new HashSet<CandidateBundle>();
        Set<AbstractBundle> unused = new HashSet<AbstractBundle>();


        CheckPoint(CandidateBundle resolving, Set<AbstractBundle> unused)
        {
            this.resolving = resolving;
            this.unused.addAll(unused);
        }

        public boolean isDone()
        {
            return toBeResolved.isEmpty();
        }

        public CheckPoint nextBundle()
        {
            resolving = toBeResolved.get(0);
            return this;
        }

        public CheckPoint newCheckPoint(CandidateWiring newWiring)
        {
            return null;  //To change body of created methods use File | Settings | File Templates.
        }

        public CheckPoint newCheckPoint(CandidateBundle newBundle, CandidateWiring newWiring)
        {
            return null;  //To change body of created methods use File | Settings | File Templates.
        }
    }

    private static class CandidateBundle
    {
        private final AbstractBundle toBeResolved;
        private final BundleImpl bundle;
        private final List<FragmentBundleImpl> fragments;
        private final Set<CandidateWiring> candidateWirings = new HashSet<CandidateWiring>();

        private CandidateBundle(AbstractBundle toBeResolved, BundleImpl bundle, List<FragmentBundleImpl> fragments)
        {
            this.toBeResolved = toBeResolved;
            this.bundle = bundle;
            this.fragments = Collections.unmodifiableList(fragments);
        }

        public CandidateBundle(CandidateBundle resolving)
        {
            this.toBeResolved = resolving.toBeResolved;
            this.bundle = resolving.bundle;
            this.fragments = Collections.unmodifiableList(new ArrayList<FragmentBundleImpl>(resolving.fragments));
            //Todo change body of created methods use File | Settings | File Templates.
        }

        public AbstractBundle getToBeResolved()
        {
            return toBeResolved;
        }

        public BundleImpl getBundle()
        {
            return bundle;
        }

        public List<FragmentBundleImpl> getFragments()
        {
            return fragments;
        }

        public Set<CandidateWiring> getCandidateWirings()
        {
            return candidateWirings;
        }

        public List<ImportDescriptionWrapper> getImports()
        {
            List<ImportDescriptionWrapper> result = new ArrayList<ImportDescriptionWrapper>();
            for (ImportDescription description : bundle.getCurrentStore().getBundleImportList())
            {
                for (String packageName : description.getPackageNames()) result.add(new ImportDescriptionWrapper(packageName, description));
            }
            return result;
        }
    }
}
