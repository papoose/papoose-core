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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Attributes;

import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
public abstract class ArchiveStore
{
    private final Papoose framework;
    private final long bundleId;
    private final int generation;
    private final String bundleActivatorClass;
    private final List<String> bundleCategories;
    private final List<String> bundleClassPath;
    private final String bundleContactAddress;
    private final String bundleCopyright;
    private final String bundleDescription;
    private final String bundleDocUrl;
    private final String bundleLocalization;
    private final short bundleManifestVersion;
    private final String bundleName;
    private final List<NativeCodeDescription> bundleNativeCodeList;
    private final boolean bundleNativeCodeListOptional;
    private final SortedSet<NativeCodeDescription> nativeCodeDescriptions;
    private final List<String> bundleExecutionEnvironment;
    private final String bundleSymbolicName;
    private final URL bundleUpdateLocation;
    private final String bundleVendor;
    private final Version bundleVersion;
    private final List<DynamicDescription> bundleDynamicImportList;
    private final List<ExportDescription> bundleExportList;
    private final List<String> bundleExportService;
    private final FragmentDescription bundleFragmentHost;
    private final List<ImportDescription> bundleImportList;
    private final List<String> bundleImportService;
    private final List<RequireDescription> bundleRequireBundle;

    protected ArchiveStore(Papoose framework, long bundleId, int generation, Attributes attributes) throws BundleException
    {
        this.framework = framework;
        this.bundleId = bundleId;
        this.generation = generation;

        this.bundleActivatorClass = attributes.getValue(Constants.BUNDLE_ACTIVATOR);
        this.bundleCategories = obtainBundleCategories(attributes);
        this.bundleClassPath = obtainBundleClasspath(attributes);
        this.bundleContactAddress = attributes.getValue(Constants.BUNDLE_CONTACTADDRESS);
        this.bundleCopyright = attributes.getValue(Constants.BUNDLE_COPYRIGHT);
        this.bundleDescription = attributes.getValue(Constants.BUNDLE_DESCRIPTION);
        this.bundleDocUrl = attributes.getValue(Constants.BUNDLE_DOCURL);
        this.bundleLocalization = attributes.getValue(Constants.BUNDLE_LOCALIZATION);
        this.bundleManifestVersion = obtainBundleManifestVersion(attributes.getValue(Constants.BUNDLE_MANIFESTVERSION));
        this.bundleName = attributes.getValue(Constants.BUNDLE_NAME);
        this.bundleNativeCodeList = obtainBundleNativeCodeList(attributes);
        this.bundleExecutionEnvironment = obtainBundleExecutionEnvironment(attributes);
        this.bundleSymbolicName = attributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
        this.bundleUpdateLocation = obtainBundleUpdateLocation(attributes);
        this.bundleVendor = attributes.getValue(Constants.BUNDLE_VENDOR);
        this.bundleVersion = Version.parseVersion(attributes.getValue(Constants.BUNDLE_VERSION));
        this.bundleDynamicImportList = obtainBundleDynamicImportList(attributes);
        this.bundleExportList = obtainBundleExportList(attributes);
        this.bundleExportService = obtainBundleExportService(attributes);
        this.bundleFragmentHost = obtainBundleFragementHost(attributes);
        this.bundleImportList = obtainBundleImportList(attributes);
        this.bundleImportService = obtainBundleImportService(attributes);
        this.bundleRequireBundle = obtainBundleRequireBundle(attributes);

        this.bundleNativeCodeListOptional = bundleNativeCodeList.size() > 0 && "*".equals(bundleNativeCodeList.get(bundleNativeCodeList.size() - 1));

        setNativeCodeDescriptions(this.nativeCodeDescriptions = resolveNativeCodeDependencies());

        if (bundleManifestVersion != 2) throw new BundleException("Bundle-ManifestVersion must be 2");

        confirmRequiredExecutionEnvironment();
    }

    abstract File getArchive();

    /**
     * Set the native code descriptions that the bundle store is to use
     * when loading native code libraries.
     *
     * @param nativeCodeDescriptions the sorted set of native code descriptions
     * @throws org.osgi.framework.BundleException
     *          if the set of native code descriptions is empty
     */
    abstract void setNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException;

    abstract String loadLibrary(String libname);

    abstract Permission[] getPermissionCollection();

    abstract ResourceHandle getResource(String resourceName);

    long getBundleId()
    {
        return bundleId;
    }

    int getGeneration()
    {
        return generation;
    }

    String getBundleActivatorClass()
    {
        return bundleActivatorClass;
    }

    String getBundleName()
    {
        return bundleName;
    }

    Version getBundleVersion()
    {
        return bundleVersion;
    }

    List<ExportDescription> getBundleExportList()
    {
        return bundleExportList;
    }

    List<ImportDescription> getBundleImportList()
    {
        return bundleImportList;
    }

