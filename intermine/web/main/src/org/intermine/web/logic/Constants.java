package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
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
 * @author Thomas Riley
 */
public interface Constants
{
    /**
     * ServletContext attribute used to store web.properties
     */
    String WEB_PROPERTIES = "WEB_PROPERTIES";

    /**
     * ServletContext attribute, List of category names.
     */
    String CATEGORIES = "CATEGORIES";

    /**
     * ServletContext attribute, autocompletion.
     */
    String AUTO_COMPLETER = "AUTO_COMPLETER";
    /**
     * ServletContext attribute, Map from unqualified type name to list of subclass names.
     */
    String SUBCLASSES = "SUBCLASSES";

    /**
     * Session attribute, name of tab selected on MyMine page.
     */
    String MYMINE_PAGE = "MYMINE_PAGE"; // serializes

    /**
     * ServletContext attribute used to store the WebConfig object for the Model.
     */
    String WEBCONFIG = "WEBCONFIG";

    /**
     * Session attribute used to store the user's Profile
     */
    String PROFILE = "PROFILE"; // serialized as 'username'

    /**
     * Session attribute used to store the current query
     */
    String QUERY = "QUERY";

    /**
     * Session attribute used to store the status new template
     */
    String NEW_TEMPLATE = "NEW_TEMPLATE";

    /**
     * Session attribute used to store the status editing template
     */
    String EDITING_TEMPLATE = "EDITING_TEMPLATE";

    /**
     * Session attribute used to store the previous template name
     */
    String PREV_TEMPLATE_NAME = "PREV_TEMPLATE_NAME";

    /**
     * Servlet context attribute - map from aspect set name to Aspect object.
     */
    String ASPECTS = "ASPECTS";

    /**
     * Session attribute equals Boolean.TRUE when logged in user is superuser.
     */
    String IS_SUPERUSER = "IS_SUPERUSER";

    /**
     * Session attribute that temporarily holds a Vector of messages that will be displayed by the
     * errorMessages.jsp on the next page viewed by the user and then removed (allows message
     * after redirect).
     */
    String MESSAGES = "MESSAGES";

    /**
     * Session attribute that temporarily holds a Vector of errors that will be displayed by the
     * errorMessages.jsp on the next page viewed by the user and then removed (allows errors
     * after redirect).
     */
    String ERRORS = "ERRORS";

    /**
     * Session attribute that temporarily holds messages from the Portal
     */
    String PORTAL_MSG = "PORTAL_MSG";

    /**
     * Session attribute that holds message when a lookup constraint has been used
     */
    String LOOKUP_MSG = "LOOKUP_MSG";

    /**
     * The name of the property to look up to find the maximum size of an inline table.
     */
    String INLINE_TABLE_SIZE = "inline.table.size";

    /**
     * Session attribut containing the default operator name, either 'and' or 'or'.
     */
    String DEFAULT_OPERATOR = "DEFAULT_OPERATOR";

    /**
     * Period of time to wait for client to poll a running query before cancelling the query.
     */
    int QUERY_TIMEOUT_SECONDS = 20;

    /**
     * Refresh period specified on query poll page.
     */
    int POLL_REFRESH_SECONDS = 2;

    /**
     * The session attribute that holds the DisplayObjectCache object for the session.
     */
    String DISPLAY_OBJECT_CACHE = "DISPLAY_OBJECT_CACHE";

    /**
     * Session attribute that holds cache of table identifiers to PagedTable objects.
     */
    String TABLE_MAP = "TABLE_MAP";

    /**
     * Session attribute that holds a map from class name to map from field name to Boolean.TRUE.
     */
    String EMPTY_FIELD_MAP = "EMPTY_FIELD_MAP";

    /**
     * Session attribute.  A Map from query id to QueryMonitor.
     */
    String RUNNING_QUERIES = "RUNNING_QUERIES";

    /**
     * Servlet attribute. Map from class name to Set of leaf class descriptors.
     */
    String LEAF_DESCRIPTORS_MAP = "LEAF_DESCRIPTORS_MAP";

    /**
     * Servlet attribute. Map from MultiKey(experiment, gene) id to temp file name.
     */
    String GRAPH_CACHE = "GRAPH_CACHE";

    /**
     * Servlet attribute. The global webapp cache - a InterMineCache object.
     */
    String GLOBAL_CACHE = "GLOBAL_CACHE";

    /**
     * Maximum size a bag should have if the user is not logged in (to save memory)
     */
    int MAX_NOT_LOGGED_BAG_SIZE = 500;

    /**
     * Servlet attribute.  Contains the SearchRepository for global/public WebSearchable objects.
     */
    String GLOBAL_SEARCH_REPOSITORY = "GLOBAL_SEARCH_REPOSITORY";

    /**
     * Default size of table implemented by PagedTable.
     */
    int DEFAULT_TABLE_SIZE = 25;

    /**
     * Session attribute used to store the size of table with results.
     */
    String RESULTS_TABLE_SIZE = "RESULTS_TABLE_SIZE";

    /**
     * Session attribute used to store WebState object.
     */
    String WEB_STATE = "WEB_STATE";

    /**
     * Current version of the InterMine WebService.
     * This constant must changed every time the API changes, either by addition
     * or deletion of features.
     */
    int WEB_SERVICE_VERSION = 3;

    /**
     * Key for a Map from class name to Boolean.TRUE for all classes in the model that do not have
     * any class keys.
     */
    String KEYLESS_CLASSES_MAP = "KEYLESS_CLASSES_MAP";

    /**
     * Key for the InterMine API object
     */
    String INTERMINE_API = "INTERMINE_API";
}
