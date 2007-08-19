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
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.SortedSet;
import java.security.Permission;

import org.osgi.framework.BundleException;
import org.apache.xbean.classloader.ResourceHandle;

import org.papoose.core.framework.NativeCodeDescription;


/**
 * @version $Revision$ $Date$
 */
public interface ArchiveStore
{
    File getArchive();

    /**
     * Set the native code descriptions that the bundle store is to use
     * when loading native code libraries.
     *
     * @param nativeCodeDescriptions the sorted set of native code descriptions
     * @throws org.osgi.framework.BundleException
     *          if the set of native code descriptions is empty
     */
    void setNativeCodeDescriptions(SortedSet<NativeCodeDescription> nativeCodeDescriptions) throws BundleException;

    String loadLibrary(String libname);

    Permission[] getPermissionCollection();

    ResourceHandle getResource(String resourceName);
}
