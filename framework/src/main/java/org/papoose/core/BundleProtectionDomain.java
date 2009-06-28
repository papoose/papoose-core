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

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.osgi.service.condpermadmin.ConditionalPermissionInfo;


/**
 * Papoose protection domain used to support conditional admin service.
 *
 * @version $Revision$ $Date$
 */
public class BundleProtectionDomain extends ProtectionDomain
{
    private final static String CLASS_NAME = BundleProtectionDomain.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final BundleGeneration bundle;

    public BundleProtectionDomain(CodeSource codesource, PermissionCollection permissions, BundleGeneration bundle)
    {
        super(codesource, permissions);

        assert bundle != null;
        this.bundle = bundle;
    }

    public BundleProtectionDomain(CodeSource codesource, PermissionCollection permissions, ClassLoader classloader, Principal[] principals, BundleGeneration bundle)
    {
        super(codesource, permissions, classloader, principals);

        assert bundle != null;
        this.bundle = bundle;
    }
}
