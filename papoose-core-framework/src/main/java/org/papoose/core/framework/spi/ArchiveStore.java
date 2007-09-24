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

import java.io.File;
import java.net.URL;
import java.security.Permission;
import java.util.List;

import org.apache.xbean.classloader.ResourceHandle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import org.papoose.core.framework.ExportDescription;
import org.papoose.core.framework.ImportDescription;

/**
 * @version $Revision$ $Date$
 */
public interface ArchiveStore extends Comparable
{
    File getArchive();

    int getFrameworkId();

    long getBundleId();

    int getGeneration();

    String getBundleActivatorClass();

    String getBundleName();

    Version getBundleVersion();

    List<String> getBundleClassPath();

    List<ExportDescription> getBundleExportList();

    List<ImportDescription> getBundleImportList();

    void refreshClassPath(List<String> classPath) throws BundleException;

    String loadLibrary(String libname);

    Permission[] getPermissionCollection();

    ResourceHandle getResource(String resourceName);

    List<URL> findResources(String resourceName);

    void close();
}
