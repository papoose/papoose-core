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

import java.net.URL;
import java.util.List;

import org.osgi.framework.Version;

import org.papoose.core.framework.spi.BundleStore;


/**
 * @version $Revision$ $Date$
 */
class SystemBundleImpl extends BundleImpl
{
    public SystemBundleImpl(ClassLoader classLoader, Papoose framework, BundleStore bundleStore, long bundleId, String bundleActivatorClass, List<String> bundleCategories, List<String> bundleClassPath, String bundleContactAddress, String bundleCopyright, String bundleDescription, String bundleDocUrl, String bundleLocalization, short bundleManifestVersion, String bundleName, List<NativeCodeDescription> bundleNativeCodeList, List<String> bundleExecutionEnvironment, String bundleSymbolicName, URL bundleUpdateLocation, String bundleVendor, Version bundleVersion, List<DynamicDescription> bundleDynamicImportList, List<ExportDescription> bundleExportList, List<String> bundleExportService, FragmentDescription bundleFragmentHost, List<ImportDescription> bundleImportList, List<String> bundleImportService, List<RequireDescription> bundleRequireBundle)
    {
        super(classLoader, framework, bundleStore, bundleId, bundleActivatorClass, bundleCategories, bundleClassPath, bundleContactAddress, bundleCopyright, bundleDescription, bundleDocUrl, bundleLocalization, bundleManifestVersion, bundleName, bundleNativeCodeList, bundleExecutionEnvironment, bundleSymbolicName, bundleUpdateLocation, bundleVendor, bundleVersion, bundleDynamicImportList, bundleExportList, bundleExportService, bundleFragmentHost, bundleImportList, bundleImportService, bundleRequireBundle);
    }
}
