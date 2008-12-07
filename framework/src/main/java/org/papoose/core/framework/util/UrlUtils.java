/**
 *
 * Copyright 2008 (C) The original author or authors
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
package org.papoose.core.framework.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Miscellaneous URL utility methods. For internal use within the framework and
 * the URL Handlers Service.
 *
 * @version $Revision$ $Date$
 */
public class UrlUtils
{
    private final static String CLASS_NAME = UrlUtils.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    /**
     * Generate a URL that references a resource within a ResourceHandle inside a Bundle.
     *
     * @param frameworkName the name of the particular framework instance
     * @param bundleId      the id of the Bundle
     * @param path          the path inside the resource
     * @param location      the location of the <code>ResourceHandle</code> within the Bundle
     * @return a URL that can be used to reference within a resource inside a particular Bundle
     * @see org.apache.xbean.classloader.ResourceHandle
     */
    public static URL generateResourceUrl(String frameworkName, long bundleId, String path, int location)
    {
        if (path.length() == 0 || path.charAt(0) != '/') path = "/" + path;

        URL result = null;
        try
        {
            result = new URL("bundle://" + Long.toString(bundleId) + "@" + frameworkName + ":" + location + path);
        }
        catch (MalformedURLException e)
        {
            LOGGER.log(Level.WARNING, "Unable to generate bundle resource URL", e);
        }
        return result;
    }

    /**
     * Generate a code source URL used when loading classes from a particular Bundle
     * @param frameworkName the name of the particular framework instance
     * @param bundleId the id of the Bundle
     * @return a URL that represents a particular Bundle's code source.
     */
    public static URL generateCodeSourceUrl(String frameworkName, long bundleId)
    {
        URL result = null;
        try
        {
            result = new URL("codesource://" + Long.toString(bundleId) + "@" + frameworkName);
        }
        catch (MalformedURLException e)
        {
            LOGGER.log(Level.WARNING, "Unable to generate bundle code source URL", e);
        }
        return result;
    }

    private UrlUtils() { }
}
