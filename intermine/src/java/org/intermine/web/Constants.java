package org.intermine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
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
     * The attribute name to use when storing the Model in the ServletContext.
     */
    public static final String MODEL = "MODEL";

    /**
     * The attribute name to use when storing the editingAlias in the ServletContext.  This is the
     * alias of the QueryClass that is currently being editing.  It is used as a key to look up the
     * QueryClass in the queryClasses Map.
     */
    public static final String EDITING_ALIAS = "EDITING_ALIAS";

    /**
     * The attribute name to use when storing the queryClasses Map in the ServletContext.
     * queryClasses is a Map from aliases to DisplayQueryClass objects.
     */
    public static final String QUERY_CLASSES = "QUERY_CLASSES";

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
     * The attribute name to use when storing the Query in the ServletContext.
     */
    public static final String QUERY = "QUERY";

    /**
     * The attribute name to use when storing the results of a query in the ServletContext.
     */
    public static final String RESULTS_TABLE = "RESULTS_TABLE";

    /**
     * The name of the attribute that is used to indicate advanced mode.
     */
    public static final String ADVANCED_MODE = "ADVANCED_MODE";
}
