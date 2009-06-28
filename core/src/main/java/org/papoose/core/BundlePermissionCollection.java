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

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.osgi.service.permissionadmin.PermissionInfo;


/**
 * @version $Revision$ $Date$
 */
class BundlePermissionCollection extends PermissionCollection
{
    private final static String CLASS_NAME = BundlePermissionCollection.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);
    private final static Map<String, PermissionInfo> PERMISSION_INFO = new HashMap<String, PermissionInfo>();
    private final static Set<PermissionInfo> DEFAULT_PERMISSION_INFO = new HashSet<PermissionInfo>();
    private final static Map<String, Permissions> PERMISSIONS = new HashMap<String, Permissions>();
    private final static Permissions DEFAULT_PERMISSIONS = new Permissions();
    private final Permissions permissions = new Permissions();
    private final String location;

    BundlePermissionCollection(String location, PermissionCollection permissionCollection)
    {
        assert location != null;

        this.location = location;

        Enumeration<Permission> enumeration = permissionCollection.elements();
        while (enumeration.hasMoreElements())
        {
            permissions.add(enumeration.nextElement());
        }
    }

    public void add(Permission permission)
    {
        if (isReadOnly()) throw new SecurityException("Collection is read only");

        permissions.add(permission);
    }

    public boolean implies(Permission permission)
    {
        Permissions p = PERMISSIONS.get(location);
        if (p == null) p = DEFAULT_PERMISSIONS;

        return p.implies(permission) || permissions.implies(permission);
    }

    public Enumeration<Permission> elements()
    {
        Permissions p = PERMISSIONS.get(location);
        if (p == null) p = DEFAULT_PERMISSIONS;

        return p.elements();
    }

    static Map<String, PermissionInfo> getPermissionInfo()
    {
        return PERMISSION_INFO;
    }

    static Set<PermissionInfo> getDefaultPermissionInfo()
    {
        return DEFAULT_PERMISSION_INFO;
    }

}
