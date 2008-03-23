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
package org.papoose.core.framework.spi;

import java.io.IOException;
import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
public interface BundleManager
{
    void setStartManager(StartManager startManager);

    InputStream getInputStream(int bundleId, int generation) throws IOException;

    void recordBundleHasStarted(Bundle bundle);

    Bundle getBundle(long bundleId);

    Bundle getBundle(String symbolicName);

    Bundle[] getBundles();

    Bundle installSystemBundle(Version version) throws BundleException;

    Bundle installBundle(String location, InputStream inputStream) throws BundleException;

    boolean resolve(Bundle bundle);

    void requestStart(Bundle bundle);

    void performStart(Bundle bundle);

    void stop(Bundle bundle);

    void uninstall(Bundle bundle);

    void fireBundleEvent(BundleEvent event);

    void fireFrameworkEvent(FrameworkEvent event);

    void fireServiceEvent(ServiceEvent event);

    void readLock() throws InterruptedException;

    void readUnlock();

    void writeLock() throws InterruptedException;

    void writeUnlock();
}
