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

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

import org.papoose.core.filter.Parser;

/**
 * @version $Revision$ $Date$
 */
public class FrameworkUtil
{
    private static Parser parser = new Parser();

    public static Parser getParser()
    {
        return parser;
    }

    public static void setParser(Parser parser)
    {
        FrameworkUtil.parser = parser;
    }

    public static Filter createFilter(String filter) throws InvalidSyntaxException
    {
        return new DefaultFilter(parser.parse(filter));
    }

    private FrameworkUtil() {}
}
