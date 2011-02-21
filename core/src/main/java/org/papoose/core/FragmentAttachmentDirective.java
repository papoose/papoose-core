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
package org.papoose.core;

import org.osgi.framework.Constants;

/**
 *
 */
public enum FragmentAttachmentDirective
{
    ALWAYS,
    NEVER,
    RESOLVE_TIME;

    public static FragmentAttachmentDirective parseFragmentDescription(String string)
    {
        if (Constants.FRAGMENT_ATTACHMENT_ALWAYS.equals(string)) return ALWAYS;
        if (Constants.FRAGMENT_ATTACHMENT_NEVER.equals(string)) return NEVER;
        if (Constants.FRAGMENT_ATTACHMENT_RESOLVETIME.equals(string)) return RESOLVE_TIME;
        throw new IllegalArgumentException("No enum const FragmentAttachmentDirective." + string);
    }
}
