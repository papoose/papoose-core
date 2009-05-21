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
package org.papoose.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * Extend this test case if you need to proveide automatic file reclamation
 *
 * @version $Revision$ $Date$
 */
public abstract class PapooseTestCase extends TestCase
{
    private final List<File> resources = new ArrayList<File>();

    /**
     * Register a file resource that should be deleted when the test tears
     * down.  This is a convenience function that cleans up files when a test
     * goes horribly wrong.
     *
     * @param resource the resource to be deleted when the test tears down
     */
    protected void register(File resource)
    {
        if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
        resources.add(resource);
    }

    /**
     * @inheritDoc
     */
    protected final void tearDown() throws Exception
    {
        try
        {
            doTearDown();
        }
        finally
        {
            for (File resource : resources) delete(resource);
            resources.clear();
        }
    }

    /**
     * @throws Exception if something goes wrong with the teardown
     */
    protected void doTearDown() throws Exception
    { }

    private static void delete(File file)
    {
        if (!file.exists()) return;
        if (file.isDirectory())
        {
            for (File f : file.listFiles()) delete(f);
        }
        file.delete();
    }
}
