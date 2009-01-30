/**
 *
 * Copyright 2008-2009 (C) The original author or authors
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
package org.papoose.core.framework.spi;

import java.io.InputStream;

import org.papoose.core.framework.PapooseException;


/**
 * Maps a bundle location string to an <code>InputStream</code> which is used
 * to load a bundle.
 * <p/>
 * A custom location mapper can be designated by passing one in the properties
 * object of the <code>Papoose</code> constructor.  Use the key
 * <code>org.papoose.core.framework.spi.LocationMapper</code>.
 *
 * @version $Revision$ $Date$
 * @see org.papoose.core.framework.DefaultLocationMapper
 * @see org.papoose.core.framework.Papoose
 */
public interface LocationMapper
{
    /**
     * The key to use when passing in a custom location mapper to the Papoose
     * constructor.
     */
    public final static String LOCATION_MANAGER = LocationMapper.class.getName();

    /**
     * Map the bundle location to an input stream.
     *
     * @param location the location string for a bundle
     * @return an input stream that corresponds to the location string
     * @throws PapooseException if an error occurs during mapping
     */
    InputStream mapLocationString(String location) throws PapooseException;
}
