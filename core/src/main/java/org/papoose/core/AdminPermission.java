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

import java.security.AccessController;
import java.security.BasicPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Indicates the caller's authority to perform specific privileged
 * administrative operations on or to get sensitive information about a bundle.
 * The actions for this permission are:
 * <p/>
 * <pre>
 *  Action               Methods
 *  class                Bundle.loadClass
 *  execute              Bundle.start
 *                       Bundle.stop
 *                       StartLevel.setBundleStartLevel
 *  extensionLifecycle   BundleContext.installBundle for extension bundles
 *                       Bundle.update for extension bundles
 *                       Bundle.uninstall for extension bundles
 *  lifecycle            BundleContext.installBundle
 *                       Bundle.update
 *                       Bundle.uninstall
 *  listener             BundleContext.addBundleListener for SynchronousBundleListener
 *                       BundleContext.removeBundleListener for SynchronousBundleListener
 *  metadata             Bundle.getHeaders
 *                       Bundle.getLocation
 *  resolve              PackageAdmin.refreshPackages
 *                       PackageAdmin.resolveBundles
 *  resource             Bundle.getResource
 *                       Bundle.getResources
 *                       Bundle.getEntry
 *                       Bundle.getEntryPaths
 *                       Bundle.findEntries
 *                       Bundle resource/entry URL creation
 *  startlevel           StartLevel.setStartLevel
 *                       StartLevel.setInitialBundleStartLevel
 * </pre>
 * <p/>
 * <p/>
 * The special action "*" will represent all actions.
 * <p/>
 * The name of this permission is a filter expression. The filter gives access
 * to the following parameters:
 * <ul>
 * <li>signer - A Distinguished Name chain used to sign a bundle. Wildcards in
 * a DN are not matched according to the filter string rules, but according to
 * the rules defined for a DN chain.</li>
 * <li>location - The location of a bundle.</li>
 * <li>id - The bundle ID of the designated bundle.</li>
 * <li>name - The symbolic name of a bundle.</li>
 * </ul>
 *
 */
public final class AdminPermission extends BasicPermission
{
    public final static String CLASS = "class";
    public final static String EXECUTE = "execute";
    public final static String EXTENSIONLIFECYCLE = "extensionLifecycle";
    public final static String LIFECYCLE = "lifecycle";
    public final static String LISTENER = "listener";
    public final static String METADATA = "metadata";
    public final static String RESOLVE = "resolve";
    public final static String RESOURCE = "resource";
    public final static String STARTLEVEL = "startlevel";

    private final static int CLASS_BIT = 0x01;
    private final static int EXECUTE_BIT = 0x02;
    private final static int EXTENSIONLIFECYCLE_BIT = 0x04;
    private final static int LIFECYCLE_BIT = 0x08;
    private final static int LISTENER_BIT = 0x10;
    private final static int METADATA_BIT = 0x11;
    private final static int RESOLVE_BIT = 0x12;
    private final static int RESOURCE_BIT = 0x14;
    private final static int STARTLEVEL_BIT = 0x18;
    private final static int ALL_BIT = 0x1F;

    private final short actions;
    private final Object target;

    public AdminPermission()
    {
        this("(id=*)", "*");
    }

    /**
     * Create a new AdminPermission.
     * <p/>
     * This constructor must only be used to create a permission that is going
     * to be checked.
     * <p/>
     * Examples:
     * <p/>
     * <pre>
     * (signer=\*,o=ACME,c=US)
     * (&amp;(signer=\*,o=ACME,c=US)(name=com.acme.*)(location=http://www.acme.com/bundles/*))
     * (id&gt;=1)
     * </pre>
     * <p/>
     * <p/>
     * When a signer key is used within the filter expression the signer value
     * must escape the special filter chars ('*', '(', ')').
     * <p/>
     * Null arguments are equivalent to "*".
     *
     * @param filter  A filter expression that can use signer, location, id, and
     *                name keys. A value of &quot;*&quot; or <code>null</code> matches
     *                all bundle.
     * @param actions <code>class</code>, <code>execute</code>,
     *                <code>extensionLifecycle</code>, <code>lifecycle</code>,
     *                <code>listener</code>, <code>metadata</code>,
     *                <code>resolve</code>, <code>resource</code>, or
     *                <code>startlevel</code>. A value of "*" or <code>null</code>
     *                indicates all actions
     */
    public AdminPermission(String filter, String actions)
    {
        super(filter == null ? "(id=*)" : ("*".equals(filter) ? "(id=*)" : filter));

        this.actions = convertActions(actions);

        Object temp;
        try
        {
            temp = FrameworkUtil.createFilter(getName());
        }
        catch (InvalidSyntaxException e)
        {
            temp = DefaultFilter.FALSE;
        }
        this.target = temp;
    }

    /**
     * Creates a new <code>AdminPermission</code> object to be used by the
     * code that must check a <code>Permission</code> object.
     *
     * @param bundle  A bundle
     * @param actions <code>class</code>, <code>execute</code>,
     *                <code>extensionLifecycle</code>, <code>lifecycle</code>,
     *                <code>listener</code>, <code>metadata</code>,
     *                <code>resolve</code>, <code>resource</code>,
     *                <code>startlevel</code>
     * @since 1.3
     */
    public AdminPermission(Bundle bundle, String actions)
    {
        super("(id=" + bundle.getBundleId() + ")");

        this.actions = convertActions(actions);

        final Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        this.target = dictionary;

        final BundleController bundleController = (BundleController) bundle;
        if (System.getSecurityManager() != null)
        {
            AccessController.doPrivileged(new PrivilegedAction<Void>()
            {
                public Void run()
                {
                    dictionary.put("signer", obtainSigner(bundleController));
                    return null;
                }
            });
        }
        else
        {
            dictionary.put("signer", obtainSigner(bundleController));
        }
        dictionary.put("id", bundle.getBundleId());
        dictionary.put("location", bundleController.getBundleStore().getLocation());
        dictionary.put("name", bundle.getSymbolicName());
    }

