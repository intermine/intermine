package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;

import org.intermine.sql.Database;

/**
 * Create an IdResolver.
 * @author rns
 * @author Fengyuan Hu
 *
 */
public abstract class IdResolverFactory
{
    protected IdResolver resolver = null;
    protected boolean caughtError = false;

    // ResolverFactory takes in a SO term/Class name, by default, "gene" is used
    protected final String defaultClsName = "gene";
    protected String clsName;

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
                this.caughtError = true;
                if (failOnError) {
                    throw new RuntimeException(e);
                }
            }
        }
        return resolver;
    }

    /**
     * Read IdResolver contents from a file, allows for caching during build.
     *
     * @param clsName the class name to resolve
     * @param f the file to read from
     * @return a created IdResolver
     * @throws IOException if problem reading from file
     */
    protected IdResolver createFromFile(String clsName, File f)
        throws IOException {
        resolver = new IdResolver(clsName);
        resolver.populateFromFile(f);
        return resolver;
    }

    /**
     * Read IdResolver contents from a file, allows for caching during build.
     *
     * @param f the file to read from
     * @return a created IdResolver
     * @throws IOException if problem reading from file
     */
    protected IdResolver createFromFile(File f)
        throws IOException {
        resolver = new IdResolver(defaultClsName);
        resolver.populateFromFile(f);
        return resolver;
    }

    /**
     * Read IdResolver contents from a database.
     *
     * @param clsName the class name to resolve
     * @param db the file to read from
     * @return null, need strictly override
     */
    protected IdResolver createFromDb(String clsName, Database db) {
        return resolver;
    }

    /**
     * Read IdResolver contents from a database.
     *
     * @param db the file to read from
     * @return null, need strictly override
     */
    protected IdResolver createFromDb(Database db) {
        return resolver;
    }

    /**
     * Create and IdResolver from source information.
     * @return the new IdResolver
     */
    protected abstract IdResolver createIdResolver();
}
