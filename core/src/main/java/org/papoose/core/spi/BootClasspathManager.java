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
package org.papoose.core.spi;

import org.papoose.core.PapooseException;

/**
 * Maps a bundle location string to an <code>InputStream</code> which is used
 * to load a bundle.
 * <p/>
 * A custom location mapper can be designated by passing one in the properties
 * object of the <code>Papoose</code> constructor.  Use the key
 * <code>org.papoose.core.framework.spi.BootClasspathManager</code>.
 * <p/>
 * Note to implementers: Do not assume that archive store that was passed to
 * <code>add()</code> is the same instance that is passed to
 * <code>remove()</code>.
 *
 * @version $Revision$ $Date$
 * @see org.papoose.core.DefaultBootClasspathManager
 * @see org.papoose.core.Papoose
 */
public interface BootClasspathManager
{
    public static final String BOOT_CLASSPATH_MANAGER = BootClasspathManager.class.getName();

    /**
     * Obtain the boot classpath manager's capabilities, i.e. if it supports
     * boot classpath management.
     *
     * @return true if boot classpath management is supported, false otherwise
     */
    boolean isSupported();

    /**
     * Add an archive store to the boot classpath.  This archive store will
     * not be available on boot classpath until after the JVM that the
     * framework is on restarts.  Call <code>markForRestart</code> to request
     * that the manager restart the JVM when the Papoose framework shuts down.
     *
     * @param archiveStore the archive store to add to the boot classpath
     * @throws PapooseException if the archive cannot be added to the boot classpath
     * @see #markForRestart
     */
    void add(ArchiveStore archiveStore) throws PapooseException;

    /**
     * Remove an archive store from the boot classpath.  This archive store
     * will remain on the boot classpath, if it was previously available, until
     * after the JVM that the framework is on restarts.  Call
     * <code>markForRestart</code> to request that the manager restart the JVM
     * when the Papoose framework shuts down.
     *
     * @param archiveStore the archive store to be removed from the boot classpath
     * @throws PapooseException if the archive cannot be removed from the boot classpath
     */
    void remove(ArchiveStore archiveStore) throws PapooseException;

    /**
     * Request that the boot classpath manager restarts the JVM when the
     * Papoose framework shuts down.
     */
    void markForRestart();
}
