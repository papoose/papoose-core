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
package org.papoose.core.framework;

import org.osgi.framework.Version;


/**
 * @version $Revision$ $Date$
 */
class VersionRange
{
    public static final Version DEFAULT_VERSION = new Version(0, 0, 0);
    public static final VersionRange DEFAULT_VERSION_RANGE = new VersionRange(new Version(0, 0, 0), null, true, false);
    private final Version start;
    private final Version end;
    private final boolean startIncluding;
    private final boolean endIncluding;

    public VersionRange(Version start, Version end, boolean startIncluding, boolean endIncluding)
    {
        if (start == null) throw new IllegalArgumentException("Start is null");
        if (end != null)
        {
            int result = start.compareTo(end);
            if (result > 0) throw new IllegalArgumentException("Start must begin before end");
            if (result == 0 && !(startIncluding && endIncluding)) throw new IllegalArgumentException("Start must begin before end");
        }

        this.start = start;
        this.end = end;
        this.startIncluding = startIncluding;
        this.endIncluding = endIncluding;
    }

    public boolean includes(Version version)
    {
        if (version == null) version = DEFAULT_VERSION;
        int result = start.compareTo(version);
        if (result > 0 || (!startIncluding && result == 0)) return false;
        if (end != null)
        {
            result = end.compareTo(version);
            if (result < 0 || (!endIncluding && result == 0)) return false;
        }
        return true;
    }

    /**
     * Parse a string which should be formatted according to the OSGi R4
     * Core specification, section 3.2.5.
     *
     * @param expression the string to be parsed
     * @return an instance of VersionRange
     * @throws IllegalArgumentException if the expression cannot be parsed
     */
    public static VersionRange parseVersionRange(String expression)
    {
        State state = new State(expression);

        state.eatWhitespace();

        Version start;
        Version end;
        boolean startsWith;
        boolean endsWith;
        switch (state.peek())
        {
            case'[':
            {
                state.eat(1);
                startsWith = true;
                break;
            }
            case'(':
            {
                state.eat(1);
                startsWith = false;
                break;
            }
            default:
                try
                {
                    return new VersionRange(Version.parseVersion(expression), null, true, false);
                }
                catch (NumberFormatException nfe)
                {
                    throw new IllegalArgumentException("Invalid version format", nfe);
                }
        }

        state.eatWhitespace();


        try
        {
            start = Version.parseVersion(state.eatVersion());
        }
        catch (NumberFormatException nfe)
        {
            throw new IllegalArgumentException("Invalid start version format", nfe);
        }

        state.eatWhitespace();

        state.eat(",");

        state.eatWhitespace();

        try
        {
            end = Version.parseVersion(state.eatVersion());
        }
        catch (NumberFormatException nfe)
        {
            throw new IllegalArgumentException("Invalid end version format", nfe);
        }

        state.eatWhitespace();

        switch (state.peek())
        {
            case']':
            {
                state.eat(1);
                endsWith = true;
                break;
            }
            case')':
            {
                state.eat(1);
                endsWith = false;
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid format");
        }

        return new VersionRange(start, end, startsWith, endsWith);
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final VersionRange that = (VersionRange) o;

        if (endIncluding != that.endIncluding) return false;
        if (startIncluding != that.startIncluding) return false;
        if (end != null ? !end.equals(that.end) : that.end != null) return false;
        return start.equals(that.start);
    }

    public int hashCode()
    {
        int result = start.hashCode();
        result = 29 * result + (end != null ? end.hashCode() : 0);
        result = 29 * result + (startIncluding ? 1 : 0);
        result = 29 * result + (endIncluding ? 1 : 0);
        return result;
    }

    private static class State
    {
        private final String expression;
        private int pointer = 0;

        public State(String expression)
        {
            this.expression = expression;
        }

        public void eatWhitespace()
        {
            try
            {
                while (Character.isWhitespace(expression.charAt(pointer))) pointer++;
            }
            catch (StringIndexOutOfBoundsException sioobe)
            {
                throw new IllegalArgumentException("Invalid format");
            }
        }

        public void eat(String pattern)
        {
            if (!expression.startsWith(pattern, pointer)) throw new IllegalArgumentException("Invalid format");
            pointer += pattern.length();
        }

        public char peek()
        {
            return expression.charAt(pointer);
        }

        public void eat(int amount)
        {
            pointer += amount;
        }

        public String eatVersion()
        {
            try
            {
                int start = pointer;
                while (isValid(expression.charAt(pointer))) pointer++;
                return expression.substring(start, pointer);
            }
            catch (StringIndexOutOfBoundsException e)
            {
                throw new IllegalArgumentException("Invalid format");
            }
        }

        private boolean isValid(char c)
        {
            return c != ' ' && c != ',' && c != ')' && c != ']';
        }
    }
}
