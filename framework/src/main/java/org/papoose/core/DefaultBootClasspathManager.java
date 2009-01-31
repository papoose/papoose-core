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
package org.papoose.core;

import org.papoose.core.spi.ArchiveStore;
import org.papoose.core.spi.BootClasspathManager;

/**
 * The default boot classpath manager which does not support adding bundles to
 * the boot classpath.
 *
 * @version $Revision$ $Date$
 */
public class DefaultBootClasspathManager implements BootClasspathManager
{
    /**
     * {@inheritDoc}
     */
    public boolean isSupported()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void add(ArchiveStore archiveStore) throws PapooseException
    {
        throw new PapooseException("Not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void remove(ArchiveStore archiveStore) throws PapooseException
    {
        throw new PapooseException("Not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void markForRestart()
    {
        throw new UnsupportedOperationException("Not supported");
    }
}
