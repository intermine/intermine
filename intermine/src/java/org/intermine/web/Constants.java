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
 * Container for global constants used by the webapp.
 *
 * @author Kim Rutherford
 */

public interface Constants
{
    /**
     * The attribute name to use when storing the ObjectStore in the ServletContext.
     */
    public static final String OBJECTSTORE = "OBJECTSTORE";

    /**
     * The attribute name to use when storing the Model in the ServletContext.
     */
    public static final String MODEL = "MODEL";

    /**
     * The attribute name to use when storing the Map of saved bags in the ServletContext.  The Map
     * is from bag names to Collections of objects.
     */
    public static final String SAVED_BAGS = "SAVED_BAGS";

    /**
     * The Session attribute name to use when storing the Map of the saved collections to saved
     * saved collection names.
     */
    public static final String SAVED_BAGS_INVERSE = "SAVED_BAGS_INVERSE";

    /**
     * The attribute name to use when storing the Map of saved queries in the ServletContext.  The
     * Map is from query names to queries.
     */
    public static final String SAVED_QUERIES = "SAVED_QUERIES";

    /**
     * The Session attribute name to use when storing the Map of the saved queries to saved
     * saved query names.
     */
    public static final String SAVED_QUERIES_INVERSE = "SAVED_QUERIES_INVERSE";

    /**
     * The attribute name to use when storing the Map of queries to QueryInfo objects.
     */
    public static final String QUERY_INFO_MAP = "QUERY_INFO_MAP";

    /**
     * The attribute name to use when storing the Query in the ServletContext.
     */
    public static final String QUERY = "QUERY";

    /**
     * Set on the request by RunQueryAction and SaveQueryAction to be the name of the last run
     * query saved. 
     */
    public static final String SAVED_QUERY_NAME = "SAVED_QUERY_NAME";

    /**
     * The attribute name to use when storing the results of a query in the ServletContext.
     */
    public static final String RESULTS_TABLE = "RESULTS_TABLE";

    /**
     * The name of the attribute that is used to store the Properties from web.properties.
     */
    public static final String WEB_PROPERTIES = "WEB_PROPERTIES";
}
