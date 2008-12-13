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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.papoose.core.framework.protocols.bundle.BundleUrlConnection;
import org.papoose.core.framework.protocols.codesource.CodesourceUrlConnection;
import org.papoose.core.framework.protocols.resource.ResourceUrlConnection;


/**
 * Miscellaneous URL utility methods. For internal use within the framework and
 * the URL Handlers Service.
 * <p/>
 * This class is not in the usual utils package because it needs package level
 * access to the framework's package protected methods.
 *
 * @version $Revision$ $Date$
 */
public class UrlUtils
{
    private final static String CLASS_NAME = UrlUtils.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    /**
     * Generate a code source URL used when loading classes from a particular Bundle.
     * We could have used the Bundle location but we cannot guarantee that the
     * location will be a valid URL.
     * <p/>
     * This URL can be used to access the JAR that was loaded.
     * <p/>
     * <code>codesource://bundle@framework</code>
     *
     * @param frameworkName the name of the particular framework instance
     * @param bundleId      the id of the Bundle
     * @return a URL that represents a particular Bundle's code source.
     */
    public static URL generateCodeSourceUrl(String frameworkName, long bundleId)
    {
        URL result = null;
        try
        {
            result = new URL("codesource://" + Long.toString(bundleId) + "@" + frameworkName);
        }
        catch (MalformedURLException mue)
        {
            LOGGER.log(Level.WARNING, "Unable to generate bundle code source URL", mue);
        }
        return result;
    }

    /**
     * Generate a URL that references a resource within a ResourceHandle inside a Bundle.
     * <p/>
     * <code>bundle://bundle:generation@framework/file</code>
     *
     * @param frameworkName the name of the particular framework instance
     * @param bundleId      the id of the Bundle
     * @param path          the path inside the resource
     * @param generation    the generation of the the Bundle
     * @return a URL that can be used to reference within a resource inside a particular Bundle
     * @see org.apache.xbean.classloader.ResourceHandle
     */
    public static URL generateEntryUrl(String frameworkName, long bundleId, String path, int generation, int location)
    {
        if (path.length() == 0 || path.charAt(0) != '/') path = "/" + path;

        URL result = null;
        try
        {
            result = new URL("bundle://" + Long.toString(bundleId) + ":" + location + "@" + frameworkName + path);
        }
        catch (MalformedURLException mue)
        {
            LOGGER.log(Level.WARNING, "Unable to generate bundle resource URL", mue);
        }
        return result;
    }

    /**
     * Generate a URL that references a resource within a ResourceHandle inside a Bundle.
     * <p/>
     * <code>resource://bundle:generation@framework:location/file</code>
     *
     * @param frameworkName the name of the particular framework instance
     * @param bundleId      the id of the Bundle
     * @param path          the path inside the resource
     * @param generation    the generation of the the Bundle
     * @param location      the location of the <code>ResourceHandle</code> within the Bundle
     * @return a URL that can be used to reference within a resource inside a particular Bundle
     * @see org.apache.xbean.classloader.ResourceHandle
     */
    public static URL generateResourceUrl(String frameworkName, long bundleId, String path, int generation, int location)
    {
        if (path.length() == 0 || path.charAt(0) != '/') path = "/" + path;

        URL result = null;
        try
        {
            result = new URL("resource://" + Long.toString(bundleId) + "@" + frameworkName + ":" + location + path);
        }
        catch (MalformedURLException mue)
        {
            LOGGER.log(Level.WARNING, "Unable to generate bundle resource URL", mue);
        }
        return result;
    }

    public static URLConnection allocateCodesourceConnection(URL url) throws IOException
    {
        try
        {
            Papoose framework = Papoose.getFramework(url.getHost());

            if (framework == null) throw new MalformedURLException("Invalid format");

            int bundleId = Integer.parseInt(url.getUserInfo());

            if (bundleId < 0) throw new MalformedURLException("Invalid format");

            return new CodesourceUrlConnection(url, framework.getBundleManager(), bundleId);
        }
        catch (NumberFormatException nfe)
        {
            LOGGER.log(Level.WARNING, "Unable to allocate code source URL connection", nfe);
        }

        throw new MalformedURLException("Invalid format");
    }

    public static URLConnection allocateBundleConnection(URL url) throws IOException
    {
        try
        {
            Papoose framework = Papoose.getFramework(url.getHost());

            if (framework == null) throw new MalformedURLException("Invalid format");

            String userInfo = url.getUserInfo();

            if (userInfo == null) throw new MalformedURLException("Invalid format");

            int bundleId = Integer.parseInt(userInfo);

            if (bundleId < 0) throw new MalformedURLException("Invalid format");

            int generation = url.getPort();

            return new BundleUrlConnection(url, framework.getBundleManager(), bundleId, generation);
        }
        catch (NumberFormatException nfe)
        {
            LOGGER.log(Level.WARNING, "Unable to allocate bundle entry URL connection", nfe);
        }

        throw new MalformedURLException("Invalid format");
    }

    public static URLConnection allocateResourceConnection(URL url) throws IOException
    {
        try
        {
            Papoose framework = Papoose.getFramework(url.getHost());

            if (framework == null) throw new MalformedURLException("Invalid format");

            String userInfo = url.getUserInfo();

            if (userInfo == null) throw new MalformedURLException("Invalid format");

            String[] parts = userInfo.trim().split(":");

            if (parts.length != 2) throw new MalformedURLException("Invalid format");

            int bundleId = Integer.parseInt(parts[0]);

            if (bundleId < 0) throw new MalformedURLException("Invalid format");

            int location = Integer.parseInt(parts[1]);

            if (location < 0) throw new MalformedURLException("Invalid format");

            int generation = url.getPort();

            return new ResourceUrlConnection(url, framework.getBundleManager(), bundleId, generation, location);
        }
        catch (NumberFormatException nfe)
        {
            LOGGER.log(Level.WARNING, "Unable to allocate bundle resource URL connection", nfe);
        }

        throw new MalformedURLException("Invalid format");
    }

    private UrlUtils() { }
}
