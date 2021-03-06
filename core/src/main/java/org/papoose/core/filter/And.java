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

import java.util.Dictionary;


/**
 *
 */
class And implements Expr
{
    private final Expr[] expressions;
    private transient volatile String string;

    public And(Expr[] expressions)
    {
        assert expressions != null;
        assert expressions.length != 0;

        this.expressions = expressions;
    }

    public boolean match(Dictionary<String, Object> dictionary)
    {
        for (Expr expr : expressions)
        {
            if (!expr.match(dictionary)) return false;
        }
        return true;
    }

    public String toString()
    {
        if (string == null)
        {
            StringBuilder builder = new StringBuilder();

            builder.append("(&");

            for (Expr expr : expressions) builder.append(expr.toString());

            builder.append(')');

            string = builder.toString();
        }
        return string;
    }
}
