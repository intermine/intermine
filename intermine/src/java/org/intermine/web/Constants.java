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
     * ServletContext attribute used to store the Map of class names to Displayer objects and
     * className+"."+fieldName to Displayer objects.
     */
    public static final String DISPLAYERS = "DISPLAYERS";

    /**
     * ServletContext attribute used to store the WebConfig object for the Model.
     */
    public static final String WEBCONFIG = "WEBCONFIG";

    /**
     * ServletContext attribute used to store the ObjectStore
     */
    public static final String OBJECTSTORE = "OBJECTSTORE";

    /**
     * ServletContext attribute used to store the ProfileManager
     */
    public static final String PROFILE_MANAGER = "PROFILE_MANAGER";

    /**
     * Session attribute used to store the user's Profile
     */
    public static final String PROFILE = "PROFILE";

    /**
     * Session attribute used to store the current query
     */
    public static final String QUERY = "QUERY";

    /**
     * Session attribute used to store the results of running the current query
     */
    public static final String QUERY_RESULTS = "QUERY_RESULTS";

    /**
     * Session attribute used to store the active results table (which may be QUERY_RESULTS)
     */
    public static final String RESULTS_TABLE = "RESULTS_TABLE";
}
