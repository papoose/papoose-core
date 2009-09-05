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
package org.papoose.core.util;

/**
 * Asserts are a good thing.  The only occasional problem is that if asserts
 * are not enabled then the conditional is not executed.  This forces the
 * awkward use of temporary booleans.  This utility class allows for the
 * unconditional execution of conditionals.
 * <p/>
 * Modern day JVMs will eventually inline these calls and so there should be no
 * performance issues with this code.
 *
 * @version $Revision$ $Date$
 */
public final class Assert
{
    public static void assertTrue(boolean test)
    {
        assert test;
    }

    public static void assertTrue(boolean test, String message)
    {
        assert test : message;
    }

    private Assert() { }
}
