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
package org.papoose.core.framework.spi;

import java.security.Permission;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.Attributes;
import java.net.URL;

import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import org.papoose.core.framework.DynamicDescription;
import org.papoose.core.framework.ExportDescription;
import org.papoose.core.framework.ImportDescription;
import org.papoose.core.framework.L18nResourceBundle;
import org.papoose.core.framework.RequireDescription;

/**
 * @version $Revision$ $Date$
 */
public interface ArchiveStore extends Comparable
{
    String getFrameworkName();

    long getBundleId();

    int getGeneration();

    Attributes getAttributes();

    String getBundleActivatorClass();

    String getBundleSymbolicName();

    Version getBundleVersion();

    List<String> getBundleClassPath();

    List<ExportDescription> getBundleExportList();

    List<ImportDescription> getBundleImportList();

    List<RequireDescription> getBundleRequireBundle();

    Set<DynamicDescription> getDynamicImportSet();

    void refreshClassPath(List<String> classPath) throws BundleException;

    String loadLibrary(String libname);

    ResourceHandle getEntry(String name);

    Enumeration getEntryPaths(String path);

    Enumeration findEntries(String path, String filePattern, boolean recurse);

    ResourceHandle getResource(String resourceName);

    ResourceHandle getResource(String resourceName, int location);

    List<ResourceHandle> findResources(String resourceName);

    L18nResourceBundle getResourceBundle(Locale locale);

    void close();
}
