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
package org.papoose.core.util;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;

import org.papoose.core.ServiceListenerWithFilter;
import org.papoose.core.spi.TrustManager;


/**
 * @version $Revision$ $Date$
 */
public class SecurityUtils
{
    private final static String CLASS_NAME = SecurityUtils.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static void checkAdminPermission(Bundle bundle, String action) throws SecurityException
    {
        LOGGER.entering(CLASS_NAME, "checkAdminPermission", new Object[]{ bundle, action });

        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            LOGGER.finest("Found security manager");

            sm.checkPermission(new AdminPermission(bundle, action));
        }

        LOGGER.exiting(CLASS_NAME, "checkAdminPermission");
    }

    public static void checkServicePermission(String name, String action) throws SecurityException
    {
        LOGGER.entering(CLASS_NAME, "checkServicePermission", new Object[]{ name, action });

        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            LOGGER.finest("Found security manager");

            sm.checkPermission(new ServicePermission(name, action));
        }

        LOGGER.exiting(CLASS_NAME, "checkServicePermission");
    }

    public static void bundleChanged(final BundleListener listener, final BundleEvent event, AccessControlContext context)
    {
        if (System.getSecurityManager() == null)
        {
            listener.bundleChanged(event);
        }
        else
        {
            AccessController.doPrivileged(new PrivilegedAction<Void>()
            {
                public Void run()
                {
                    listener.bundleChanged(event);
                    return null;
                }
            }, context);
        }
    }

    public static void frameworkEvent(final FrameworkListener listener, final FrameworkEvent event, AccessControlContext context)
    {
        if (System.getSecurityManager() == null)
        {
            listener.frameworkEvent(event);
        }
        else
        {
            AccessController.doPrivileged(new PrivilegedAction<Void>()
            {
                public Void run()
                {
                    listener.frameworkEvent(event);
                    return null;
                }
            }, context);
        }
    }

    public static void serviceEvent(final ServiceListenerWithFilter listener, final ServiceEvent event, AccessControlContext context)
    {
        if (listener.getFilter().match(event.getServiceReference()))
        {
            if (System.getSecurityManager() == null)
            {
                listener.serviceChanged(event);
            }
            else
            {
                AccessController.doPrivileged(new PrivilegedAction<Void>()
                {
                    public Void run()
                    {
                        listener.serviceChanged(event);
                        return null;
                    }
                }, context);
            }
        }
    }

    public static void modifiedServiceEvent(final ServiceListenerWithFilter listener, final ServiceReference reference, Dictionary old, AccessControlContext context)
    {
        Filter filter = listener.getFilter();

        if (filter.match(reference))
        {
            modifiedServiceEvent(listener, new ServiceEvent(ServiceEvent.MODIFIED, reference), context);
        }
        else if (filter.match(old) && !filter.match(reference))
        {
            modifiedServiceEvent(listener, new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH, reference), context);
        }
    }

    public static void modifiedServiceEvent(final ServiceListenerWithFilter listener, final ServiceEvent event, AccessControlContext context)
    {
        if (System.getSecurityManager() == null)
        {
            listener.serviceChanged(event);
        }
        else
        {
            AccessController.doPrivileged(new PrivilegedAction<Void>()
            {
                public Void run()
                {
                    listener.serviceChanged(event);
                    return null;
                }
            }, context);
        }
    }

    public static <T> T doPrivilegedExceptionAction(PrivilegedExceptionAction<T> action, AccessControlContext context) throws Exception
    {
        if (System.getSecurityManager() == null)
        {
            try
            {
                return AccessController.doPrivileged(action, context);
            }
            catch (PrivilegedActionException e)
            {
                throw e.getException();
            }
        }
        else
        {
            return action.run();
        }
    }

    public static Certificate[] getCertificates(byte[] archive, TrustManager trustManager)
    {
        return null; //todo
    }

    public static Certificate[] getCertificates(JarFile archive, TrustManager trustManager)
    {
        int numberOfChainsEncountered = 0;

        // This is tricky: jdk1.3 doesn't say anything about what is happening
        // if a bad sig is detected on an entry - later jdk's do say that they
        // will throw a security Exception. The below should cater for both
        // behaviors.
        List<List<Certificate>> certificateChains = new ArrayList<List<Certificate>>();

        Enumeration<JarEntry> enumeration = archive.entries();
        JarEntry entry;
        while (enumeration.hasMoreElements())
        {
            entry = enumeration.nextElement();

            if (entry.isDirectory() || entry.getName().startsWith("META-INF")) continue;

            Certificate[] certificates = entry.getCertificates();

            // Workaround stupid bug in the sun jdk 1.5.x - getCertificates()
            // returns null there even if there are valid certificates.
            // This is a regression bug that has been fixed in 1.6.
            //
            // We use reflection to see whether we have a SignerCertPath
            // for the entry (available >= 1.5) and if so check whether
            // there are valid certificates - don't try this at home.
            if (certificates == null)
            {
                try
                {
                    CodeSigner[] signers = entry.getCodeSigners();

                    if (signers != null)
                    {
                        List<Certificate> certificateList = new ArrayList<Certificate>();

                        for (CodeSigner signer : signers)
                        {
                            CertPath path = signer.getSignerCertPath();

                            certificateList.addAll(path.getCertificates());
                        }

                        certificates = certificateList.toArray(new Certificate[certificateList.size()]);
                    }
                }
                catch (Exception e)
                {
                    LOGGER.log(Level.WARNING, "Unable to obtain certificates", e);
                }
            }

            if (certificates == null || certificates.length == 0) return null;

            List<List<Certificate>> chains = getRootChains(certificates, trustManager);

            if (certificateChains.isEmpty())
            {
                certificateChains.addAll(chains);
                numberOfChainsEncountered = certificateChains.size();
            }
            else
            {
                for (Iterator<List<Certificate>> iter2 = certificateChains.iterator(); iter2.hasNext();)
                {
                    X509Certificate cert = (X509Certificate) iter2.next().get(0);
                    boolean found = false;
                    for (List<Certificate> chain : chains)
                    {
                        X509Certificate cert2 = (X509Certificate) ((List) chain).get(0);

                        if (cert.getSubjectDN().equals(cert2.getSubjectDN()) && cert.equals(cert2))
                        {
                            found = true;
                            break;
                        }
                    }

                    if (!found) iter2.remove();
                }
            }

            if (certificateChains.isEmpty())
            {
                if (numberOfChainsEncountered > 0) LOGGER.warning("Bad signers for " + archive.getName());

                return null;
            }
        }

        List<Certificate> result = new ArrayList<Certificate>();

        for (List<Certificate> chain : certificateChains)
        {
            result.addAll(chain);
        }

        return result.toArray(new Certificate[result.size()]);
    }

    private static List<List<Certificate>> getRootChains(Certificate[] certificates, TrustManager trustManager)
    {
        List<List<Certificate>> chains = new ArrayList<List<Certificate>>();
        List<Certificate> chain = new ArrayList<Certificate>();

        boolean revoked = false;

        for (int i = 0; i < certificates.length - 1; i++)
        {
            X509Certificate certificate = (X509Certificate) certificates[i];

            if (!revoked && trustManager.revoked(certificate))
            {
                revoked = true;
            }
            else if (!revoked)
            {
                try
                {
                    certificate.checkValidity();

                    chain.add(certificate);
                }
                catch (CertificateException e)
                {
                    LOGGER.log(Level.FINEST, "Certificate is invalid " + certificate, e);

                    revoked = true;
                }
            }

            if (!((X509Certificate) certificates[i + 1]).getSubjectDN().equals(certificate.getIssuerDN()))
            {
                if (!revoked && trustManager.trusted(certificate)) chains.add(chain);

                revoked = false;

                if (!chain.isEmpty()) chain = new ArrayList<Certificate>();
            }
        }

        if (!revoked)
        {
            chain.add(certificates[certificates.length - 1]);

            if (trustManager.trusted(certificates[certificates.length - 1])) chains.add(chain);
        }

        return chains;
    }

    private SecurityUtils()
    {
    }
}
