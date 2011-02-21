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
package org.papoose.core.util;

import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.papoose.core.L18nResourceBundle;
import org.papoose.core.spi.ArchiveStore;


/**
 *
 */
public class I18nUtils
{
    /**
     * Generate a list of locales to support the specific search order
     * specified in Section 3.10  Localization.
     *
     * @param locale the seed locale
     * @return a list of locales in a specific order
     */
    public static List<Locale> generateLocaleList(Locale locale)
    {
        List<Locale> result = new Vector<Locale>(3);
        String language = locale.getLanguage();
        int languageLength = language.length();
        String country = locale.getCountry();
        int countryLength = country.length();
        String variant = locale.getVariant();
        int variantLength = variant.length();

        if (languageLength + countryLength + variantLength == 0) return result;

        if (languageLength > 0) result.add(new Locale(language));

        if (countryLength + variantLength == 0) return result;

        if (countryLength > 0) result.add(new Locale(language, country));

        if (variantLength == 0) return result;

        result.add(new Locale(language, country, variant));

        return result;
    }

    /**
     * Attempt to obtain a resource bundle from an archive store, setting it's
     * parent if one is found.  If a resource bundle is not found then the
     * parent is returned.
     *
     * @param store  the archive store to attempt to obtain the resource bundle
     * @param parent the parent to use if a resource bundle is found
     * @param locale the locale to use to attempt to obtain the resource bundle
     * @return the resource bundle if one is found, else parent
     */
    public static L18nResourceBundle loadResourceBundle(ArchiveStore store, L18nResourceBundle parent, Locale locale)
    {
        L18nResourceBundle resourceBundle = store.getResourceBundle(locale);
        if (resourceBundle != null)
        {
            resourceBundle.setParent(parent);
            return resourceBundle;
        }
        else
        {
            return parent;
        }
    }

    /**
     * Parse a string to obtain a Locale instance.
     * <p/>
     * I don't know why <code>Locale</code> doesn't have a handy utility method
     * to do this.
     *
     * @param locale a locale in string form
     * @return a <code>Locale</code> instance parsed from the local string
     */
    public static Locale parseLocale(String locale)
    {
        String[] tokens = locale.split("_");
        if (tokens.length == 3) return new Locale(tokens[0], tokens[1], tokens[3]);
        if (tokens.length == 2) return new Locale(tokens[0], tokens[1]);
        return new Locale(tokens[0]);
    }

    private I18nUtils() { }
}
