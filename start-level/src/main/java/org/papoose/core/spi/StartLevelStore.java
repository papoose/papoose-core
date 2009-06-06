/**
 *
 * Copyright 2009 (C) The original author or authors
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

import org.osgi.framework.Bundle;

import org.papoose.core.Papoose;

/**
 * @version $Revision$ $Date$
 */
public interface StartLevelStore
{
    void start(Papoose framework);

    void stop();

    int getBundleStartLevel(Bundle bundle);

    void setBundleStartLevel(Bundle bundle, int bundleStartLevel);

    void clearBundleStartLevel(Bundle bundle);

    int getInitialBundleStartLevel();

    void setInitialBundleStartLevel(int initialBundleStartLevel);
}
