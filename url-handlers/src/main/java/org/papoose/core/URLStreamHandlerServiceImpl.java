/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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

import java.io.IOException;
import java.net.ContentHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;


/**
 *
 */
public class URLStreamHandlerServiceImpl
{
    private final static String CLASS_NAME = URLStreamHandlerServiceImpl.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final static URLStreamHandlerService URL_STREAM_HANDLER_NOT_FOUND = new URLStreamHandlerNotFound();
    private final static ContentHandler CONTENT_HANDLER_NOT_FOUND = new ContentHandlerNotFound();
    private final static Map<String, Set<UrlStreamHandlerHolder>> urlHandlers = new Hashtable<String, Set<UrlStreamHandlerHolder>>();
    private final static Map<String, Set<ContentHandlerHolder>> contentHandlers = new Hashtable<String, Set<ContentHandlerHolder>>();
    private Papoose framework;

    public void start(Papoose framework)
    {
        LOGGER.entering(CLASS_NAME, "start", framework);

        this.framework = framework;

        try
        {
            URL.setURLStreamHandlerFactory(new URLStreamHandlerFactoryImpl(this));

            LOGGER.fine("URL stream handler factory registered");
        }
        catch (Error e)
        {
            LOGGER.fine("URL stream handler factory already registered, testing factory");

            try
            {
                new URL(SentinelURLStreamHandler.TEST_PROTOCOL, "localhost", "");

                LOGGER.fine("Registered URL stream handler factory belongs to this service");
            }
            catch (MalformedURLException mue)
            {
                throw new FatalError("Unable to register URL stream handler factory");
            }
        }

        try
        {
            URLConnection.setContentHandlerFactory(new ContentHandlerFactoryImpl(this));

            LOGGER.fine("Content handler factory registered");
        }
        catch (Exception e)
        {
            LOGGER.fine("Content handler factory already registered, testing factory");

            try
            {
                URL url = new URL(SentinelURLStreamHandler.TEST_PROTOCOL, "localhost", "");
                url.getContent();

                LOGGER.fine("Registered content handler factory belongs to this service");
            }
            catch (MalformedURLException mue)
            {
                throw new FatalError("Unable to register content handler factory");
            }
            catch (IOException ioe)
            {
                throw new FatalError("Unable to register content handler factory");
            }
        }

        BundleContext systemBundleContext = framework.getSystemBundleContext();

        try
        {
            systemBundleContext.addServiceListener(new ServiceListener()
            {
                public void serviceChanged(ServiceEvent serviceEvent)
                {
                    ServiceReference reference = serviceEvent.getServiceReference();

                    URLStreamHandlerService service = (URLStreamHandlerService) URLStreamHandlerServiceImpl.this.framework.getSystemBundleContext().getService(reference);
                    UrlStreamHandlerHolder holder = new UrlStreamHandlerHolder(service, (Integer) reference.getProperty(Constants.SERVICE_RANKING));

                    String protocol = (String) serviceEvent.getServiceReference().getProperty(URLConstants.URL_HANDLER_PROTOCOL);
                    Set<UrlStreamHandlerHolder> holders = getUrlStreamHandlerHolders(protocol);

                    if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
                    {
                        holders.remove(holder);
                    }
                    else
                    {
                        holders.add(holder);
                    }
                }
            }, "(&(objectClass=" + URLStreamHandlerService.class.getName() + ")(" + URLConstants.URL_HANDLER_PROTOCOL + "=*))");

            systemBundleContext.addServiceListener(new ServiceListener()
            {
                public void serviceChanged(ServiceEvent serviceEvent)
                {
                    ServiceReference reference = serviceEvent.getServiceReference();

                    ContentHandler handler = (ContentHandler) URLStreamHandlerServiceImpl.this.framework.getSystemBundleContext().getService(reference);
                    ContentHandlerHolder holder = new ContentHandlerHolder(handler, (Integer) reference.getProperty(Constants.SERVICE_RANKING));

                    String mimetype = (String) reference.getProperty(URLConstants.URL_CONTENT_MIMETYPE);
                    Set<ContentHandlerHolder> holders = getContentHandlerHolders(mimetype);

                    if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
                    {
                        holders.remove(holder);
                    }
                    else
                    {
                        holders.add(holder);
                    }
                }
            }, "(&(objectClass=" + ContentHandler.class.getName() + ")(" + URLConstants.URL_CONTENT_MIMETYPE + "=*))");
        }
        catch (InvalidSyntaxException ise)
        {
            throw new FatalError("Ugh, no idea why this failed");
        }

        LOGGER.exiting(CLASS_NAME, "start");
    }

    public void stop()
    {
        LOGGER.entering(CLASS_NAME, "stop");

        framework = null;

        LOGGER.exiting(CLASS_NAME, "stop");
    }

    protected URLStreamHandlerService lookupUrlStreamHandler(String protocol)
    {
        LOGGER.entering(CLASS_NAME, "lookupUrlStreamHandler", protocol);

        Set<UrlStreamHandlerHolder> handlerSet = getUrlStreamHandlerHolders(protocol);

        URLStreamHandlerService handler = handlerSet.iterator().next().getService();

        LOGGER.exiting(CLASS_NAME, "lookupUrlStreamHandler", handler);

        return handler;
    }

    protected Set<UrlStreamHandlerHolder> getUrlStreamHandlerHolders(String protocol)
    {
        LOGGER.entering(CLASS_NAME, "getUrlStreamHandlerHolders", protocol);

        synchronized (urlHandlers)
        {
            Set<UrlStreamHandlerHolder> handlerSet = urlHandlers.get(protocol);
            if (handlerSet == null)
            {
                urlHandlers.put(protocol, handlerSet = Collections.synchronizedSet(new TreeSet<UrlStreamHandlerHolder>()));
                handlerSet.add(new UrlStreamHandlerHolder(URL_STREAM_HANDLER_NOT_FOUND));
            }

            LOGGER.exiting(CLASS_NAME, "getUrlStreamHandlerHolders", handlerSet);

            return handlerSet;
        }
    }

    protected ContentHandler lookupContentHandler(String mimetype)
    {
        LOGGER.entering(CLASS_NAME, "lookupContentHandler", mimetype);

        Set<ContentHandlerHolder> handlerSet = getContentHandlerHolders(mimetype);

        ContentHandler handler = handlerSet.iterator().next().getHandler();

        LOGGER.exiting(CLASS_NAME, "lookupContentHandler", handler);

        return handler;
    }

    protected Set<ContentHandlerHolder> getContentHandlerHolders(String mimetype)
    {
        LOGGER.entering(CLASS_NAME, "getContentHandlerHolders", mimetype);

        synchronized (contentHandlers)
        {
            Set<ContentHandlerHolder> handlerSet = contentHandlers.get(mimetype);
            if (handlerSet == null)
            {
                contentHandlers.put(mimetype, handlerSet = Collections.synchronizedSet(new TreeSet<ContentHandlerHolder>()));
                handlerSet.add(new ContentHandlerHolder(CONTENT_HANDLER_NOT_FOUND));
            }

            LOGGER.exiting(CLASS_NAME, "getContentHandlerHolders", handlerSet);

            return handlerSet;
        }
    }

}
