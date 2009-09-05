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
package org.papoose.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.osgi.framework.BundleException;

import org.papoose.core.Papoose;
import org.papoose.core.descriptions.Extension;
import org.papoose.core.descriptions.Resolution;
import org.papoose.core.descriptions.Visibility;


/**
 * @version $Revision$ $Date$
 */
public final class Util
{
    private final static String CLASS_NAME = Util.class.getName();
    private final static Logger LOGGER = Logger.getLogger(CLASS_NAME);

    public static void copy(InputStream input, OutputStream output) throws IOException
    {
        LOGGER.entering(CLASS_NAME, "copy", new Object[]{ input, output });

        byte[] buffer = new byte[4096];
        int len;

        while ((len = input.read(buffer)) != -1) output.write(buffer, 0, len);

        LOGGER.exiting(CLASS_NAME, "copy");
    }

    public static void close(Closeable closeable)
    {
        LOGGER.entering(CLASS_NAME, "close", closeable);

        try
        {
            if (closeable != null) closeable.close();
        }
        catch (IOException ioe)
        {
            LOGGER.log(Level.WARNING, "Problems closing closeable", ioe);
        }

        LOGGER.exiting(CLASS_NAME, "close");
    }

    public static void delete(File file)
    {
        LOGGER.entering(CLASS_NAME, "delete", file);

        if (file.isDirectory())
        {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine(file + " is a directory");
            for (File f : file.listFiles()) delete(f);
        }

        //noinspection ResultOfMethodCallIgnored
        file.delete();

        LOGGER.exiting(CLASS_NAME, "delete");
    }

    public static boolean callSetter(Object pojo, String property, Object value)
    {
        LOGGER.entering(CLASS_NAME, "callSetter", new Object[]{ pojo, property, value });

        try
        {
            String methodName = constructMethodName(property);
            Method method = getDeclaredMethod(methodName, pojo.getClass(), value.getClass());

            if (method == null)
            {
                LOGGER.warning(methodName + "(" + value.getClass() + ") not found on class " + pojo.getClass());
                return false;
            }

            method.setAccessible(true);
            method.invoke(pojo, value);

            return true;
        }
        catch (IllegalAccessException iae)
        {
            LOGGER.log(Level.WARNING, "Problems calling setter", iae);
        }
        catch (InvocationTargetException ite)
        {
            LOGGER.log(Level.WARNING, "Problems calling setter", ite);
        }
        catch (SecurityException se)
        {
            LOGGER.log(Level.WARNING, "Problems calling setter", se);
        }

        LOGGER.exiting(CLASS_NAME, "callSetter", false);

        return false;
    }

    private static Method getDeclaredMethod(String name, Class pojoClass, Class propertyClass)
    {
        LOGGER.entering(CLASS_NAME, "getDeclaredMethod", new Object[]{ name, pojoClass, propertyClass });

        if (propertyClass == null)
        {
            LOGGER.exiting(CLASS_NAME, "getDeclaredMethod", null);
            return null;
        }

        try
        {
            Method method = pojoClass.getDeclaredMethod(name, propertyClass);

            LOGGER.exiting(CLASS_NAME, "getDeclaredMethod", method);

            return method;
        }
        catch (NoSuchMethodException e)
        {
            if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine(pojoClass + " does not have method " + name + "(" + propertyClass + ")");

            Method method;
            for (Class iface : propertyClass.getInterfaces())
            {
                if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Searching interface " + iface);

                if ((method = getDeclaredMethod(name, pojoClass, iface)) != null)
                {
                    LOGGER.exiting(CLASS_NAME, "getDeclaredMethod", method);

                    return method;
                }
            }

            method = getDeclaredMethod(name, pojoClass, propertyClass.getSuperclass());

            LOGGER.exiting(CLASS_NAME, "getDeclaredMethod", method);

            return method;
        }
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

                if (c == '-')
                {
                    builder.append(property.substring(++pointer, pointer + 1).toUpperCase());
                }
                else
                {
                    builder.append(c);
                }
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
        for (String token : path.split("\\."))
        {
            if (!isJavaIdentifier(token)) return false;
        }

        return true;
    }

    /**
     * Parse the class/resource name which may have any number of wildcards.
     *
     * @param name the class/resource name
     * @return an array of strings split on wildcard, "*", boundaries
     */
    public static String[] parseNameAndValidate(String name)
    {
        assert name != null;

        if ("*".equals(name)) return new String[]{ "" };

        List<String> values = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        int pointer = 0;
        char c;

        for (int i = 0; i < name.length(); i++)
        {
            c = name.charAt(pointer);

            switch (c)
            {
                case '*':
                {
                    pointer++;
                    values.add(builder.toString());
                    builder = new StringBuilder();
                    break;
                }
                default:
                {
                    pointer++;
                    builder.append(c);
                }
            }
        }

        values.add(builder.toString());

        return values.toArray(new String[values.size()]);
    }

