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

import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.util.logging.Logger;


/**
 *
 */
class ContentHandlerFactoryImpl implements ContentHandlerFactory
{
    private final static String CLASS_NAME = ContentHandlerFactoryImpl.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final URLStreamHandlerServiceImpl owner;

    public ContentHandlerFactoryImpl(URLStreamHandlerServiceImpl owner)
    {
        assert owner != null;

        this.owner = owner;
    }

    public ContentHandler createContentHandler(String mimetype)
    {
        LOGGER.entering(CLASS_NAME, "createContentHandler", mimetype);

        ContentHandler handler = null;
        if (SentinelContentHandler.TEST_CONTENT_TYPE.equals(mimetype))
        {
            handler = new SentinelContentHandler();
        }

        if (handler == null)
        {
            String contentHandlerClassName = typeToPackageName(mimetype);

            String packagePrefixList = System.getProperty("java.content.handler.pkgs");

            if (packagePrefixList.length() != 0) packagePrefixList += "|";

            packagePrefixList += "sun.net.www.content";

            String[] packagePrefixes = packagePrefixList.split("|");

            for (String packagePrefix : packagePrefixes)
            {
                packagePrefix = packagePrefix.trim();

                String className = packagePrefix + "." + contentHandlerClassName;

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
                        handler = (ContentHandler) clazz.newInstance();
                    }
                }
                catch (Exception ignore)
                {
                }
            }
        }

        if (handler == null)
        {
            handler = new ServiceBasedContentHandler(owner, mimetype);
        }

        LOGGER.exiting(CLASS_NAME, "createContentHandler", handler);

        return handler;
    }

    private static String typeToPackageName(String contentType)
    {
        contentType = contentType.toLowerCase();
        int len = contentType.length();
        char name[] = new char[len];
        contentType.getChars(0, len, name, 0);
        for (int i = 0; i < len; i++)
        {
            char c = name[i];
            if (c == '/')
            {
                name[i] = '.';
            }
            else if (!('A' <= c && c <= 'Z' ||
                       'a' <= c && c <= 'z' ||
                       '0' <= c && c <= '9'))
            {
                name[i] = '_';
            }
        }
        return new String(name);
    }
}
