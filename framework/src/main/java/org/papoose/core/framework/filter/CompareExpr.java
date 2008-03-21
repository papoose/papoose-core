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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Dictionary;


/**
 * @version $Revision$ $Date$
 */
abstract class CompareExpr implements Expr
{
    protected final String attribute;
    protected final String value;

    public CompareExpr(String attribute, String value)
    {
        assert attribute != null;
        assert value != null;

        this.attribute = attribute;
        this.value = value;
    }

    public final boolean match(Dictionary<String, Object> dictionary)
    {
        try
        {
            Object object = dictionary.get(attribute);

            if (object == null) return false;

            Object test;
            if (isScalar(object))
            {
                test = convert(object, value);
            }
            else if (object.getClass().isArray())
            {
                int length = Array.getLength(object);

                for (int i = 0; i < length; i++)
                {
                    Object element = Array.get(object, i);
                    if (isScalar(element) && testPair(element, convert(element, value))) return true;
                }
                return false;
            }
            else if (object instanceof Collection)
            {
                for (Object element : (Collection) object)
                {
                    if (isScalar(element) && testPair(element, convert(element, value))) return true;
                }
                return false;
            }
            else
            {
                return false;
            }


            return testPair(object, test);
        }
        catch (NumberFormatException doNothing)
        {
            return false;
        }
    }

    protected abstract boolean testPair(Object object, Object value);

    protected abstract Object getFalseObject();

    protected final Object convert(Object object, String value)
    {
        if (object instanceof String)
        {
            return value;
        }
        else if (object instanceof Integer)
        {
            return Integer.parseInt(value);
        }
        else if (object instanceof Long)
        {
            return Long.parseLong(value);
        }
        else if (object instanceof Float)
        {
            return Float.parseFloat(value);
        }
        else if (object instanceof Double)
        {
            return Double.parseDouble(value);
        }
        else if (object instanceof Byte)
        {
            return Byte.parseByte(value);
        }
        else if (object instanceof Short)
        {
            return Short.parseShort(value);
        }
        else if (object instanceof Character && value.length() == 1)
        {
            return value.charAt(0);
        }
        else if (object instanceof Boolean)
        {
            return Boolean.parseBoolean(value);
        }
        else if (object != null)
        {
            try
            {
                Constructor constructor = object.getClass().getConstructor(String.class);
                return constructor.newInstance(value);
            }
            catch (NoSuchMethodException e)
            {
                return getFalseObject();
            }
            catch (SecurityException e)
            {
                return getFalseObject();
            }
            catch (IllegalAccessException e)
            {
                return getFalseObject();
            }
            catch (InvocationTargetException e)
            {
                return getFalseObject();
            }
            catch (InstantiationException e)
            {
                return getFalseObject();
            }
        }
        else
        {
            return getFalseObject();
        }
    }

    protected static boolean isScalar(Object object)
    {
        if (object instanceof String)
        {
            return true;
        }
        else if (object instanceof Integer)
        {
            return true;
        }
        else if (object instanceof Long)
        {
            return true;
        }
        else if (object instanceof Float)
        {
            return true;
        }
        else if (object instanceof Double)
        {
            return true;
        }
        else if (object instanceof Byte)
        {
            return true;
        }
        else if (object instanceof Short)
        {
            return true;
        }
        else if (object instanceof Character)
        {
            return true;
        }
        else if (object instanceof Boolean)
        {
            return true;
        }
        else
        {
            try
            {
                return (object != null && object.getClass().getConstructor(String.class) != null);
            }
            catch (NoSuchMethodException e)
            {
                return false;
            }
            catch (SecurityException e)
            {
                return false;
            }
        }
    }

}
