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

import java.util.Set;

import org.osgi.framework.BundleException;
import org.papoose.core.framework.Generation;
import org.papoose.core.framework.Papoose;
import org.papoose.core.framework.PapooseException;


/**
 * Interface for resolvers.
 * <p/>
 * <b>Note:</b> No effort is made to protect against concurrent access of this
 * interface's methods.  It is up to the implementer to ensure thread safety.
 *
 * @version $Revision$ $Date$
 */
public interface Resolver
{
    /**
     * Called by the owning framework to notify the resolver that the framework
     * is starting.  The owning framework passes a referene to itself.
     * <p/>
     * If an error occurs that prevents the resolver from functioning properly
     * an exception will be thrown.  When this happens, the owning framework
     * will shutdown and throw an exception for its start method.
     *
     * @param framework the owning framework
     * @throws PapooseException if an error occurs that prevents the resolver
     *                          from functioning correctly.
     */
    public void start(Papoose framework) throws PapooseException;

    /**
     * Called by the owning framework to notify the resolver that the framework
     * is stopping.
     */
    public void stop();

    /**
     * Notify the resolve that a bundle has been added.
     * <p/>
     * The bundle will be fully installed when this method is called.
     *
     * @param bundle the bundle that was added.
     */
    public void added(Generation bundle);

    /**
     * Notify the resolver that a bundle has been removed.
     * <p/>
     * The bundle will be fully uninstalled when this method is called.
     *
     * @param bundle the bundle that was removed.
     */
    public void removed(Generation bundle);

    /**
     * Resolve a bundle to obtain a solution set of bundles that can be
     * resolved and their solutions.  Throw <code>BundleException</code> if no
     * consistent set of solutions can be found.
     *
     * @param bundle the bundle to resolve
     * @return a set of solutions that is consistent with the bundle's requirements
     * @throws BundleException if no consistent set of wires can be found
     */
    public Set<Solution> resolve(Generation bundle) throws BundleException;
}
