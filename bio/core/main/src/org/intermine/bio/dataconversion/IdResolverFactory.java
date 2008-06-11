package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Create an IdResolver.
 * @author rns
 *
 */
public abstract class IdResolverFactory
{
    private IdResolver resolver = null;
    private boolean caughtError = false;
    
    /**
     * Return an IdResolver, if not already built then create it.
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver() {
        return getIdResolver(true);
    }

    /**
     * Return an IdResolver, if not already built then create it.  If failOnError
     * set to false then swallow any exceptions and return null.  Allows code to
     * continue if no resolver can be set up.
     * @param failOnError if false swallow any exceptions and return null
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver(boolean failOnError) {
        if (resolver == null && !caughtError) {
            try {
                this.resolver = createIdResolver();
            } catch (Exception e) {
                if (failOnError) {
                    this.caughtError = true;
                    throw new RuntimeException(e);
                }
            }
        }
        return resolver;
    }
    
    /**
     * Create and IdResolver from source information.
     * @return the new IdResolver
     */
    protected abstract IdResolver createIdResolver();
}
