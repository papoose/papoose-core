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
package org.papoose.core.descriptions;

import java.util.Collections;
import java.util.Set;


/**
 *
 */
public class LazyActivationDescription
{
    private final boolean lazyActivation;
    private Set<String> include = Collections.emptySet();
    private Set<String> exclude = Collections.emptySet();

    public LazyActivationDescription(boolean lazyActivation)
    {
        this.lazyActivation = lazyActivation;
    }

    public boolean isLazyActivation()
    {
        return lazyActivation;
    }

    public Set<String> getInclude()
    {
        return include;
    }

    public void setInclude(Set<String> include)
    {
        this.include = include;
    }

    public Set<String> getExclude()
    {
        return exclude;
    }

    public void setExclude(Set<String> exclude)
    {
        this.exclude = exclude;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LazyActivationDescription that = (LazyActivationDescription) o;

        if (lazyActivation != that.lazyActivation) return false;
        if (exclude != null ? !exclude.equals(that.exclude) : that.exclude != null) return false;
        return !(include != null ? !include.equals(that.include) : that.include != null);
    }

    @Override
    public int hashCode()
    {
        int result = (lazyActivation ? 1 : 0);
        result = 31 * result + (include != null ? include.hashCode() : 0);
        result = 31 * result + (exclude != null ? exclude.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return (lazyActivation ? "LAZY" : "EAGER");
    }
}
