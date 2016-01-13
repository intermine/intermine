package org.intermine.api.tracker.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 *
 * @author Daniela
 */
public final class TrackerUtil
{
    /** name of class that tracks templates **/
    public static final String TEMPLATE_TRACKER = "TemplateTracker";
    /** name of table that tracks templates **/
    public static final String TEMPLATE_TRACKER_TABLE = "templatetrack";
    /** name of class that tracks lists **/
    public static final String LIST_TRACKER = "ListTracker";
    /** name of table that tracks lists **/
    public static final String LIST_TRACKER_TABLE = "listtrack";
    /** name of class that tracks lists **/
    public static final String LOGIN_TRACKER = "LoginTracker";
    /** name of table that tracks logins **/
    public static final String LOGIN_TRACKER_TABLE = "logintrack";
    /** name of class that tracks searches **/
    public static final String SEARCH_TRACKER = "SearchTracker";
    /** name of table that tracks searches **/
    public static final String SEARCH_TRACKER_TABLE = "searchtrack";
    /** name of class that tracks queries **/
    public static final String QUERY_TRACKER = "QueryTracker";
    /** name of table that tracks queries **/
    public static final String QUERY_TRACKER_TABLE = "querytrack";

    private TrackerUtil() {
        // just don't
    }
}
