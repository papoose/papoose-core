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

import java.lang.ref.WeakReference;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.papoose.core.spi.ProtectionDomainFactory;


/**
 *
 */
public class DefaultProtectionDomainFactory implements ProtectionDomainFactory
{
    private final static String CLASS_NAME = DefaultProtectionDomainFactory.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final Map<CodeSource, WeakReference<ProtectionDomain>> domains = new WeakHashMap<CodeSource, WeakReference<ProtectionDomain>>();

    public ProtectionDomain assignProtectionDomain(BundleGeneration bundle, CodeSource codesource, PermissionCollection permissions)
    {
        assert bundle != null;
        assert codesource != null;

        synchronized (domains)
        {
            WeakReference<ProtectionDomain> reference = domains.get(codesource);
            if (reference != null)
            {
                ProtectionDomain pinned = reference.get();
                if (pinned != null)
                {
                    return pinned;
                }
            }

            ProtectionDomain protectionDomain = new InternalProtectionDomain(bundle, codesource, permissions);

            domains.put(codesource, new WeakReference<ProtectionDomain>(protectionDomain));

            return protectionDomain;
        }
    }

    private static class InternalProtectionDomain extends ProtectionDomain
    {
        private final BundleGeneration bundle;

        private InternalProtectionDomain(BundleGeneration bundle, CodeSource codesource, PermissionCollection permissions)
        {
            super(codesource, permissions);

            assert bundle != null;
            assert codesource != null;

            this.bundle = bundle;
        }
    }
}
