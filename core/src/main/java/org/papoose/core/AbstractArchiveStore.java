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
package org.papoose.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

import org.papoose.core.descriptions.DynamicDescription;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.Extension;
import org.papoose.core.descriptions.FragmentDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.descriptions.LazyActivationDescription;
import org.papoose.core.descriptions.NativeCodeDescription;
import org.papoose.core.descriptions.RequireDescription;
import org.papoose.core.descriptions.Resolution;
import org.papoose.core.descriptions.Visibility;
import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.util.AttributeUtils;
import org.papoose.core.util.AttributesWrapper;
import org.papoose.core.util.Util;


/**
 * @version $Revision$ $Date$
 */
public abstract class AbstractArchiveStore implements ArchiveStore
{
    private final Papoose framework;
    private final long bundleId;
    private final int generation;
    private final Attributes attributes;
    private final String bundleActivatorClass;
    private final List<String> bundleClassPath;
    private final String bundleLocalization;
    private final List<NativeCodeDescription> bundleNativeCodeList;
    private final List<String> bundleRequiredExecutionEnvironment;
    private final String bundleSymbolicName;
    private final boolean singleton;
    private final FragmentAttachmentDirective fragmentAttachmentDirective;
    private final URL bundleUpdateLocation;
    private final Version bundleVersion;
    private final List<DynamicDescription> bundleDynamicImportList;
    private final List<ExportDescription> bundleExportList;
    private final FragmentDescription bundleFragmentHost;
    private final List<ImportDescription> bundleImportList;
    private final List<RequireDescription> bundleRequireBundle;
    private final LazyActivationDescription lazyActivationDescription;

    protected AbstractArchiveStore(Papoose framework, long bundleId, int generation, Attributes attributes) throws BundleException
    {
        this.framework = framework;
        this.bundleId = bundleId;
        this.generation = generation;

        this.attributes = new AttributesWrapper(attributes);

        String[] tokens = this.attributes.getValue(Constants.BUNDLE_SYMBOLICNAME).split(";");

        this.bundleSymbolicName = tokens[0];

        String singletonString = "false";
        String fragmentAttachmentString = Constants.FRAGMENT_ATTACHMENT_ALWAYS;
        for (int i = 1; i < tokens.length; i++)
        {
            String[] assignment = tokens[i].split("=:");
            if (Constants.SINGLETON_DIRECTIVE.equals(assignment[0]))
            {
                singletonString = assignment[1];
            }
            else if (Constants.FRAGMENT_ATTACHMENT_DIRECTIVE.equals(assignment[0])) fragmentAttachmentString = assignment[1];
        }
        this.singleton = Boolean.parseBoolean(singletonString);
        this.fragmentAttachmentDirective = FragmentAttachmentDirective.parseFragmentDescription(fragmentAttachmentString);

        this.bundleActivatorClass = this.attributes.getValue(Constants.BUNDLE_ACTIVATOR);
        this.bundleClassPath = obtainBundleClasspath(this.attributes);
        this.bundleLocalization = this.attributes.getValue(Constants.BUNDLE_LOCALIZATION);
        this.bundleNativeCodeList = obtainBundleNativeCodeList(this.attributes);
        this.bundleRequiredExecutionEnvironment = obtainBundleExecutionEnvironment(this.attributes);
        this.bundleUpdateLocation = obtainBundleUpdateLocation(this.attributes);
        this.bundleVersion = Version.parseVersion(this.attributes.getValue(Constants.BUNDLE_VERSION));
        this.bundleDynamicImportList = obtainBundleDynamicImportList(this.attributes);
        this.bundleExportList = obtainBundleExportList(this.attributes, bundleSymbolicName, bundleVersion);
        this.bundleFragmentHost = obtainBundleFragementHost(this.attributes);
        this.bundleImportList = obtainBundleImportList(this.attributes);
        this.bundleRequireBundle = obtainBundleRequireBundle(this.attributes);
        this.lazyActivationDescription = obtainLazyActivationDescription(this.attributes);

        short bundleManifestVersion = obtainBundleManifestVersion(this.attributes.getValue(Constants.BUNDLE_MANIFESTVERSION));
        if (bundleManifestVersion != 2) throw new BundleException("Bundle-ManifestVersion must be 2");

        assert this.lazyActivationDescription != null;
    }

