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
import java.util.Enumeration;

/**
 *
 */
class AdminPermissionCollection extends PermissionCollection
{
    public void add(Permission permission)
    {
        //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public boolean implies(Permission permission)
    {
        return false;  //Todo: change body of implemented methods use File | Settings | File Templates.
    }

    public Enumeration<Permission> elements()
    {
        return null;  //Todo: change body of implemented methods use File | Settings | File Templates.
    }
}