    /**
     * Make sure that at least one native code description is valid.
     * <p/>
     * This could be done during the <code>BundleImpl</code> constructor but
     * it seems to be more transparent to have the bundle manager call this
     * method.
     *
     * @return a list of resolvable native code descriptions
     * @throws BundleException if the method is unable to find at least one valid native code description
     */
    private SortedSet<NativeCodeDescription> resolveNativeCodeDependencies() throws BundleException
    {
        SortedSet<NativeCodeDescription> set = new TreeSet<NativeCodeDescription>();

        if (!bundleNativeCodeList.isEmpty())
        {
            VersionRange osVersionRange = VersionRange.parseVersionRange((String) framework.getProperty(Constants.FRAMEWORK_OS_VERSION));

            nextDescription:
            for (NativeCodeDescription description : bundleNativeCodeList)
            {
                Map<String, Object> parameters = description.getParameters();
                for (String key : parameters.keySet())
                {
                    if ("osname".equals(key) && !framework.getProperty(Constants.FRAMEWORK_OS_NAME).equals(parameters.get(key))) continue nextDescription;
                    else if ("processor".equals(key) && !framework.getProperty(Constants.FRAMEWORK_PROCESSOR).equals(parameters.get(key))) continue nextDescription;
                    else if ("osversion".equals(key))
                    {
                        if (!osVersionRange.includes(description.getOsVersion())) continue nextDescription;
                    }
                    else if ("language".equals(key) && !framework.getProperty(Constants.FRAMEWORK_LANGUAGE).equals(description.getLanguage())) continue nextDescription;
                    else if ("selection-filter".equals(key))
                    {
                        try
                        {
                            Filter selectionFilter = new FilterImpl(framework.getParser().parse((String) parameters.get(key)));
                            if (!selectionFilter.match(framework.getProperties())) continue nextDescription;
                        }
                        catch (InvalidSyntaxException ise)
                        {
                            throw new BundleException("Invalid selection filter", ise);
                        }
                    }
                }

                set.add(description);
            }
        }

        return set;
    }


    protected void confirmRequiredExecutionEnvironment() throws BundleException
    {
        if (!bundleExecutionEnvironment.isEmpty())
        {
            String string = (String) framework.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
            if (string == null) throw new BundleException(Constants.FRAMEWORK_EXECUTIONENVIRONMENT + " not set");
            String[] environments = string.split(",");

            nextRequirement:
            for (String requirement : bundleExecutionEnvironment)
            {
                for (String environment : environments)
                {
                    if (requirement.equals(environment)) continue nextRequirement;
                }
                throw new BundleException("Missing required execution environment: " + requirement);
            }
        }
    }

    protected static List<String> obtainBundleCategories(Attributes attributes)
    {
        List<String> result;

        if (attributes.containsKey(Constants.BUNDLE_CATEGORY))
        {
            String[] tokens = attributes.getValue(Constants.BUNDLE_CATEGORY).split(",");
            result = new ArrayList<String>(tokens.length);

            for (String token : tokens) result.add(token.trim());
        }
        else
        {
            result = Collections.emptyList();
        }

        return result;
    }

    protected static List<String> obtainBundleClasspath(Attributes attributes) throws BundleException
    {
        List<String> result;

        if (attributes.containsKey(Constants.BUNDLE_CLASSPATH))
        {
            String[] tokens = attributes.getValue(Constants.BUNDLE_CLASSPATH).split(",");
            result = new ArrayList<String>(tokens.length);

            for (String token : tokens)
            {
                token = token.trim();

                if (!Util.isValidPackageName(token)) throw new BundleException("Malformed package in Bundle-Classpath: " + token);

                result.add(token);
            }
        }
        else
        {
            result = new ArrayList<String>(1);
            result.add(".");
        }

        return result;
    }

    private static short obtainBundleManifestVersion(String value)
    {
        try
        {
            return Short.parseShort(value);
        }
        catch (NumberFormatException e)
        {
            return 2;
        }
    }

    protected static List<NativeCodeDescription> obtainBundleNativeCodeList(Attributes attributes) throws BundleException
    {
        List<NativeCodeDescription> result;
        if (attributes.containsKey(Constants.BUNDLE_NATIVECODE))
        {
            String[] nativecodes = Util.split(attributes.getValue(Constants.BUNDLE_NATIVECODE), ",");
            result = new ArrayList<NativeCodeDescription>(nativecodes.length);
            int ordinal = 0;

            for (String nativecode : nativecodes)
            {
                List<String> paths = new ArrayList<String>(1);
                Map<String, Object> parameters = new HashMap<String, Object>();
                NativeCodeDescription nativeCodeDescription = new NativeCodeDescription(paths, parameters, ordinal++);

                Util.parseParameters(nativecode, nativeCodeDescription, parameters, false, paths);

                if (parameters.containsKey("osversion")) parameters.put("osversion", VersionRange.parseVersionRange((String) parameters.get("osversion")));
                if (parameters.containsKey("language")) parameters.put("language", new Locale((String) parameters.get("language")));
                if (parameters.containsKey("selection-filter")) parameters.put("selection-filter", VersionRange.parseVersionRange((String) parameters.get("selection-filter")));

                result.add(nativeCodeDescription);
            }
        }
        else
        {
            result = Collections.emptyList();
        }

        return result;
    }

