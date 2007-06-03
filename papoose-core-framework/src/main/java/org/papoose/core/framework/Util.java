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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

import org.osgi.framework.BundleException;
import org.osgi.framework.Filter;

import org.papoose.core.framework.filter.Parser;


/**
 * @version $Revision$ $Date$
 */
final class Util
{
    @SuppressWarnings({"EmptyCatchBlock"})
    public static boolean callSetter(Object pojo, String property, Object value)
    {
        try
        {
            Method method = pojo.getClass().getDeclaredMethod(constructMethodName(property), value.getClass());

            method.setAccessible(true);
            method.invoke(pojo, value);

            return true;
        }
        catch (IllegalAccessException fallThrough)
        {
        }
        catch (InvocationTargetException fallThrough)
        {
        }
        catch (SecurityException fallThrough)
        {
        }
        catch (NoSuchMethodException fallThrough)
        {
        }

        return false;
    }

    private static String constructMethodName(String property)
    {
        if (property.contains("-"))
        {
            StringBuilder builder = new StringBuilder("set");
            int pointer = 0;

            builder.append(property.substring(0, 1).toUpperCase());

            while (++pointer < property.length())
            {
                char c = property.charAt(pointer);

                if (c == '-') builder.append(property.substring(++pointer, pointer + 1).toUpperCase());
                else builder.append(c);
            }

            return builder.toString();
        }
        else
        {
            return "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
        }
    }

    public static boolean isValidPackageName(String path)
    {
        String[] tokens = path.split("\\.");

        return checkPath(tokens, "*".equals(tokens[tokens.length - 1]));
    }

    public static boolean isValidWildcardName(String name)
    {
        if ("*".equals(name)) return true;

        String[] tokens = name.split("\\.");

        return checkPath(tokens, "*".equals(tokens[tokens.length - 1]));
    }

    private static boolean checkPath(String[] tokens, boolean skipLast)
    {
        if (!skipLast && !isJavaIdentifier(tokens[tokens.length - 1])) return false;

        for (int i = 0; i < tokens.length - 1; i++)
        {
            if (!isJavaIdentifier(tokens[i])) return false;
        }

        return true;
    }

    public static boolean isJavaIdentifier(String token)
    {
        if (token.length() == 0) return false;

        if (!Character.isJavaIdentifierStart(token.charAt(0))) return false;
        for (int j = 1; j < token.length(); j++) if (!Character.isJavaIdentifierPart(token.charAt(j))) return false;

        return true;
    }

    /**
     * Split a string on <i>pattern</i>, making sure that we do not split
     * on quoted sections.
     *
     * @param string  the string to be split
     * @param pattern the characters to split on
     * @return an array if split sections
     */
    public static String[] split(String string, String pattern)
    {
        List<String> list = new ArrayList<String>(1);
        StringBuilder builder = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < string.length(); i++)
        {
            if (!inQuotes && match(pattern, string, i))
            {
                list.add(builder.toString());
                builder = new StringBuilder();
                i += pattern.length() - 1;
            }
            else if (string.charAt(i) == '"')
            {
                inQuotes = !inQuotes;
            }
            else
            {
                builder.append(string.charAt(i));
            }
        }

        list.add(builder.toString());

