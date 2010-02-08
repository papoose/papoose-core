/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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
package org.papoose.core.mock;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import org.papoose.core.Papoose;
import org.papoose.core.PapooseException;
import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.spi.BundleStore;
import org.papoose.core.spi.Store;


/**
 * @version $Revision$ $Date$
 */
public class MockStore implements Store
{
    public boolean isPreviouslyUsed() throws PapooseException
    {
        return false;
    }

    public void clear() throws PapooseException
    {
    }

    public void start() throws PapooseException
    {
    }

    public void stop() throws PapooseException
    {
    }

    public List<BundleStore> loadBundleStores() throws PapooseException
    {
        return Collections.EMPTY_LIST;
    }

    public BundleStore obtainSystemBundleStore() throws BundleException
    {
        return new MockBundleStore(0, Constants.SYSTEM_BUNDLE_LOCATION);
    }

    public BundleStore allocateBundleStore(long bundleId, String location) throws BundleException
    {
        return new MockBundleStore(bundleId, location);
    }

    public void removeBundleStore(long bundleId) throws BundleException
    {
        int i = 0;
        //todo: consider this autogenerated code
    }

    public ArchiveStore allocateArchiveStore(Papoose framework, long bundleId, InputStream inputStream) throws BundleException
    {
        return new MockArchiveStore();
    }

    public ArchiveStore loadArchiveStore(Papoose framework, long bundleId) throws BundleException
    {
        return null;  //todo: consider this autogenerated code
    }
}