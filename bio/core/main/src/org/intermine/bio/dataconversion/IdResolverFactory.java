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
    protected static IdResolver resolver = null; // static to cache

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
     * Return an IdResolver, if not already built then create it. If failOnError
     * set to false then swallow any exceptions and return null. Allows code to
     * continue if no resolver can be set up.
     * @param failOnError if false swallow any exceptions and return null
     * @return a specific IdResolver
     */
    public IdResolver getIdResolver(boolean failOnError) {
        if (!caughtError) {
            try {
                createIdResolver();
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
    protected void createFromFile(String clsName, File f)
        throws IOException {
        resolver = new IdResolver(clsName);
        resolver.populateFromFile(f);
    }

    /**
     * Read IdResolver contents from a file, allows for caching during build.
     *
     * @param f the file to read from
     * @return a created IdResolver
     * @throws IOException if problem reading from file
     */
    protected void createFromFile(File f)
        throws IOException {
        resolver = new IdResolver(defaultClsName);
        resolver.populateFromFile(f);
    }

    /**
     * Read IdResolver contents from a database.
     *
     * @param clsName the class name to resolve
     * @param db the file to read from
     * @return null, need strictly override
     */
    protected void createFromDb(String clsName, Database db) {
    }

    /**
     * Read IdResolver contents from a database.
     *
     * @param db the file to read from
     * @return null, need strictly override
     */
    protected void createFromDb(Database db) {
    }

    /**
     * Create and IdResolver from source information.
     * @return the new IdResolver
     */
    protected abstract void createIdResolver();
}
