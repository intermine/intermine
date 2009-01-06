package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2009 FlyMine
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
    public static final String WEB_PROPERTIES = "WEB_PROPERTIES";

    /**
     * ServletContext attribute maps category name to List of TemplateQuerys
     */
    //public static final String CATEGORY_TEMPLATES = "CATEGORY_TEMPLATES";

    /**
     * ServletContext attribute maps a class name to a Map of category names to List of
     * TemplateQuerys.
     */
    //public static final String CLASS_CATEGORY_TEMPLATES = "CLASS_CATEGORY_TEMPLATES";

    /**
     * ServletContext attribute maps a class name to a Map of template names to simple expressions -
     * the expression describes a field that should be set when a template is linked to from the
     * object details page.  eg. Gene.identifier
     */
    //public static final String CLASS_TEMPLATE_EXPRS = "CLASS_TEMPLATE_EXPRS";

    /**
     * ServletContext attribute, List of category names.
     */
    public static final String CATEGORIES = "CATEGORIES";

    /**
     * ServletContext attribute, autocompletion.
     */
    public static final String AUTO_COMPLETER = "AUTO_COMPLETER";
    /**
     * ServletContext attribute, Map from unqualified type name to list of subclass names.
     */
    public static final String SUBCLASSES = "SUBCLASSES";

    /**
     * ServletContext attribute, provides an interface for actions and
     * controllers to query some model meta-data like class counts and
     * field enumerations.
     */
    public static final String OBJECT_STORE_SUMMARY = "OBJECT_STORE_SUMMARY";

    /**
     * Session attribute, name of tab selected on MyMine page.
     */
    public static final String MYMINE_PAGE = "MYMINE_PAGE"; // serializes

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
    public static final String PROFILE = "PROFILE"; // serialized as 'username'

    /**
     * Session attribute used to store the current query
     */
    public static final String QUERY = "QUERY";

    /**
     * Session attribute set of type TemplateBuildState present when query
     * builder is in template building mode.
     */
    public static final String TEMPLATE_BUILD_STATE = "TEMPLATE_BUILD_STATE";

    /**
     * Servlet context attribute - map from aspect set name to Aspect object.
     */
    public static final String ASPECTS = "ASPECTS";

    /**
     * Session attribute equals Boolean.TRUE when logged in user is superuser.
     */
    public static final String IS_SUPERUSER = "IS_SUPERUSER";

    /**
     * Session attribute containing Map containing 'collapsed' state of objectDetails.jsp
     * UI elements.
     */
    public static final String COLLAPSED = "COLLAPSED";

    /**
     * Session attribute that temporarily holds a Vector of messages that will be displayed by the
     * errorMessages.jsp on the next page viewed by the user and then removed (allows message
     * after redirect).
     */
    public static final String MESSAGES = "MESSAGES";

    /**
     * Session attribute that temporarily holds a Vector of errors that will be displayed by the
     * errorMessages.jsp on the next page viewed by the user and then removed (allows errors
     * after redirect).
     */
    public static final String ERRORS = "ERRORS";

    /**
     * Session attribute that temporarily holds messages from the Portal
     */
    public static final String PORTAL_MSG = "PORTAL_MSG";

    /**
     * Session attribute that holds message when a lookup constraint has been used
     */
    public static final String LOOKUP_MSG = "LOOKUP_MSG";

    /**
     * The name of the property that is set to TRUE in the PortalQueryAction Action to indicate
     * to the ObjectDetailsController that we have come from a portal page.
     */
    public static final String PORTAL_QUERY_FLAG = "PORTAL_QUERY_FLAG";

    /**
     * The name of the property to look up to find the maximum size of an inline table.
     */
    public static final String INLINE_TABLE_SIZE = "inline.table.size";

    /**
     * Session attribut containing the default operator name, either 'and' or 'or'.
     */
    public static final String DEFAULT_OPERATOR = "DEFAULT_OPERATOR";

    /**
     * Period of time to wait for client to poll a running query before cancelling the query.
     */
    public static final int QUERY_TIMEOUT_SECONDS = 20;

    /**
     * Refresh period specified on query poll page.
     */
    public static final int POLL_REFRESH_SECONDS = 2;

    /**
     * The session attribute that holds the DisplayObjectCache object for the session.
     */
    public static final String DISPLAY_OBJECT_CACHE = "DISPLAY_OBJECT_CACHE";

    /**
     * Session attribute that holds cache of table identifiers to PagedTable objects.
     */
    public static final String TABLE_MAP = "TABLE_MAP";

    /**
     * Session attribute that holds a map from class name to map from field name to Boolean.TRUE.
     */
    public static final String EMPTY_FIELD_MAP = "EMPTY_FIELD_MAP";

    /**
     * Session attribute.  A Map from query id to QueryMonitor.
     */
    public static final String RUNNING_QUERIES = "RUNNING_QUERIES";

    /**
     * Servlet attribute. Map from class name to Set of leaf class descriptors.
     */
    public static final String LEAF_DESCRIPTORS_MAP = "LEAF_DESCRIPTORS_MAP";

    /**
     * Servlet attribute. Map from MultiKey(experiment, gene) id to temp file name.
     */
    public static final String GRAPH_CACHE = "GRAPH_CACHE";

    /**
     * Servlet attribute. The global webapp cache - a InterMineCache object.
     */
    public static final String GLOBAL_CACHE = "GLOBAL_CACHE";

    /**
     * Maximum size a bag should have if the user is not logged in (to save memory)
     */
    public static final int MAX_NOT_LOGGED_BAG_SIZE = 500;

    /**
     * Servlet attribute.  Map from class name to set of defined keys.
     */
    public static final String CLASS_KEYS = "CLASS_KEYS";

    /**
     * Servlet attribute.  Map from class name to custom bag query.
     */
    public static final String BAG_QUERY_CONFIG = "BAG_QUERY_CONFIG";

    /**
     * Servlet attribute.  Contains the SearchRepository for global/public WebSearchable objects.
     */
    public static final String GLOBAL_SEARCH_REPOSITORY = "GLOBAL_SEARCH_REPOSITORY";

    /**
     * Default size of table implemented by PagedTable.
     */
    public static final int DEFAULT_TABLE_SIZE = 10;

    /**
     * Session attribute used to store the size of table with results.
     */
    public static final String RESULTS_TABLE_SIZE = "RESULTS_TABLE_SIZE";

    /**
     * Session attribute used to store WebState object.
     */
    public static final String WEB_STATE = "WEB_STATE";

    /**
     * Batch size for the underlying objectstore
     */
    public static final int BATCH_SIZE = 500;
}
