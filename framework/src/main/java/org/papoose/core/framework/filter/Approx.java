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
package org.papoose.core.framework.filter;

import net.jcip.annotations.Immutable;

/**
 * @version $Revision$ $Date$
 */
@Immutable
class Approx extends CompareExpr
{
    private final ApproxAlgorithm algorithm;
    private transient volatile String string;

    public Approx(String attribute, String value, ApproxAlgorithm algorithm)
    {
        super(attribute, value);

        assert algorithm != null;

        this.algorithm = algorithm;
    }

    protected boolean testPair(Object object, Object value)
    {
        return algorithm.test(object, value);
    }

    protected Object getFalseObject()
    {
        return null;
    }

    public String toString()
    {
        if (string == null)
        {
            StringBuilder builder = new StringBuilder();

            builder.append('(');
            builder.append(attribute);
            builder.append("~=");
            builder.append(value);
            builder.append(')');

            string = builder.toString();
        }
        return string;
    }
}