    protected static List<String> obtainBundleExecutionEnvironment(Attributes attributes)
    {
        List<String> result;

        if (attributes.containsKey("Bundle-ExecutionEnvironment"))
        {
            String[] tokens = attributes.getValue("Bundle-ExecutionEnvironment").split(",");
            result = new ArrayList<String>(tokens.length);

            for (String token : tokens) result.add(token.trim());
        }
        else
        {
            result = Collections.emptyList();
        }

        return result;
    }

    protected static URL obtainBundleUpdateLocation(Attributes attributes) throws BundleException
    {
        try
        {
            if (attributes.containsKey("Bundle-UpdateLocation")) return new URL(attributes.getValue("Bundle-UpdateLocation"));
            else return null;
        }
        catch (MalformedURLException murle)
        {
            throw new BundleException("Invalid Bundle-UpdateLocation", murle);
        }
    }

    protected static List<DynamicDescription> obtainBundleDynamicImportList(Attributes attributes) throws BundleException
    {
        List<DynamicDescription> result;

        if (attributes.containsKey("DynamicImport-Package"))
        {
            String[] importDescriptions = attributes.getValue("DynamicImport-Package").split(",");
            result = new ArrayList<DynamicDescription>(importDescriptions.length);

            for (String importDescription : importDescriptions)
            {
                List<String> paths = new ArrayList<String>(1);
                Map<String, Object> parameters = new HashMap<String, Object>();
                DynamicDescription description = new DynamicDescription(paths, parameters);

                Util.parseParameters(importDescription, description, parameters, true, paths);

                if (description.getVersion() == null) Util.callSetter(description, "version", DynamicDescription.DEFAULT_VERSION_RANGE);
                if (description.getBundleVersion() == null) Util.callSetter(description, "bundle-version", DynamicDescription.DEFAULT_VERSION_RANGE);

                result.add(description);
            }
        }
        else
        {
            result = Collections.emptyList();
        }

        return result;
    }

    protected static List<ExportDescription> obtainBundleExportList(Attributes attributes) throws BundleException
    {
        List<ExportDescription> result;

        if (attributes.containsKey(Constants.EXPORT_PACKAGE))
        {
            String[] exportDescriptions = Util.split(attributes.getValue(Constants.EXPORT_PACKAGE), ",");
            result = new ArrayList<ExportDescription>(exportDescriptions.length);

            for (String exportDescription : exportDescriptions)
            {
                List<String> paths = new ArrayList<String>(1);
                Map<String, Object> parameters = new HashMap<String, Object>();
                ExportDescription description = new ExportDescription(paths, parameters);

                Util.parseParameters(exportDescription, description, parameters, true, paths);

                if (parameters.containsKey("specification-version")) parameters.put("specification-version", Version.parseVersion((String) parameters.get("specification-version")));

                if (!parameters.containsKey("version"))
                {
                    if (parameters.containsKey("specification-version")) parameters.put("version", parameters.get("specification-version"));
                    else parameters.put("version", ExportDescription.DEFAULT_VERSION);
                }
                else
                {
                    parameters.put("version", Version.parseVersion((String) parameters.get("version")));
                }

                if (parameters.containsKey("specification-version") && !parameters.get("specification-version").equals(parameters.get("version"))) throw new BundleException("version and specification-version do not match");

                if (parameters.containsKey("bundle-symbolic-name")) throw new BundleException("Attempted to set bundle-symbolic-name in Export-Package");

                if (parameters.containsKey("bundle-version")) throw new BundleException("Attempted to set bundle-version in Export-Package");

                result.add(description);
            }
        }
        else
        {
            result = Collections.emptyList();
        }

        return result;
    }

    private static FragmentDescription obtainBundleFragementHost(Attributes attributes) throws BundleException
    {
        FragmentDescription fragmentDescription = null;

        if (attributes.containsKey(Constants.FRAGMENT_HOST))
        {
            Map<String, Object> parameters = new HashMap<String, Object>();
            String description = attributes.getValue(Constants.FRAGMENT_HOST);
            int index = description.indexOf(';');

            if (index != -1)
            {
                fragmentDescription = new FragmentDescription(Util.checkSymbolName(description.substring(0, index)), parameters);

                Util.parseParameters(description.substring(index + 1), fragmentDescription, parameters, true);
            }
            else
            {
                fragmentDescription = new FragmentDescription(Util.checkSymbolName(description), parameters);
            }

            if (parameters.containsKey("bundle-version")) parameters.put("bundle-version", VersionRange.parseVersionRange((String) parameters.get("bundle-verison")));
            else parameters.put("bundle-version", FragmentDescription.DEFAULT_VERSION_RANGE);
        }

        return fragmentDescription;
    }