    public Papoose getFramework()
    {
        return framework;
    }

    public String getFrameworkName()
    {
        return framework.getFrameworkName();
    }

    public long getBundleId()
    {
        return bundleId;
    }

    public int getGeneration()
    {
        return generation;
    }

    public Attributes getAttributes()
    {
        return attributes;
    }

    public String getBundleActivatorClass()
    {
        return bundleActivatorClass;
    }

    public String getBundleSymbolicName()
    {
        return bundleSymbolicName;
    }

    public boolean isSingleton()
    {
        return singleton;
    }

    public FragmentAttachmentDirective getFragmentAttachmentDirective()
    {
        return fragmentAttachmentDirective;
    }

    public URL getBundleUpdateLocation()
    {
        return bundleUpdateLocation;
    }

    public Version getBundleVersion()
    {
        return bundleVersion;
    }

    public List<String> getBundleClassPath()
    {
        return bundleClassPath;
    }

    public List<NativeCodeDescription> getBundleNativeCodeList()
    {
        return bundleNativeCodeList;
    }

    public List<String> getBundleRequiredExecutionEnvironment()
    {
        return bundleRequiredExecutionEnvironment;
    }

    public String getBundleLocalization()
    {
        return bundleLocalization;
    }

    public List<ExportDescription> getExportDescriptions()
    {
        return bundleExportList;
    }

    public List<ImportDescription> getImportDescriptions()
    {
        return bundleImportList;
    }

    public List<RequireDescription> getRequireDescriptions()
    {
        return bundleRequireBundle;
    }

    public List<DynamicDescription> getDynamicDescriptions()
    {
        return bundleDynamicImportList;
    }

    public FragmentDescription getFragmentDescription()
    {
        return bundleFragmentHost;
    }

    public LazyActivationDescription getLazyActivationDescription()
    {
        return lazyActivationDescription;
    }

    public boolean isLazyActivationPolicy()
    {
        return lazyActivationDescription.isLazyActivation();
    }

    public int compareTo(Object o)
    {
        if (!(o instanceof AbstractArchiveStore)) return 1;
        return Long.valueOf(bundleId).compareTo(((AbstractArchiveStore) o).getBundleId());
    }

    @Override
    public int hashCode()
    {
        int result = (int) (bundleId ^ (bundleId >>> 32));
        result = 31 * result + generation;
        return result;
    }

    @SuppressWarnings({ "SimplifiableIfStatement" })
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractArchiveStore that = (AbstractArchiveStore) o;