        return list.toArray(new String[list.size()]);
    }

    public static boolean match(String pattern, String string, int start)
    {
        int j = 0;
        for (int i = start; i < string.length() && j < pattern.length(); i++, j++)
        {
            if (pattern.charAt(j) != string.charAt(i)) return false;
        }
        return j == pattern.length();
    }

    public static String checkSymbolName(String string)
    {
        new State(string).eatSymbolName();
        return string;
    }

    public static void parseParameters(String string, Object pojo, Map<String, Object> parameters) throws BundleException
    {
        parseParameters(string, pojo, parameters, new ArrayList<String>(0));
    }

    public static void parseParameters(String string, Object pojo, Map<String, Object> parameters, List<String> paths) throws BundleException
    {
        Set<String> parameterKeys = new HashSet<String>();
        Set<String> argumentKeys = new HashSet<String>();
        State state = new State(string);
        while (true)
        {
            state.eatWhitespace();

            String token = state.eatToken();

            state.eatWhitespace();

            switch (state.peek())
            {
                case'=':
                {
                    state.eat(1);
                    state.eatWhitespace();

                    if (parameterKeys.contains(token)) throw new BundleException("Duplicate parameter key: " + token);
                    else parameterKeys.add(token);

                    String argument = state.eatArgument();

                    parameters.put(token, argument);

                    break;
                }
                case':':
                {
                    state.eat(1);
                    state.eat("=");
                    state.eatWhitespace();

                    if (argumentKeys.contains(token)) throw new BundleException("Duplicate argument key: " + token);
                    else argumentKeys.add(token);

                    Object argument = state.eatArgument();

                    if ("visibility".equals(token))
                    {
                        argument = Visibility.valueOf(((String) argument).toUpperCase());
                    }
                    else if ("resolution".equals(token))
                    {
                        argument = Resolution.valueOf(((String) argument).toUpperCase());
                    }
                    else if ("extension".equals(token))
                    {
                        argument = Extension.valueOf(((String) argument).toUpperCase());
                    }
                    else if ("uses".equals(token))
                    {
                        String[] packageNames = ((String) argument).split(",");
                        List<String> list = new ArrayList<String>(packageNames.length);

                        for (String packageName : packageNames)
                        {
                            if (!isValidPackageName(packageName)) throw new BundleException("Malformed package name: " + packageName);
                            list.add(packageName);
                        }

                        argument = list;
                    }
                    else if ("mandatory".equals(token))
                    {
                        String[] attributes = ((String) argument).split(",");
                        List<String> list = new ArrayList<String>(attributes.length);

                        for (String attribute : attributes) list.add(attribute);

                        argument = list;
                    }
                    else if ("include".equals(token))
                    {
                        String[] names = ((String) argument).split(",");
                        List<String> list = new ArrayList<String>(names.length);

                        for (String name : names)
                        {
                            if (!isValidPackageName(name)) throw new BundleException("Malformed class name: " + name);
                            list.add(name);
                        }

                        argument = list;
                    }
                    else if ("exclude".equals(token))
                    {
                        String[] names = ((String) argument).split(",");
                        List<String> list = new ArrayList<String>(names.length);

                        for (String name : names)
                        {
                            if (!isValidPackageName(name)) throw new BundleException("Malformed class name: " + name);
                            list.add(name);
                        }

                        argument = list;
                    }

                    callSetter(pojo, token, argument);

                    break;
                }
                case';':
                {
                    paths.add(token);

                    break;
                }
                default:
                    throw new BundleException("misformatted parameter/path");
            }

            state.eatWhitespace();

            if (state.isComplete()) return;

            state.eat(";");
        }
    }

    private static class State
    {
        private final String string;
        private int pointer = 0;

        public State(String string)
        {
            this.string = string;
        }

        public char peek()
        {
            return string.charAt(pointer);
        }

        public void eat(int amount)
        {
            pointer += amount;
        }

        public void eat(String pattern) throws BundleException
        {
            if (!string.startsWith(pattern, pointer)) throw new BundleException("Expected '" + pattern + "' in " + string);
            pointer += pattern.length();
        }

        public void eatWhitespace()
        {
            while (pointer < string.length() && Character.isWhitespace(string.charAt(pointer))) pointer++;
        }

        public boolean isComplete()
        {
            return !(pointer < string.length());
        }

        public String eatToken()
        {
            StringBuilder builder = new StringBuilder();
            char c;

            while (pointer < string.length() && isValidTokenChar(c = string.charAt(pointer)))
            {
                pointer++;
                builder.append(c);
            }

            return builder.toString();
        }

        public String eatArgument() throws BundleException
        {
            if (peek() == '"')
            {
                eat(1);

                StringBuilder builder = new StringBuilder();
                try
                {
                    char c;

                    while (isValidArgumentChar(c = string.charAt(pointer)))
                    {
                        if (c == '\\')
                        {
                            eat(1);
                            c = string.charAt(pointer);
                        }

                        builder.append(c);
                        pointer++;
                    }

                    eat("\"");
                }
                catch (StringIndexOutOfBoundsException sioobe)
                {
                    throw new BundleException("Invalid argument in " + string);
                }

                return builder.toString();
            }
            else
            {
                return eatToken();
            }
        }

        public void eatSymbolName()
        {
            eatWhitespace();

            eatToken();

            if (isComplete()) return;

            while (peek() == '.')
            {
                eat(1);
                eatToken();
                if (isComplete()) return;
            }
        }

        private boolean isValidTokenChar(char c)
        {
            return Character.isLetterOrDigit(c) || c == '_' || c == '-';
        }

        private boolean isValidArgumentChar(char c)
        {
            return c != '"' && c != 0x0D && c != 0x0A && c != 0x00;
        }

        public String toString()
        {
            return string.substring(Math.min(pointer, string.length() - 1));
        }
    }

    private Util()
    {
    }
}
