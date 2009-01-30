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
 * The Apache Felix project seemed to have a nice default implementation.
 *
 * @version $Revision$ $Date$
 */
@Immutable
public class DefaultApproxAlgorithm implements ApproxAlgorithm
{
    // Criteria in % to accept something as approximate.
    public static final int APPROX_CRITERIA = 10;

    /**
     * Test if two objects are approximate. The two objects that are passed must
     * have the same type.
     * <p/>
     * Approximate for numerical values involves a difference of less than APPROX_CRITERIA
     * Approximate for string values is calculated by using the Levenshtein distance
     * between strings and is case insensitive. Lesser than APPROX_CRITERIA of
     * difference is considered as approximate.
     * <p/>
     * Supported types only include the following subclasses of Number:
     * - Byte
     * - Double
     * - Float
     * - Int
     * - Long
     * - Short
     * - BigInteger
     * - BigDecimal
     * As subclasses of Number must provide methods to convert the represented numeric value
     * to byte, double, float, int, long, and short. (see API)
     *
     * @param obj1
     * @param obj2
     * @return true if they are approximate
     */
    public boolean test(Object obj1, Object obj2)
    {
        if (obj1 instanceof Byte)
        {
            byte value1 = (Byte) obj1;
            byte value2 = (Byte) obj2;
            return (value2 >= (value1 - ((Math.abs(value1) * (byte) APPROX_CRITERIA) / (byte) 100))
                    && value2 <= (value1 + ((Math.abs(value1) * (byte) APPROX_CRITERIA) / (byte) 100)));
        }
        else if (obj1 instanceof Character)
        {
            char value1 = (Character) obj1;
            char value2 = (Character) obj2;
            return (value2 >= (value1 - ((Math.abs(value1) * (char) APPROX_CRITERIA) / (char) 100))
                    && value2 <= (value1 + ((Math.abs(value1) * (char) APPROX_CRITERIA) / (char) 100)));
        }
        else if (obj1 instanceof Double)
        {
            double value1 = (Double) obj1;
            double value2 = (Double) obj2;
            return (value2 >= (value1 - ((Math.abs(value1) * (double) APPROX_CRITERIA) / (double) 100))
                    && value2 <= (value1 + ((Math.abs(value1) * (double) APPROX_CRITERIA) / (double) 100)));
        }
        else if (obj1 instanceof Float)
        {
            float value1 = (Float) obj1;
            float value2 = (Float) obj2;
            return (value2 >= (value1 - ((Math.abs(value1) * (float) APPROX_CRITERIA) / (float) 100))
                    && value2 <= (value1 + ((Math.abs(value1) * (float) APPROX_CRITERIA) / (float) 100)));
        }
        else if (obj1 instanceof Integer)
        {
            int value1 = (Integer) obj1;
            int value2 = (Integer) obj2;
            return (value2 >= (value1 - ((Math.abs(value1) * (int) APPROX_CRITERIA) / (int) 100))
                    && value2 <= (value1 + ((Math.abs(value1) * (int) APPROX_CRITERIA) / (int) 100)));
        }
        else if (obj1 instanceof Long)
        {
            long value1 = (Long) obj1;
            long value2 = (Long) obj2;
            return (value2 >= (value1 - ((Math.abs(value1) * (long) APPROX_CRITERIA) / (long) 100))
                    && value2 <= (value1 + ((Math.abs(value1) * (long) APPROX_CRITERIA) / (long) 100)));
        }
        else if (obj1 instanceof Short)
        {
            short value1 = (Short) obj1;
            short value2 = (Short) obj2;
            return (value2 >= (value1 - ((Math.abs(value1) * (short) APPROX_CRITERIA) / (short) 100))
                    && value2 <= (value1 + ((Math.abs(value1) * (short) APPROX_CRITERIA) / (short) 100)));
        }
        else if (obj1 instanceof String)
        {
            int distance = LD(obj1.toString().toLowerCase(), obj2.toString().toLowerCase());
            int size = ((String) obj1).length();
            return (distance <= ((size * APPROX_CRITERIA) / 100));
        }
        else
        {
            return false;
        }
    }

    /**
     * Compute the Levenshtein distance
     * <p/>
     * Levenshtein distance (LD) is a measure of the similarity between two
     * strings, which we will refer to as the source string (s) and the target
     * string (t). The distance is the number of deletions, insertions, or
     * substitutions required to transform s into t.
     *
     * @param s the source string
     * @param t the target string
     * @return the Levenshtein distance between s and t
     * @see http://www.merriampark.com/ld.htm
     */
    public static int LD(String s, String t)
    {
        int d[][]; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s
        char t_j; // jth character of t
        int cost; // cost

        // Step 1

        n = s.length();
        m = t.length();

        if (n == 0) return m;
        if (m == 0) return n;

        d = new int[n + 1][m + 1];

        // Step 2

        for (i = 0; i <= n; i++) d[i][0] = i;
        for (j = 0; j <= m; j++) d[0][j] = j;

        // Step 3

        for (i = 1; i <= n; i++)
        {
            s_i = s.charAt(i - 1);

            // Step 4

            for (j = 1; j <= m; j++)
            {
                t_j = t.charAt(j - 1);

                // Step 5

                if (s_i == t_j)
                {
                    cost = 0;
                }
                else
                {
                    cost = 1;
                }

                // Step 6

                d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
            }
        }

        // Step 7

        return d[n][m];
    }

    private static int Minimum(int a, int b, int c)
    {
        int mi = a;
        if (b < mi) mi = b;
        if (c < mi) mi = c;
        return mi;
    }

}
