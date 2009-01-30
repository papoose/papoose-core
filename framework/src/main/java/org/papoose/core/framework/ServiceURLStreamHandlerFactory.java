/**
 *
 * Copyright 2007-2009 (C) The original author or authors
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

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;


/**
 * @version $Revision$ $Date$
 */
class ServiceURLStreamHandlerFactory implements URLStreamHandlerFactory
{
    private static final String PROTOCOL_PATH_PROPERTY = "java.protocol.handler.pkgs";
    private static final String PAPOOSE_PREFIX = "org.papoose.core.framework.protocols";
    private static final String SUN_PREFIX = "sun.net.www.protocol";

    public URLStreamHandler createURLStreamHandler(String protocol)
    {
        String packagePrefixList = PAPOOSE_PREFIX + "|";

        packagePrefixList += AccessController.doPrivileged(new PrivilegedAction<String>()
        {
            public String run()
            {
                String prefixList = System.getProperty(PROTOCOL_PATH_PROPERTY, "");
                if (!prefixList.equals("")) prefixList += "|";
                prefixList += SUN_PREFIX;
                return prefixList;
            }
        });

        URLStreamHandler handler = null;

        for (String packagePrefix : packagePrefixList.split("\\|"))
        {
            packagePrefix = packagePrefix.trim();

            try
            {
                String clsName = packagePrefix + "." + protocol + ".Handler";
                Class clazz = null;
                try
                {
                    clazz = Class.forName(clsName);
                }
                catch (ClassNotFoundException e)
                {
                    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                    if (classLoader != null) clazz = classLoader.loadClass(clsName);
                }
                if (clazz != null) return (URLStreamHandler) clazz.newInstance();
            }
            catch (Exception ignored)
            {
            }
        }
        throw new InternalError("could not load " + protocol + "system protocol handler");
    }
}