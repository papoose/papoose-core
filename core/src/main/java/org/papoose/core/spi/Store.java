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

import java.io.InputStream;
import java.util.List;

import org.osgi.framework.BundleException;

import org.papoose.core.Papoose;
import org.papoose.core.PapooseException;


/**
 * @version $Revision$ $Date$
 */
public interface Store
{
    /**
     * Clear the space for use by the store.  When pointing a store at a new
     * space it is usually a good practice to clear the space before using it.
     * <p/>
     * This method will perform what ever steps that are needed to ensure the safe
     * use of the space by this store.
     *
     * @throws PapooseException if an error occurs while clearing this space
     */
    void clear() throws PapooseException;

    /**
     * Notifies the store to start.  This method should only be called once
     * before {@link #stop} is called but should do no harm if it gets called
     * more than that.
     *
     * @throws PapooseException if an error occurs starting
     */
    void start() throws PapooseException;

    /**
     * Notifies the store to stop.  This method should only be called once
     * after {@link #start} is called but should do no harm if it gets called
     * more than that.
     *
     * @throws PapooseException if an error occurs stopping
     */
    void stop() throws PapooseException;

    List<BundleStore> loadBundleStores() throws PapooseException;

    BundleStore obtainSystemBundleStore() throws BundleException;

    BundleStore allocateBundleStore(long bundleId, String location) throws BundleException;

    void removeBundleStore(long bundleId) throws BundleException;

    ArchiveStore allocateArchiveStore(Papoose framework, long bundleId, InputStream inputStream) throws BundleException;

    ArchiveStore loadArchiveStore(Papoose framework, long bundleId) throws BundleException;
}
