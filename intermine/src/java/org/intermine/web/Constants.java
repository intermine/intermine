package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Container for ServletContext and Session attribute names used by the webapp
 *
 * @author Kim Rutherford
 */
public interface Constants
{
    /**
     * ServletContext attribute used to store web.properties
     */
    public static final String WEB_PROPERTIES = "WEB_PROPERTIES";

    /**
     * ServletContext attribute used to store the example queries
     */
    public static final String EXAMPLE_QUERIES = "EXAMPLE_QUERIES";

    /**
     * ServletContext attribute used to store the ObjectStore
     */
    public static final String OBJECTSTORE = "OBJECTSTORE";

    /**
     * Session attribute used to store saved bags
     */
    public static final String SAVED_BAGS = "SAVED_BAGS";

    /**
     * Session attribute used to store saved queries
     */
    public static final String SAVED_QUERIES = "SAVED_QUERIES";

    /**
     * Session attribute used to store the current query
     */
    public static final String QUERY = "QUERY";

    /**
     * Session attribute used to store the current view
     */
    public static final String VIEW = "VIEW";

    /**
     * Session attribute used to store the current results table
     */
    public static final String RESULTS_TABLE = "RESULTS_TABLE";
}
