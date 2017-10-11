package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Construct a DoNothingIdResolver that will resolve every id to the id that was given as input.
 *
 * @author Richard Smith
 */
public class DoNothingIdResolverFactory extends IdResolverFactory
{

    private IdResolver resolver = null;

    /**
     * Construct with class name for mock IdResolver
     * @param clsName the type to resolve
     */
    public DoNothingIdResolverFactory(String clsName) {
        resolver = new DoNothingIdResolver(clsName);
    }

    /**
     * @return the ID resolver
     * @see org.intermine.bio.dataconversion.IdResolverFactory#getIdResolver()
     */
    public IdResolver getIdResolver() {
        return resolver;
    }

    @Override
    protected void createIdResolver() {
    }

}
