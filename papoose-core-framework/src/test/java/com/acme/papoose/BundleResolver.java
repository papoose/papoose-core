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
package com.acme.papoose;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.papoose.core.framework.BundleImpl;
import org.papoose.core.framework.ExportDescription;
import org.papoose.core.framework.Wire;


/**
 * Simple class to check package access.
 *
 * @version $Revision$ $Date$
 */
public class BundleResolver extends org.papoose.core.framework.BundleResolver
{
    protected Set<Candidate> collectUsed(List<String> uses, BundleImpl bundle)
    {
        Set<Candidate> result = new HashSet<Candidate>();

        nextPackage:
        for (String packageName : uses)
        {
            for (Wire wire : bundle.getClassLoader().getWires())
            {
                if (wire.getPackageName().equals(packageName))
                {
                    for (ExportDescription exportDescription : wire.getBundle().getBundleExportList())
                    {
                        if (exportDescription.getPackages().contains(packageName))
                        {
                            result.addAll(collectUsed(exportDescription.getUses(), wire.getBundle()));
                            result.add(new Candidate(packageName, exportDescription, wire.getBundle()));

                            continue nextPackage;
                        }
                    }
                }
            }
        }
        return result;
    }
}