    private static Object obtainSigner(BundleController bundleController)
    {
        return new Object();  //Todo: change body of created methods use File | Settings | File Templates.
    }

    private static short convertActions(String actions)
    {
        if (actions == null) return ALL_BIT;
        if ("*".equals(actions)) return ALL_BIT;

        short result = 0;

        String tokens[] = actions.split(",");

        for (String token : tokens)
        {
            token = token.trim();

            if (CLASS.equals(token))
            {
                result |= CLASS_BIT;
            }
            else if (EXECUTE.equals(token))
            {
                result |= EXECUTE_BIT;
            }
            else if (EXTENSIONLIFECYCLE.equals(token))
            {
                result |= EXTENSIONLIFECYCLE_BIT;
            }
            if (LIFECYCLE.equals(token))
            {
                result |= LIFECYCLE_BIT;
            }
            else if (LISTENER.equals(token))
            {
                result |= LISTENER_BIT;
            }
            else if (METADATA.equals(token))
            {
                result |= METADATA_BIT;
            }
            else if (RESOLVE.equals(token))
            {
                result |= RESOLVE_BIT;
            }
            else if (RESOURCE.equals(token))
            {
                result |= RESOURCE_BIT;
            }
            else if (STARTLEVEL.equals(token))
            {
                result |= STARTLEVEL_BIT;
            }
        }

        return result;
    }

    /**
     * Determines the equality of two <code>AdminPermission</code> objects.
     *
     * @param obj The object being compared for equality with this object.
     * @return <code>true</code> if <code>obj</code> is equivalent to this
     *         <code>AdminPermission</code>; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;

        if (!(obj instanceof AdminPermission)) return false;

        AdminPermission that = (AdminPermission) obj;

        return this.target.equals(that.target) && (this.actions ^ that.actions) == 0;
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return Hash code value for this object.
     */
    @Override
    public int hashCode()
    {
        int result = (int) actions;
        result = 31 * result + target.hashCode();
        return result;
    }

    /**
     * Returns the canonical string representation of the
     * <code>AdminPermission</code> actions.
     * <p/>
     * <p/>
     * Always returns present <code>AdminPermission</code> actions in the
     * following order: <code>class</code>, <code>execute</code>,
     * <code>extensionLifecycle</code>, <code>lifecycle</code>,
     * <code>listener</code>, <code>metadata</code>, <code>resolve</code>,
     * <code>resource</code>, <code>startlevel</code>.
     *
     * @return Canonical string representation of the
     *         <code>AdminPermission</code> actions.
     */
    @Override
    public String getActions()
    {
        StringBuilder builder = new StringBuilder();

        if ((CLASS_BIT & actions) > 0)
        {
            builder.append(CLASS);
        }
        if ((EXECUTE_BIT & actions) > 0)
        {
            if (builder.length() > 0) builder.append(",");
            builder.append(EXECUTE);
        }
        if ((EXTENSIONLIFECYCLE_BIT & actions) > 0)
        {
            if (builder.length() > 0) builder.append(",");
            builder.append(EXTENSIONLIFECYCLE);
        }
        if ((LIFECYCLE_BIT & actions) > 0)
        {
            if (builder.length() > 0) builder.append(",");
            builder.append(LIFECYCLE);
        }
        if ((LISTENER_BIT & actions) > 0)
        {
            if (builder.length() > 0) builder.append(",");
            builder.append(LISTENER);
        }
        if ((METADATA_BIT & actions) > 0)
        {
            if (builder.length() > 0) builder.append(",");
            builder.append(METADATA);
        }
        if ((RESOLVE_BIT & actions) > 0)
        {
            if (builder.length() > 0) builder.append(",");
            builder.append(RESOLVE);
        }
        if ((RESOURCE_BIT & actions) > 0)
        {
            if (builder.length() > 0) builder.append(",");
            builder.append(RESOURCE);
        }
        if ((STARTLEVEL_BIT & actions) > 0)
        {
            if (builder.length() > 0) builder.append(",");
            builder.append(STARTLEVEL);
        }

        return builder.toString();
    }

    /**
     * Determines if the specified permission is implied by this object. This
     * method throws an exception if the specified permission was not
     * constructed with a bundle.
     * <p/>
     * <p/>
     * This method returns <code>true</code> if the specified permission is an
     * AdminPermission AND
     * <ul>
     * <li>this object's filter matches the specified permission's bundle ID,
     * bundle symbolic name, bundle location and bundle signer distinguished
     * name chain OR</li>
     * <li>this object's filter is "*"</li>
     * </ul>
     * AND this object's actions include all of the specified permission's
     * actions.
     * <p/>
     * Special case: if the specified permission was constructed with "*"
     * filter, then this method returns <code>true</code> if this object's
     * filter is "*" and this object's actions include all of the specified
     * permission's actions
     *
     * @param p The permission to interrogate.
     * @return <code>true</code> if the specified permission is implied by
     *         this object; <code>false</code> otherwise.
     * @throws RuntimeException if specified permission was not constructed with
     *                          a bundle or "*"
     */
    @Override
    public boolean implies(Permission p)
    {
        if (!(p instanceof AdminPermission)) return false;

        AdminPermission that = (AdminPermission) p;
        return true;
    }

    /**
     * Returns a new <code>PermissionCollection</code> object suitable for
     * storing <code>AdminPermission</code>s.
     *
     * @return A new <code>PermissionCollection</code> object.
     */
    @Override
    public PermissionCollection newPermissionCollection()
    {
        return new Permissions();
    }
}