    public static String encodeName(String[] parts)
    {
        StringBuilder builder = new StringBuilder();

        for (String part : parts)
        {
            if (part.length() == 0)
            {
                builder.append("*");
            }
            else
            {
                builder.append(part);
            }
        }
        return builder.toString();
    }

    public static boolean isJavaIdentifier(String token)
    {
        assert token != null;

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
        assert string != null;
        assert pattern != null;

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
                builder.append(string.charAt(i));
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
        assert pattern != null;
        assert string != null;

        int j = 0;
        for (int i = start; i < string.length() && j < pattern.length(); i++, j++)
        {
            if (pattern.charAt(j) != string.charAt(i)) return false;
        }
        return j == pattern.length();
    }

    public static String checkSymbolName(String string)
    {
        assert string != null;

        new State(string).eatSymbolName();
        return string;
    }

    public static void parseParameters(String string, Object pojo, Map<String, Object> parameters, boolean preventDuplicates) throws BundleException
    {
        parseParameters(string, pojo, parameters, preventDuplicates, new HashSet<String>(0));
    }

    public static void parseParameters(String string, Object pojo, Map<String, Object> parameters, boolean preventDuplicates, Set<String> paths) throws BundleException
    {
        assert string != null;
        assert pojo != null;
        assert parameters != null;

        Set<String> parameterKeys = new HashSet<String>();
        Set<String> argumentKeys = new HashSet<String>();
        State state = new State(string);

        while (true)
        {
            state.eatWhitespace();

            String token = state.eatToken();

            if ("*".equals(token))
            {
                token = "";
            }
            else if (token.endsWith(".*"))
            {
                token = token.substring(0, token.length() - 1);
            }
            if (token.contains("*")) throw new BundleException("Package name not properly wildcarded");

            state.eatWhitespace();

            if (state.isComplete())
            {
                paths.add(token);
                return;
            }

            switch (state.peek())
            {
                case '=':
                {
                    state.eat(1);
                    state.eatWhitespace();

                    String argument = state.eatArgument();

                    if (parameterKeys.contains(token))
                    {
                        if (preventDuplicates)
                        {
                            throw new BundleException("Duplicate parameter key: " + token);
                        }
                        else
                        {
                            parameters.put(token, parameters.get(token) + "," + argument);
                        }
                    }
                    else
                    {
                        parameterKeys.add(token);
                    }

                    parameters.put(token, argument);

                    break;
                }
                case ':':
                {
                    state.eat(1);
                    state.eat("=");
                    state.eatWhitespace();

                    if (argumentKeys.contains(token))
                    {
                        throw new BundleException("Duplicate argument key: " + token);
                    }
                    else
                    {
                        argumentKeys.add(token);
                    }

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
                        Set<String> set = new HashSet<String>(packageNames.length);

                        for (String packageName : packageNames)
                        {
                            if (!isValidPackageName(packageName)) throw new BundleException("Malformed package name: " + packageName);
                            set.add(packageName);
                        }

                        argument = set;
                    }
                    else if ("mandatory".equals(token))
                    {
                        String[] attributes = ((String) argument).split(",");
                        List<String> list = new ArrayList<String>(attributes.length);

                        list.addAll(Arrays.asList(attributes));

                        argument = list;
                    }
                    else if ("include".equals(token))
                    {
                        String[] names = ((String) argument).split(",");
                        List<String[]> list = new ArrayList<String[]>(names.length);

                        for (String name : names)
                        {
                            list.add(parseNameAndValidate(name));
                        }

                        argument = list;
                    }
                    else if ("exclude".equals(token))
                    {
                        String[] names = ((String) argument).split(",");
                        List<String[]> list = new ArrayList<String[]>(names.length);

                        for (String name : names)
                        {
                            list.add(parseNameAndValidate(name));
                        }

                        argument = list;
                    }

                    callSetter(pojo, token, argument);

                    break;
                }
                case ';':
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

    public static void parseLazyActivationDescription(String string, Object pojo) throws BundleException
    {
        assert string != null;
        assert pojo != null;

        Set<String> argumentKeys = new HashSet<String>();
        State state = new State(string);

        while (true)
        {
            state.eatWhitespace();

            String token = state.eatToken();

            state.eatWhitespace();

            if (state.isComplete())
            {
                return;
            }

            switch (state.peek())
            {
                case ':':
                {
                    state.eat(1);
                    state.eat("=");
                    state.eatWhitespace();

                    if (argumentKeys.contains(token))
                    {
                        throw new BundleException("Duplicate argument key: " + token);
                    }
                    else
                    {
                        argumentKeys.add(token);
                    }

                    Object argument = state.eatArgument();

                    if ("include".equals(token))
                    {
                        String[] packageNames = ((String) argument).split(",");
                        Set<String> set = new HashSet<String>(packageNames.length);

                        for (String packageName : packageNames)
                        {
                            if (!isValidPackageName(packageName)) throw new BundleException("Malformed package name: " + packageName);
                            set.add(packageName);
                        }

                        argument = set;
                    }
                    else if ("exclude".equals(token))
                    {
                        String[] packageNames = ((String) argument).split(",");
                        Set<String> set = new HashSet<String>(packageNames.length);

                        for (String packageName : packageNames)
                        {
                            if (!isValidPackageName(packageName)) throw new BundleException("Malformed package name: " + packageName);
                            set.add(packageName);
                        }

                        argument = set;
                    }

                    callSetter(pojo, token, argument);

                    break;
                }
                default:
                    throw new BundleException("misformatted directives");
            }

            state.eatWhitespace();

            if (state.isComplete()) return;
        }
    }

    public static boolean match(Object value, String test)
    {
        if (value instanceof String)
        {
            return test.equals(value);
        }
        else if (value instanceof String[]) return match((String[]) value, test);
        return false;
    }

    public static boolean match(String[] values, String test)
    {
        if (values.length == 1) return values[0].length() == 0 || test.equals(values[0]);

        if (!values[0].regionMatches(0, test, 0, values[0].length())) return false;

        int pointer = values[0].length();

        done:
        for (int i = 1; i < values.length - 1; i++)
        {
            int length = values[i].length();
            int limit = test.length() - length;
            while (pointer <= limit)
            {
                if (values[i].regionMatches(0, test, pointer++, length))
                {
                    pointer += values[i].length() - 1;
                    continue done;
                }
            }
            return false;
        }

        return test.substring(pointer).endsWith(values[values.length - 1]);
    }

    public static void callStart(Object pojo, Papoose framework) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Class pojoClass = pojo.getClass();
        Method startMethod = pojoClass.getMethod("start", Papoose.class);
        startMethod.invoke(pojo, framework);
    }

    public static void callStop(Object pojo) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Class pojoClass = pojo.getClass();
        Method stopMethod = pojoClass.getMethod("stop");
        stopMethod.invoke(pojo);
    }

