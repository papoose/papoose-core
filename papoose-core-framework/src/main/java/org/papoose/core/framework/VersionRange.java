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
    private final Version start;
    private final Version end;
    private final boolean startIncluding;
    private final boolean endIncluding;

    public VersionRange(Version start, Version end, boolean startIncluding, boolean endIncluding)
    {
        if (start != null && end != null)
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

    public Version getStart()
    {
        return start;
    }

    public Version getEnd()
    {
        return end;
    }

    public boolean isStartIncluding()
    {
        return startIncluding;
    }

    public boolean isEndIncluding()
    {
        return endIncluding;
    }

    public boolean includes(Version version)
    {
        if (start != null && (start.compareTo(version) > 0 || (!startIncluding && start.compareTo(version) == 0))) return false;
        return !(end != null && (end.compareTo(version) < 0 || (!endIncluding && end.compareTo(version) == 0)));
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final VersionRange that = (VersionRange) o;

        if (endIncluding != that.endIncluding) return false;
        if (startIncluding != that.startIncluding) return false;
        if (end != null ? !end.equals(that.end) : that.end != null) return false;
        return !(start != null ? !start.equals(that.start) : that.start != null);
    }

    public int hashCode()
    {
        int result;
        result = (start != null ? start.hashCode() : 0);
        result = 29 * result + (end != null ? end.hashCode() : 0);
        result = 29 * result + (startIncluding ? 1 : 0);
        result = 29 * result + (endIncluding ? 1 : 0);
        return result;
    }
}
