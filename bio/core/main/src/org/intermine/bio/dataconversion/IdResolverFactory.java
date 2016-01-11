package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.intermine.sql.Database;

/**
 * Create an IdResolver.
 *
 * @author Richard Smith
 * @author Fengyuan Hu
 *
 */
public abstract class IdResolverFactory
{
    protected static IdResolver resolver = null;

    protected boolean caughtError = false;

    // ResolverFactory takes in a SO term/Class name (as a collection), "gene" is used by default
    protected final Set<String> defaultClsCol = new HashSet<String>(
            Arrays.asList(new String[] {"gene"}));
    protected Set<String> clsCol = new HashSet<String>();

    protected static String idResolverCachedFileName = "build/idresolver.cache";

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
     * @param f the file to read from
     * @return a created IdResolver
     * @throws IOException if problem reading from file
     */
    protected boolean restoreFromFile(File f)
        throws IOException {
        if (f.exists()) {
            resolver.populateFromFile(f);
            return true;
        }
        return false;
    }

    /**
     * Read IdResolver contents from a file, allows for caching during build. Use default file name.
     *
     * @return a created IdResolver
     * @throws IOException if problem reading from file
     */
    protected boolean restoreFromFile()
        throws IOException {
        File f = new File(idResolverCachedFileName);
        if (f.exists()) {
            resolver.populateFromFile(f);
            return true;
        }
        return false;
    }

    /**
     * Read IdResolver contents from a database.
     *
     * @param db the file to read from
     */
    protected void createFromDb(Database db) {
        createFromDb(defaultClsCol, db);
    }

    /**
     * Read IdResolver contents from a database.
     *
     * @param clsName the class name to resolve
     * @param db the file to read from
     */
    protected void createFromDb(String clsName, Database db) {
        createFromDb(new HashSet<String>(Arrays.asList(new String[]{clsName})), db);
    }

    /**
     * Read IdResolver contents from a database.
     *
     * @param clsCol a collection of class name to resolve
     * @param db the file to read from
     */
    protected void createFromDb(Set<String> clsCol, Database db) {
    }

    /**
     * Create and IdResolver from source information.
     */
    protected abstract void createIdResolver();
}
