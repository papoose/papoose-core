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
package org.papoose.core;

import java.util.logging.Logger;
import java.security.ProtectionDomain;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Principal;

import org.osgi.service.condpermadmin.ConditionalPermissionInfo;


/**
 *
 */
public class BundleProtectionDomain extends ProtectionDomain implements PermissionChangeListener
{
    private final static String CLASS_NAME = BundleProtectionDomain.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final BundleGeneration bundleGeneration;

    public BundleProtectionDomain(BundleGeneration bundleGeneration, CodeSource codesource, PermissionCollection permissions)
    {
        super(codesource, permissions);

        this.bundleGeneration = bundleGeneration;
    }

    public BundleProtectionDomain(BundleGeneration bundleGeneration, CodeSource codesource, PermissionCollection permissions, ClassLoader classloader, Principal[] principals)
    {
        super(codesource, permissions, classloader, principals);

        this.bundleGeneration = bundleGeneration;
    }

    public void added(ConditionalPermissionInfo info)
    {
        //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public void replaced(ConditionalPermissionInfo info)
    {
        //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public void removed(ConditionalPermissionInfo info)
    {
        //Todo: change body of implemented methods use File | Settings | File Templates.
    }
}
