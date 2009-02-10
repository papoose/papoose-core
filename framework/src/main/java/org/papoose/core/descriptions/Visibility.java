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
package org.papoose.core.descriptions;

/**
 * The visibility of the exports of a required bundle.
 *
 * @version $Revision$ $Date$
 */
public enum Visibility
{
    /**
     * Do not re-export the wired bundle's exports
     */
    PRIVATE,

    /**
     * Re-export the wired bundle's exports
     */
    REEXPORT
}