    @SuppressWarnings({"deprecation"})
    protected static List<String> obtainBundleExportService(Attributes attributes)
    {
        List<String> result;

        if (attributes.containsKey(Constants.EXPORT_SERVICE))
        {
            String[] tokens = attributes.getValue(Constants.EXPORT_SERVICE).split(",");
            result = new ArrayList<String>(tokens.length);

            for (String token : tokens) result.add(token.trim());
        }
        else
        {
            result = Collections.emptyList();
        }

        return result;
    }

    protected static List<ImportDescription> obtainBundleImportList(Attributes attributes) throws BundleException
    {
        List<ImportDescription> result;

        if (attributes.containsKey(Constants.IMPORT_PACKAGE))
        {
            Set<String> importedPaths = new HashSet<String>();
            String[] importDescriptions = attributes.getValue(Constants.IMPORT_PACKAGE).split(",");
            result = new ArrayList<ImportDescription>(importDescriptions.length);

            for (String importDescription : importDescriptions)
            {
                List<String> paths = new ArrayList<String>(1);
                Map<String, Object> parameters = new HashMap<String, Object>();

                ImportDescription description = new ImportDescription(paths, parameters);

                Util.parseParameters(importDescription, description, parameters, true, paths);

                if (parameters.containsKey("specification-version")) parameters.put("specification-version", VersionRange.parseVersionRange((String) parameters.get("specification-version")));

                if (!parameters.containsKey("version"))
                {
                    if (parameters.containsKey("specification-version")) parameters.put("version", parameters.get("specification-version"));
                    else parameters.put("version", ImportDescription.DEFAULT_VERSION_RANGE);
                }
                else
                {
                    parameters.put("version", VersionRange.parseVersionRange((String) parameters.get("version")));
                }

                if (parameters.containsKey("specification-version") && !parameters.get("specification-version").equals(parameters.get("version"))) throw new BundleException("version and specification-version do not match");

                if (parameters.containsKey("bundle-version")) parameters.put("bundle-version", VersionRange.parseVersionRange((String) parameters.get("bundle-veriosn")));
                else parameters.put("bundle-version", ImportDescription.DEFAULT_VERSION_RANGE);

                for (String path : paths)
                {
                    if (importedPaths.contains(path)) throw new BundleException("Duplicate import: " + path);
                    else importedPaths.add(path);
                }

                result.add(description);
            }
        }
        else
        {
            result = Collections.emptyList();
        }

        return result;
    }

    @SuppressWarnings({"deprecation"})
    protected static List<String> obtainBundleImportService(Attributes attributes)
    {
        List<String> result;

        if (attributes.containsKey(Constants.IMPORT_SERVICE))
        {
            String[] tokens = attributes.getValue(Constants.IMPORT_SERVICE).split(",");
            result = new ArrayList<String>(tokens.length);

            for (String token : tokens) result.add(token.trim());
        }
        else
        {
            result = Collections.emptyList();
        }

        return result;
    }

    protected static List<RequireDescription> obtainBundleRequireBundle(Attributes attributes) throws BundleException
    {
        List<RequireDescription> result = null;

        if (attributes.containsKey(Constants.REQUIRE_BUNDLE))
        {
            result = new ArrayList<RequireDescription>();

            String[] descriptions = Util.split(attributes.getValue(Constants.REQUIRE_BUNDLE), ",");
            for (String description : descriptions)
            {
                Map<String, Object> parameters = new HashMap<String, Object>();
                RequireDescription requireDescription;
                int index = description.indexOf(';');

                if (index != -1)
                {
                    requireDescription = new RequireDescription(Util.checkSymbolName(description.substring(0, index)), parameters);

                    Util.parseParameters(description.substring(index + 1), requireDescription, parameters, true);
                }
                else
                {
                    requireDescription = new RequireDescription(Util.checkSymbolName(description), parameters);
                }

                if (requireDescription.getVisibility() == null) Util.callSetter(requireDescription, "visibility", Visibility.PRIVATE);

                if (requireDescription.getResolution() == null) Util.callSetter(requireDescription, "resolution", Resolution.MANDATORY);

                if (parameters.containsKey("bundle-version")) parameters.put("bundle-version", VersionRange.parseVersionRange((String) parameters.get("bundle-verison")));
                else parameters.put("bundle-version", RequireDescription.DEFAULT_VERSION_RANGE);

                result.add(requireDescription);
            }
        }

        return result;
    }
}
