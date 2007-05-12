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

import org.papoose.core.framework.spi.BundleStore;
import org.papoose.core.framework.spi.Store;


/**
 * @version $Revision$ $Date$
 */
public class FileStore implements Store
{
    private final File root;

    public FileStore(File root)
    {
        this.root = root;
    }

    public File getRoot()
    {
        return root;
    }

    public BundleStore allocateBundleStore(long bundleId)
    {
        File bundleRoot = new File(root, "bundles" + File.pathSeparator + bundleId);

        bundleRoot.mkdir();

        return new FileBundleStore(bundleRoot);
    }

    public void removeBundleStore(long bundleId)
    {
        File bundleRoot = new File(root, "bundles" + File.pathSeparator + bundleId);

        if (bundleRoot.exists()) bundleRoot.delete();
    }

    private class FileBundleStore implements BundleStore
    {
        private final File bundleRoot;

        public FileBundleStore(File bundleRoot)
        {
            this.bundleRoot = bundleRoot;
        }

        public File getDataRoot()
        {
            return new File(bundleRoot, "data");
        }

        public File getArchive()
        {
            return new File(bundleRoot, "archive");
        }
    }
}
