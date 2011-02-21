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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.InvalidSyntaxException;


/**
 *
 */
public final class Parser
{
    private final ApproxAlgorithm approxAlgorithm;

    public Parser()
    {
        this(new DefaultApproxAlgorithm());
    }

    public Parser(ApproxAlgorithm approxAlgorithm)
    {
        if (approxAlgorithm == null) throw new IllegalArgumentException("Approx algorithm cannot be null");

        this.approxAlgorithm = approxAlgorithm;
    }

    public Expr parse(String expression) throws InvalidSyntaxException
    {
        if (expression == null) throw new IllegalArgumentException("The expression cannot be null");

        try
        {
            return parseFilter(new State(expression));
        }
        catch (StringIndexOutOfBoundsException e)
        {
            throw new InvalidSyntaxException("Rolled off the end of the string while parsing the expression", expression);
        }
    }

    Expr parseFilter(State state) throws InvalidSyntaxException
    {
        Expr result;

        state.eatWhitespace();

        state.eat("(");

        state.eatWhitespace();

        switch (state.peek())
        {
            case '&':
            {
                state.eat(1);
                result = parseAnd(state);
                break;
            }
            case '|':
            {
                state.eat(1);
                result = parseOr(state);
                break;
            }
            case '!':
            {
                state.eat(1);
                result = parseNot(state);
                break;
            }
            default:
                result = parseOperation(state);
        }

        state.eatWhitespace();

        state.eat(")");

        return result;
    }

    And parseAnd(State state) throws InvalidSyntaxException
    {
        List<Expr> result = new Vector<Expr>();

        Expr expression = parseFilter(state);

        while (expression != null)
        {
            result.add(expression);

            state.eatWhitespace();

            if (state.peek() == ')') break;

            expression = parseFilter(state);
        }

        if (result.isEmpty()) throw new InvalidSyntaxException("Empty & expression", state.getExpression());

        return new And(result.toArray(new Expr[result.size()]));
    }

    Or parseOr(State state) throws InvalidSyntaxException
    {
        List<Expr> result = new Vector<Expr>();

        Expr expression = parseFilter(state);

        while (expression != null)
        {
            result.add(expression);

            state.eatWhitespace();

            if (state.peek() == ')') break;

            expression = parseFilter(state);
        }

        if (result.isEmpty()) throw new InvalidSyntaxException("Empty | expression", state.getExpression());

        return new Or(result.toArray(new Expr[result.size()]));
    }

    Not parseNot(State state) throws InvalidSyntaxException
    {
        Expr expression = parseFilter(state);

        if (expression == null) throw new InvalidSyntaxException("Empty ! expression", state.getExpression());

        return new Not(expression);
    }

    Expr parseOperation(State state) throws InvalidSyntaxException
    {
        String attribute = parseAttribute(state);

        switch (state.peek())
        {
            case '=':
            {
                state.eat(1);
                return parseEqualOrSubstrOrPresent(state, attribute);
            }
            case '~':
            {
                state.eat(1);
                state.eat("=");
                return parseApprox(state, attribute);
            }
            case '>':
            {
                state.eat(1);
                state.eat("=");
                return parseGreater(state, attribute);
            }
            case '<':
            {
                state.eat(1);
                state.eat("=");
                return parseLess(state, attribute);
            }
        }

        return null;
    }

    String parseAttribute(State state) throws InvalidSyntaxException
    {
        return state.eatAttribute().trim();
    }

    Expr parseEqualOrSubstrOrPresent(State state, String attribute) throws InvalidSyntaxException
    {
        Object value = state.eatValue();

        if (value instanceof String[])
        {
            return new Substr(attribute, (String[]) value);
        }
        else if (value instanceof String)
        {
            return new Equal(attribute, (String) value);
        }
        else
        {
            return new Present(attribute);
        }
    }

    Approx parseApprox(State state, String attribute) throws InvalidSyntaxException
    {
        Object value = state.eatValue();

        if (value instanceof String)
        {
            return new Approx(attribute, (String) value, approxAlgorithm);
        }
        else
        {
            throw new InvalidSyntaxException("Did not expect substr value", state.getExpression());
        }

    }

    Greater parseGreater(State state, String attribute) throws InvalidSyntaxException
    {
        Object value = state.eatValue();

        if (value instanceof String)
        {
            return new Greater(attribute, (String) value);
        }
        else
        {
            throw new InvalidSyntaxException("Did not expect substr value", state.getExpression());
        }
    }

    Lesser parseLess(State state, String attribute) throws InvalidSyntaxException
    {
        Object value = state.eatValue();

        if (value instanceof String)
        {
            return new Lesser(attribute, (String) value);
        }
        else
        {
            throw new InvalidSyntaxException("Did not expect substr value", state.getExpression());
        }
    }


    private static class State
    {
        private final String expression;
        private int pointer = 0;

        public State(String expression)
        {
            this.expression = expression;
        }

        public String getExpression()
        {
            return expression;
        }

        public void eatWhitespace()
        {
            while (Character.isWhitespace(expression.charAt(pointer))) pointer++;
        }

        public void eat(String pattern) throws InvalidSyntaxException
        {
            if (!expression.startsWith(pattern, pointer)) throw new InvalidSyntaxException("Expected '" + pattern + "'", expression);
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

        public String eatAttribute() throws InvalidSyntaxException
        {
            int start = pointer;

            while (isValidAttributeChar(expression.charAt(pointer))) pointer++;

            if (start == pointer) throw new InvalidSyntaxException("Invalid attribute name", expression);

            return expression.substring(start, pointer);
        }

        public boolean isValidAttributeChar(char c)
        {
            return c != '=' && c != '>' && c != '<' && c != '~' && c != '(' && c != ')';
        }

        public Object eatValue() throws InvalidSyntaxException
        {
            List<String> values = new ArrayList<String>();

            StringBuilder builder = new StringBuilder();
            try
            {
                char c;

                while (isValidValueChar(c = expression.charAt(pointer)))
                {
                    switch (c)
                    {
                        case '\\':
                        {
                            pointer++;
                            builder.append(expression.charAt(pointer++));
                            break;
                        }
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
            }
            catch (StringIndexOutOfBoundsException e)
            {
                throw new InvalidSyntaxException("Invalid escaping of value", expression);
            }

            values.add(builder.toString());

            if (values.size() == 1)
            {
                return values.get(0);
            }
            else if (values.size() == 2 & values.get(0).length() == 0 && values.get(1).length() == 0)
            {
                return null;
            }
            else
            {
                return values.toArray(new String[values.size()]);
            }
        }

        public boolean isValidValueChar(char c)
        {
            return c != ')';
        }

        public String toString()
        {
            return expression.substring(Math.min(pointer, expression.length() - 1));
        }
    }
}
