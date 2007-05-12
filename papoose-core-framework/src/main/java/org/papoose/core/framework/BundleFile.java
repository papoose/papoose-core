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
import java.net.URI;


/**
 * @version $Revision$ $Date$
 */
public class BundleFile extends File
{
    public BundleFile(String pathname)
    {
        super(pathname);
    }

    public BundleFile(String parent, String child)
    {
        super(parent, child);
    }

    public BundleFile(File parent, String child)
    {
        super(parent, child);
    }

    public BundleFile(URI uri)
    {
        super(uri);
    }
}
