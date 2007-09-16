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

import java.io.File;
import java.util.Properties;

import org.papoose.core.framework.mock.MockThreadPool;
import org.papoose.core.framework.spi.ArchiveStore;
import org.papoose.test.PapooseTestCase;

/**
 * @version $Revision$ $Date$
 */
public class FileStoreTest extends PapooseTestCase
{
    public void test() throws Exception
    {
        File fileStoreRoot = new File("./target/store");
        try
        {
            FileStore fileStore = new FileStore(fileStoreRoot);
            Papoose papoose = new Papoose(fileStore, new MockThreadPool(), new Properties());
            File testBundle = new File("./target/bundle.jar");

            ArchiveStore archiveStore = fileStore.allocateArchiveStore(papoose, 1, 0, testBundle.toURL().openStream());

            String name = archiveStore.getBundleName();
            String activator = archiveStore.getBundleActivatorClass();

            fileStore.removeBundleStore(1);
        }
        finally
        {
            Util.delete(fileStoreRoot);
        }
    }
}
