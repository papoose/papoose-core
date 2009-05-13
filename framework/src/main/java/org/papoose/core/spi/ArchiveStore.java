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
package org.papoose.core.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.jar.Attributes;

import org.apache.xbean.classloader.ResourceLocation;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

import org.papoose.core.FragmentAttachment;
import org.papoose.core.L18nResourceBundle;
import org.papoose.core.descriptions.DynamicDescription;
import org.papoose.core.descriptions.ExportDescription;
import org.papoose.core.descriptions.FragmentDescription;
import org.papoose.core.descriptions.ImportDescription;
import org.papoose.core.descriptions.LazyActivationDescription;
import org.papoose.core.descriptions.NativeCodeDescription;
import org.papoose.core.descriptions.RequireDescription;


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

    boolean isSingleton();

    FragmentAttachment getFragmentAttachment();

    Version getBundleVersion();

    List<String> getBundleClassPath();

    List<NativeCodeDescription> getBundleNativeCodeList();

    List<String> getBundleRequiredExecutionEnvironment();

    List<ExportDescription> getExportDescriptions();

    List<ImportDescription> getImportDescriptions();

    List<RequireDescription> getRequireDescriptions();

    List<DynamicDescription> getDynamicDescriptions();

    FragmentDescription getFragmentDescription();

    ResourceLocation registerClassPathElement(String classPathElement) throws BundleException;

    LazyActivationDescription getLazyActivationDescription();

    boolean isLazyActivationPolicy();

    String loadLibrary(String libname);

    Enumeration<URL> findEntries(String path, String filePattern, boolean includeDirectory, boolean recurse);

    L18nResourceBundle getResourceBundle(Locale locale);

    InputStream getInputStreamForCodeSource() throws IOException;

    InputStream getInputStreamForEntry(String path) throws IOException;

    InputStream getInputStreamForResource(int location, String path) throws IOException;

    public Certificate[] getCertificates();

    /**
     * Set the native code descriptions that the bundle store is to use
     * when loading native code libraries.
     *
     * @param nativeCodeDescriptions the sorted set of native code descriptions
     * @throws BundleException if any entry in the set does not have a physical library
     */
    void assignNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException;

    void close();
}
