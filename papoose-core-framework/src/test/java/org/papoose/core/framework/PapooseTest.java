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

import java.io.InputStream;
import java.util.Properties;
import java.util.List;

import junit.framework.TestCase;

import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.Store;
import org.papoose.core.framework.spi.ThreadPool;
import org.papoose.core.framework.AbstractStore;

import org.osgi.framework.BundleException;


/**
 * @version $Revision$ $Date$
 */
public class PapooseTest extends TestCase
{
    public void test()
    {
        Papoose poo = new Papoose(new MockStore(), new MockThreadPool(), new Properties());
        poo.start();
        poo.stop();
    }

    static class MockStore implements Store
    {
        public List<BundleStore> loadBundleStores() throws BundleException
        {
            return null;  //todo: consider this autogenerated code
        }

        public BundleStore allocateBundleStore(long bundleId, String location) throws BundleException
        {
            return null;  //todo: consider this autogenerated code
        }

        public BundleStore loadBundleStore(long bundleId) throws BundleException
        {
            return null;  //todo: consider this autogenerated code
        }

        public void removeBundleStore(long bundleId) throws BundleException
        {
            //todo: consider this autogenerated code
        }

        public AbstractStore allocateArchiveStore(Papoose framework, long bundleId, int generaton, InputStream inputStream) throws BundleException
        {
            return null;  //todo: consider this autogenerated code
        }

        public List<AbstractStore> loadArchiveStores(Papoose framework, long bundleId) throws BundleException
        {
            return null;  //todo: consider this autogenerated code
        }

        public void removeArchiveStore(long bundleId, int generation) throws BundleException
        {
            //todo: consider this autogenerated code
        }
    }

    static class MockThreadPool implements ThreadPool
    {
        public boolean runInThread(Runnable runnable)
        {
            return false;  //todo: consider this autogenerated code
        }
    }
}
