/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.papoose.tck.core.store;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.papoose.core.spi.Store;
import org.papoose.core.util.FileUtils;
import org.papoose.store.memory.TmpFileMemoryStore;
import org.papoose.store.test.BaseStoreTest;


/**
 * @version $Revision: $ $Date: $
 */
public class TmpFileMemoryStoreTest extends BaseStoreTest
{
    private File testDirectory;

    /**
     * Tmp file memory stores do not support persistent archive stores
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testLoadArchiveStore() throws Exception
    {
    }

    /**
     * Tmp file memory stores do not support persistent bundle stores
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testBundleStorePersistence() throws Exception
    {
    }

    @Override
    protected Store createStore()
    {
        return new TmpFileMemoryStore(testDirectory);
    }

    @Before
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void setUp() throws Exception
    {
        testDirectory = File.createTempFile("papoose", "test");
        testDirectory.delete();
        testDirectory.mkdir();
    }

    @After
    public void tearDown() throws Exception
    {
        FileUtils.delete(testDirectory);
    }
}
