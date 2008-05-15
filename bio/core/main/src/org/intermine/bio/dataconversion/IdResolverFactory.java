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
     * Create an IdResolver.
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver() {
        if (resolver == null) {
            return createIdResolver();
        }
        return resolver;
    }
    
    protected abstract IdResolver createIdResolver();
}
