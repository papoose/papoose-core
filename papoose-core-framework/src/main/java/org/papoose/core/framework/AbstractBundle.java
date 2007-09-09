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

import java.util.List;

import org.osgi.framework.Bundle;

import org.papoose.core.framework.spi.BundleStore;


/**
 * @version $Revision$ $Date$
 */
abstract class AbstractBundle implements Bundle
{
    protected final long bundleId;
    private final BundleStore bundleStore;
    private final ArchiveStore archiveStore;

    protected AbstractBundle(long bundleId, BundleStore bundleStore, ArchiveStore archiveStore)
    {
        this.bundleId = bundleId;
        this.bundleStore = bundleStore;
        this.archiveStore = archiveStore;
    }

    public long getBundleId()
    {
        return bundleId;
    }

    public List<ExportDescription> getBundleExportList()
    {
        return archiveStore.getBundleExportList();
    }

    BundleStore getBundleStore()
    {
        return bundleStore;
    }

    ArchiveStore getArchiveStore()
    {
        return archiveStore;
    }

    void markInstalled()
    {
        archiveStore.markInstalled();
    }

    abstract class State implements org.osgi.framework.Bundle
    {
    }
}
