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

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.logging.Logger;


/**
 * @version $Revision$ $Date$
 */
class URLStreamHandlerFactoryImpl implements URLStreamHandlerFactory
{
    private final static String CLASS_NAME = URLStreamHandlerFactoryImpl.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final URLStreamHandlerServiceImpl owner;

    public URLStreamHandlerFactoryImpl(URLStreamHandlerServiceImpl owner)
    {
        assert owner != null;

        this.owner = owner;
    }

    public URLStreamHandler createURLStreamHandler(String protocol)
    {
        LOGGER.entering(CLASS_NAME, "createURLStreamHandler", protocol);

        URLStreamHandler handler = null;
        if (SentinelURLStreamHandler.TEST_PROTOCOL.equals(protocol))
        {
            handler = new SentinelURLStreamHandler();
        }

        if (handler == null)
        {
            String packagePrefixList = System.getProperty("java.protocol.handler.pkgs");

            if (packagePrefixList.length() != 0) packagePrefixList += "|";

            packagePrefixList += "sun.net.www.protocol";

            String[] packagePrefixes = packagePrefixList.split("|");

            for (String packagePrefix : packagePrefixes)
            {
                packagePrefix = packagePrefix.trim();

                String className = packagePrefix + "." + protocol + ".Handler";

                try
                {
                    Class clazz = null;
                    try
                    {
                        clazz = Class.forName(className);
                    }
                    catch (ClassNotFoundException e)
                    {
                        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                        if (classLoader != null)
                        {
                            clazz = classLoader.loadClass(className);
                        }
                    }

                    if (clazz != null)
                    {
                        handler = (URLStreamHandler) clazz.newInstance();
                    }
                }
                catch (Exception e)
                {
                }
            }
        }

        if (handler == null)
        {
            handler = new ServiceBasedURLStreamHandler(owner, protocol);
        }

        LOGGER.exiting(CLASS_NAME, "createURLStreamHandler", handler);

        return handler;
    }
}
