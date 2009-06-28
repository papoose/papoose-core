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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

import org.papoose.core.spi.ProtectionDomainFactory;


/**
 * @version $Revision$ $Date$
 */
public class ConditionalPermissionAdminImpl implements ConditionalPermissionAdmin, SynchronousBundleListener, ProtectionDomainFactory
{
    private final Map<String, ConditionalPermissionInfo> permissionTable = new HashMap<String, ConditionalPermissionInfo>();
    private Papoose framework;
    private ServiceRegistration registration;
    private ProtectionDomainFactory savedFactory;

    public void start(Papoose framework)
    {
        this.framework = framework;

        this.savedFactory = framework.getBundleManager().getProtectionDomainFactory();
        framework.getBundleManager().setProtectionDomainFactory(this);

        BundleContext context = framework.getSystemBundleContext();

        context.addBundleListener(this);

        this.registration = context.registerService(ConditionalPermissionAdmin.class.getName(), this, null);
    }

    public void stop()
    {
        BundleContext context = framework.getSystemBundleContext();

        registration.unregister();
        context.removeBundleListener(this);

        framework.getBundleManager().setProtectionDomainFactory(savedFactory);

        savedFactory = null;
        registration = null;
        framework = null;
    }

    public ConditionalPermissionInfo addConditionalPermissionInfo(ConditionInfo[] conditionInfos, PermissionInfo[] permissionInfos)
    {
        return null;  //todo: consider this autogenerated code
    }

    public ConditionalPermissionInfo setConditionalPermissionInfo(String name, ConditionInfo[] conditionInfos, PermissionInfo[] permissionInfos)
    {
        return null;  //todo: consider this autogenerated code
    }

    public Enumeration getConditionalPermissionInfos()
    {
        return null;  //todo: consider this autogenerated code
    }

    public ConditionalPermissionInfo getConditionalPermissionInfo(String name)
    {
        return null;  //todo: consider this autogenerated code
    }

    public AccessControlContext getAccessControlContext(String[] signers)
    {
        return null;  //todo: consider this autogenerated code
    }

    public void bundleChanged(BundleEvent event)
    {
        Bundle bundle = event.getBundle();

        if (event.getType() == BundleEvent.INSTALLED)
        {
            URL url = bundle.getEntry("OSGI-INF/permissions.perm");
            if (url != null)
            {
            }
        }
        if (event.getType() == BundleEvent.UPDATED)
        {
            URL url = bundle.getEntry("OSGI-INF/permissions.perm");
            if (url != null)
            {

            }
        }
        if (event.getType() == BundleEvent.UNINSTALLED)
        {

        }

        //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public ProtectionDomain assignProtectionDomain(BundleGeneration bundle, CodeSource codesource, PermissionCollection permissions)
    {
        return null;  //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    private class ConditionalPermissionInfoImpl implements ConditionalPermissionInfo
    {
        private final String name;
        private final ConditionInfo[] conditionInfos;
        private final PermissionInfo[] permissionInfos;

        private ConditionalPermissionInfoImpl(String name, ConditionInfo[] conditionInfos, PermissionInfo[] permissionInfos)
        {
            assert name != null;

            this.name = name;

            this.conditionInfos = new ConditionInfo[conditionInfos.length];
            for (int i = 0; i < conditionInfos.length; i++)
            {
                ConditionInfo from = conditionInfos[i];

                String[] args = new String[from.getArgs().length];
                System.arraycopy(from.getArgs(), 0, args, 0, args.length);

                this.conditionInfos[i] = new ConditionInfo(from.getType(), args);
            }

            this.permissionInfos = new PermissionInfo[permissionInfos.length];
            for (int i = 0; i < permissionInfos.length; i++)
            {
                PermissionInfo from = permissionInfos[i];

                this.permissionInfos[i] = new PermissionInfo(from.getType(), from.getName(), from.getActions());
            }
        }

        public ConditionInfo[] getConditionInfos()
        {
            ConditionInfo[] result = new ConditionInfo[conditionInfos.length];
            System.arraycopy(conditionInfos, 0, result, 0, result.length);

            return result;
        }

        public PermissionInfo[] getPermissionInfos()
        {
            PermissionInfo[] result = new PermissionInfo[permissionInfos.length];
            System.arraycopy(permissionInfos, 0, result, 0, result.length);

            return result;
        }

        public void delete()
        {
            permissionTable.remove(name);
        }

        public String getName()
        {
            return name;
        }
    }

    static private PermissionCollection parsePermissionCollection(URL url) throws IOException
    {
        PermissionCollection collection = new Permissions();

        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        String line;
        while ((line = reader.readLine()) != null)
        {

        }

        return collection;
    }
}
