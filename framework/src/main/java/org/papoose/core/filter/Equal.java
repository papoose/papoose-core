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
package org.papoose.core.filter;

import net.jcip.annotations.Immutable;

/**
 * @version $Revision$ $Date$
 */
@Immutable
class Equal extends CompareExpr
{
    private transient volatile String string;

    public Equal(String attribute, String value)
    {
        super(attribute, value);
    }

    protected boolean testPair(Object object, Object value)
    {
        return value.equals(object);
    }

    protected Object getFalseObject()
    {
        return FALSE;
    }

    private final static Object FALSE = new Object()
    {
        public boolean equals(Object obj)
        {
            return false;
        }
    };

    public String toString()
    {
        if (string == null)
        {
            StringBuilder builder = new StringBuilder();

            builder.append('(');
            builder.append(attribute);
            builder.append('=');
            builder.append(value);
            builder.append(')');

            string = builder.toString();
        }
        return string;
    }
}
