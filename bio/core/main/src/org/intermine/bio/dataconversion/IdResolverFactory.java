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

    /**
     * Return an IdResolver, if not already built then create it.
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver() {
        if (resolver == null) {
            this.resolver = createIdResolver();
        }
        return resolver;
    }

    /**
     * Create and IdResolver from source information.
     * @return the new IdResolver
     */
    protected abstract IdResolver createIdResolver();
}
