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
import java.util.List;

import org.osgi.framework.BundleException;

import org.papoose.core.framework.ArchiveStore;
import org.papoose.core.framework.Papoose;


/**
 * @version $Revision$ $Date$
 */
public interface Store
{
    List<BundleStore> loadBundleStores() throws BundleException;

    BundleStore allocateBundleStore(long bundleId, String location) throws BundleException;

    BundleStore loadBundleStore(long bundleId) throws BundleException;

    void removeBundleStore(long bundleId) throws BundleException;

    ArchiveStore allocateArchiveStore(Papoose framework, long bundleId, int generaton, InputStream inputStream) throws BundleException;

    List<ArchiveStore> loadArchiveStores(Papoose framework, long bundleId) throws BundleException;

    void removeArchiveStore(long bundleId, int generation) throws BundleException;
}