    private static class State
    {
        private final String string;
        private int pointer = 0;

        public State(String string)
        {
            assert string != null;

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
            return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ',' || c == '*';
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

    public static String strip(String src)
    {
        StringBuilder builder = new StringBuilder();

        for (char c : src.toCharArray())
        {
            switch (c)
            {
                case '/':
                {
                    builder.append(File.separatorChar);
                    break;
                }
                default:
                    builder.append(c);
            }
        }

        return builder.toString();
    }

    public static InputStream safeStream(File file) throws BundleException
    {
        try
        {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException ioe)
        {
            throw new BundleException("Unable to obtain input stream", ioe);
        }
    }

    public static <T> Iterable<List<T>> combinations(final List<T> set)
    {
        final List<T> list = new ArrayList<T>(set);
        return new Iterable<List<T>>()
        {
            public Iterator<List<T>> iterator()
            {
                return new Iterator<List<T>>()
                {
                    int i = set.size();
                    Iterator<List<T>> iterator = combinations(list, i);

                    public boolean hasNext()
                    {
                        return i > 0 || iterator.hasNext();
                    }

                    public List<T> next()
                    {
                        try
                        {
                            return iterator.next();
                        }
                        catch (NoSuchElementException e)
                        {
                            return (iterator = combinations(list, --i)).next();
                        }
                    }

                    public void remove() { throw new UnsupportedOperationException(); }
                };
            }
        };
    }

    public static <T> Iterator<List<T>> combinations(final List<T> list, final int z)
    {
        return new Iterator<List<T>>()
        {
            final private int n = list.size();
            final private int r = z;
            final private int[] indexes = new int[z];
            final private BigInteger total;
            private BigInteger numLeft;

            {
                BigInteger nFact = getFactorial(n);
                BigInteger rFact = getFactorial(r);
                BigInteger nminusrFact = getFactorial(n - r);

                total = nFact.divide(rFact.multiply(nminusrFact));

                for (int i = 0; i < indexes.length; i++) indexes[i] = i;

                numLeft = new BigInteger(total.toString());
            }

            public boolean hasNext()
            {
                return numLeft.compareTo(BigInteger.ZERO) == 1;
            }

            public List<T> next()
            {
                if (numLeft.compareTo(BigInteger.ZERO) != 1)
                {
                    throw new NoSuchElementException();
                }
                else if (numLeft.equals(total))
                {
                    numLeft = numLeft.subtract(BigInteger.ONE);
                }
                else
                {
                    int i = r - 1;
                    while (indexes[i] == n - r + i) i--;
                    indexes[i] = indexes[i] + 1;
                    for (int j = i + 1; j < r; j++) indexes[j] = indexes[i] + j - i;

                    numLeft = numLeft.subtract(BigInteger.ONE);
                }

                List<T> result = new ArrayList<T>(indexes.length);

                for (int index : indexes) result.add(list.get(index));

                return result;
            }

            public void remove() { throw new UnsupportedOperationException(); }
        };
    }


    private static BigInteger getFactorial(int n)
    {
        BigInteger fact = BigInteger.ONE;

        for (int i = n; i > 1; i--) fact = fact.multiply(BigInteger.valueOf(i));

        return fact;
    }

    private Util()
    {
    }
}
