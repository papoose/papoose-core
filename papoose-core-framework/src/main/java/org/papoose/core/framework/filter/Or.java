/**
 *
 * Copyright 2007 (C) The original author or authors
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

import java.util.Dictionary;


/**
 * @version $Revision$ $Date$
 */
class Or implements Expr
{
    private final Expr[] expressions;
    private transient volatile String string;

    public Or(Expr[] expressions)
    {
        if (expressions == null) throw new IllegalArgumentException("expressions is null");
        if (expressions.length == 0) throw new IllegalArgumentException("expressions is empty");

        this.expressions = expressions;
    }

    public boolean match(Dictionary<String, Object> dictionary)
    {
        for (Expr expr : expressions)
        {
            if (expr.match(dictionary)) return true;
        }
        return false;
    }

    public String toString()
    {
        if (string == null)
        {
            StringBuilder builder = new StringBuilder();

            builder.append("(|");

            for (Expr expr : expressions) builder.append(expr.toString());

            builder.append(')');

            string = builder.toString();
        }
        return string;
    }
}