        if (bundleId != that.bundleId) return false;
        return generation == that.generation;
    }

    protected static List<String> obtainBundleClasspath(Attributes headers) throws BundleException
    {
        List<String> result;

        if (headers.containsKey(Constants.BUNDLE_CLASSPATH))
        {
            String[] tokens = headers.getValue(Constants.BUNDLE_CLASSPATH).split(",");
            result = new ArrayList<String>(tokens.length);

            for (String token : tokens) result.add(token.trim());
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

    protected static List<NativeCodeDescription> obtainBundleNativeCodeList(Attributes headers) throws BundleException
    {
        List<NativeCodeDescription> result = Collections.emptyList();
        if (headers.containsKey(Constants.BUNDLE_NATIVECODE))
        {
            String[] nativecodes = Util.split(headers.getValue(Constants.BUNDLE_NATIVECODE), ",");
            result = new ArrayList<NativeCodeDescription>(nativecodes.length);
            int ordinal = 0;

            for (String nativecode : nativecodes)
            {
                Set<String> paths = new HashSet<String>(1);
                Map<String, Object> parameters = new HashMap<String, Object>();
                NativeCodeDescription nativeCodeDescription = new NativeCodeDescription(paths, parameters, ordinal++);

                Util.parseParameters(nativecode, nativeCodeDescription, parameters, false, paths);

                if (parameters.containsKey("osversion")) parameters.put("osversion", VersionRange.parseVersionRange((String) parameters.get("osversion")));
                if (parameters.containsKey("language")) parameters.put("language", new Locale((String) parameters.get("language")));
                if (parameters.containsKey("selection-filter")) parameters.put("selection-filter", VersionRange.parseVersionRange((String) parameters.get("selection-filter")));

                result.add(nativeCodeDescription);
            }
        }

        return result;
    }

    protected static List<String> obtainBundleExecutionEnvironment(Attributes headers)
    {
        List<String> result = Collections.emptyList();

        if (headers.containsKey(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT))
        {
            String[] tokens = headers.getValue(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT).split(",");
            result = new ArrayList<String>(tokens.length);

            for (String token : tokens) result.add(token.trim());
        }

        return result;
    }

    protected static URL obtainBundleUpdateLocation(Attributes headers) throws BundleException
    {
        try
        {
            if (headers.containsKey(Constants.BUNDLE_UPDATELOCATION))
            {
                return new URL(headers.getValue(Constants.BUNDLE_UPDATELOCATION));
            }
            else
            {
                return null;
            }
        }
        catch (MalformedURLException murle)
        {
            throw new BundleException("Invalid Bundle-UpdateLocation", murle);
        }
    }

    protected static List<DynamicDescription> obtainBundleDynamicImportList(Attributes headers) throws BundleException
    {
        List<DynamicDescription> result = Collections.emptyList();

        if (headers.containsKey("DynamicImport-Package"))
        {
            String[] dynamicDescriptions = headers.getValue("DynamicImport-Package").split(",");
            result = new ArrayList<DynamicDescription>(dynamicDescriptions.length);

            for (String dynamicDescription : dynamicDescriptions)
            {
                Set<String> paths = new HashSet<String>(1);
                Map<String, Object> parameters = new HashMap<String, Object>();
                DynamicDescription description = new DynamicDescription(paths, parameters);

                Util.parseParameters(dynamicDescription, description, parameters, true, paths);

                if (parameters.containsKey("version"))
                {
                    parameters.put("version", VersionRange.parseVersionRange((String) parameters.get("version")));
                }
                else
                {
                    parameters.put("version", DynamicDescription.DEFAULT_VERSION_RANGE);
                }

                if (parameters.containsKey("bundle-version"))
                {
                    parameters.put("bundle-version", VersionRange.parseVersionRange((String) parameters.get("bundle-veriosn")));
                }
                else
                {
                    parameters.put("bundle-version", DynamicDescription.DEFAULT_VERSION_RANGE);
                }

                result.add(description);
            }
        }

        return result;
    }

    protected static List<ExportDescription> obtainBundleExportList(Attributes headers, String bundleSymbolicName, Version bundleVersion) throws BundleException
    {
        List<ExportDescription> result = Collections.emptyList();

        if (headers.containsKey(Constants.EXPORT_PACKAGE))
        {
            result = AttributeUtils.parseBundleExportList(headers.getValue(Constants.EXPORT_PACKAGE), bundleSymbolicName, bundleVersion);
        }

        return result;
    }

    private static FragmentDescription obtainBundleFragementHost(Attributes headers) throws BundleException
    {
        FragmentDescription fragmentDescription = null;

        if (headers.containsKey(Constants.FRAGMENT_HOST))
        {
            Map<String, Object> parameters = new HashMap<String, Object>();
            String description = headers.getValue(Constants.FRAGMENT_HOST);
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

            if (parameters.containsKey("bundle-version"))
            {
                try
                {
                    fragmentDescription.setVersionRange(VersionRange.parseVersionRange((String) parameters.get("bundle-verison")));
                }
                catch (IllegalArgumentException e)
                {
                    throw new BundleException("Illegal value for extension parameter", e);
                }
            }
            else
            {
                fragmentDescription.setVersionRange(FragmentDescription.DEFAULT_VERSION_RANGE);
            }

            if (parameters.containsKey("extension"))
            {
                try
                {
                    fragmentDescription.setExtension(Extension.valueOf((String) parameters.get("extension")));
                }
                catch (IllegalArgumentException e)
                {
                    throw new BundleException("Illegal value for extension parameter", e);
                }
            }
        }

        return fragmentDescription;
    }

    protected static List<ImportDescription> obtainBundleImportList(Attributes headers) throws BundleException
    {
        List<ImportDescription> result = Collections.emptyList();

        if (headers.containsKey(Constants.IMPORT_PACKAGE))
        {
            Set<String> importedPaths = new HashSet<String>();
            String[] importDescriptions = Util.split(headers.getValue(Constants.IMPORT_PACKAGE), ",");
            result = new ArrayList<ImportDescription>(importDescriptions.length);

            for (String importDescription : importDescriptions)
            {
                Set<String> paths = new HashSet<String>(1);
                Map<String, Object> parameters = new HashMap<String, Object>();

                ImportDescription description = new ImportDescription(paths, parameters);

                Util.parseParameters(importDescription, description, parameters, true, paths);

                if (parameters.containsKey("specification-version")) parameters.put("specification-version", VersionRange.parseVersionRange((String) parameters.get("specification-version")));

                if (!parameters.containsKey("version"))
                {
                    if (parameters.containsKey("specification-version"))
                    {
                        parameters.put("version", parameters.get("specification-version"));
                    }
                    else
                    {
                        parameters.put("version", ImportDescription.DEFAULT_VERSION_RANGE);
                    }
                }
                else
                {
                    parameters.put("version", VersionRange.parseVersionRange((String) parameters.get("version")));
                }

                if (parameters.containsKey("specification-version") && !parameters.get("specification-version").equals(parameters.get("version"))) throw new BundleException("version and specification-version do not match");

                if (parameters.containsKey("bundle-version"))
                {
                    parameters.put("bundle-version", VersionRange.parseVersionRange((String) parameters.get("bundle-veriosn")));
                }
                else
                {
                    parameters.put("bundle-version", ImportDescription.DEFAULT_VERSION_RANGE);
                }

                for (String path : paths)
                {
                    if (importedPaths.contains(path))
                    {
                        throw new BundleException("Duplicate import: " + path);
                    }
                    else
                    {
                        importedPaths.add(path);
                    }
                }

                result.add(description);
            }
        }

        return result;
    }

    protected static List<RequireDescription> obtainBundleRequireBundle(Attributes headers) throws BundleException
    {
        List<RequireDescription> result = new ArrayList<RequireDescription>();

        if (headers.containsKey(Constants.REQUIRE_BUNDLE))
        {
            String[] descriptions = Util.split(headers.getValue(Constants.REQUIRE_BUNDLE), ",");
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

                if (parameters.containsKey("bundle-version"))
                {
                    parameters.put("bundle-version", VersionRange.parseVersionRange((String) parameters.get("bundle-verison")));
                }
                else
                {
                    parameters.put("bundle-version", RequireDescription.DEFAULT_VERSION_RANGE);
                }

                result.add(requireDescription);
            }
        }

        return result;
    }

    private static LazyActivationDescription obtainLazyActivationDescription(Attributes headers) throws BundleException
    {
        LazyActivationDescription lazyActivationDescription;

        if (headers.containsKey(Constants.BUNDLE_ACTIVATIONPOLICY))
        {
            lazyActivationDescription = new LazyActivationDescription(true);

            String description = headers.getValue(Constants.BUNDLE_ACTIVATIONPOLICY);
            int index = description.indexOf(';');

            if (index != -1)
            {
                Util.parseLazyActivationDescription(description.substring(index + 1), lazyActivationDescription);
            }
        }
        else
        {
            lazyActivationDescription = new LazyActivationDescription(false);
        }

        return lazyActivationDescription;
    }


    protected static Object parseValue(String expression) throws InvalidSyntaxException
    {
        List<String> values = new ArrayList<String>();
        int pointer = 0;

        if ("*".equals(expression)) return new String[]{ "" };
        expression += ")"; // TODO: this is stupid

        StringBuilder builder = new StringBuilder();
        try
        {
            char c;

            while (isValidValueChar(c = expression.charAt(pointer)))
            {
                switch (c)
                {
                    case '\\':
                    {
                        pointer++;
                        builder.append(expression.charAt(pointer++));
                        break;
                    }
                    case '*':
                    {
                        pointer++;
                        values.add(builder.toString());
                        builder = new StringBuilder();
                        break;
                    }
                    default:
                    {
                        pointer++;
                        builder.append(c);
                    }
                }
            }
        }
        catch (StringIndexOutOfBoundsException e)
        {
            throw new InvalidSyntaxException("Invalid escaping of value", expression);
        }

        values.add(builder.toString());

        if (values.size() == 1)
        {
            return values.get(0);
        }
        else if (values.size() == 2 & values.get(0).length() == 0 && values.get(1).length() == 0)
        {
            return null;
        }
        else
        {
            return values.toArray(new String[values.size()]);
        }
    }

    protected static boolean isValidValueChar(char c)
    {
        return c != ')';
    }
}
