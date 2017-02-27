package org.intermine.web.logic;

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
 * Container for ServletContext and Session attribute names used by the webapp
 *
 * @author Kim Rutherford
 * @author Thomas Riley
 */
public final class Constants
{

    private Constants() {
        // Hidden constructor.
    }

    /**
     * ServletContext attribute used to store web.properties
     */
    public static final String WEB_PROPERTIES = "WEB_PROPERTIES";

    /**
     * Attribute used to store origin information about properties in
     * the context of this web application.
     */
    public static final String PROPERTIES_ORIGINS = "PROPERTIES_ORIGINS";

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
     * Session attribute, name of tab selected on MyMine page.
     */
    public static final String MYMINE_PAGE = "MYMINE_PAGE"; // serializes

    /**
     * ServletContext attribute used to store the WebConfig object for the Model.
     */
    public static final String WEBCONFIG = "WEBCONFIG";

    /**
     * Session attribute used to store the user's Profile
     */
    public static final String PROFILE = "PROFILE"; // serialized as 'username'

    /**
     * Session attribute used to store the current query
     */
    public static final String QUERY = "QUERY";

    /**
     * Session attribute used to store the status new template
     */
    public static final String NEW_TEMPLATE = "NEW_TEMPLATE";

    /**
     * Session attribute used to store the status editing template
     */
    public static final String EDITING_TEMPLATE = "EDITING_TEMPLATE";

    /**
     * Session attribute used to store the previous template name
     */
    public static final String PREV_TEMPLATE_NAME = "PREV_TEMPLATE_NAME";

    /**
     * Servlet context attribute - map from aspect set name to Aspect object.
     */
    public static final String ASPECTS = "ASPECTS";

    /**
     * Session attribute equals Boolean.TRUE when logged in user is superuser.
     */
    public static final String IS_SUPERUSER = "IS_SUPERUSER";

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
     * The session attribute that holds the ReportObjectCache object for the session.
     */
    public static final String REPORT_OBJECT_CACHE = "REPORT_OBJECT_CACHE";

    /**
     * Session attribute that holds cache of table identifiers to PagedTable objects.
     */
    public static final String TABLE_MAP = "TABLE_MAP";

    /**
     * Session attribute.  A Map from query id to QueryMonitor.
     */
    public static final String RUNNING_QUERIES = "RUNNING_QUERIES";

    /**
     * Servlet attribute. Map from MultiKey(experiment, gene) id to temp file name.
     */
    public static final String GRAPH_CACHE = "GRAPH_CACHE";

    /**
     * Servlet attribute. Map from class name to Set of leaf class descriptors.
     */
    public static final String LEAF_DESCRIPTORS_MAP = "LEAF_DESCRIPTORS_MAP";

    /**
     * Servlet attribute. The global webapp cache - a InterMineCache object.
     */
    public static final String GLOBAL_CACHE = "GLOBAL_CACHE";

    /**
     * Maximum size a bag should have if the user is not logged in (to save memory)
     */
    public static final int MAX_NOT_LOGGED_BAG_SIZE = 500;

    /**
     * Servlet attribute.  Contains the SearchRepository for global/public WebSearchable objects.
     */
    public static final String GLOBAL_SEARCH_REPOSITORY = "GLOBAL_SEARCH_REPOSITORY";

    /**
     * Default size of table implemented by PagedTable.
     */
    public static final int DEFAULT_TABLE_SIZE = 25;

    /**
     * Session attribute used to store the size of table with results.
     */
    public static final String RESULTS_TABLE_SIZE = "RESULTS_TABLE_SIZE";

    /**
     * Session attribute used to store WebState object.
     */
    public static final String WEB_STATE = "WEB_STATE";

    /**
     * Current version of the InterMine WebService.
     * This constant must changed every time the API changes, either by addition
     * or deletion of features.
     *
     * 12 - Added ability to filter lists from AvailableListsService
     * 13 - Added ability to serve characterish subsequences
     * 15 - Added jbrowse endpoint.
     * 16 - Added lists with issues, also changed the default output of the id resolution service
     *      which is now category based by default.
     *    - Added JBrowse-names, simple-data service.
     * 17 - Added jbrowse-config.
     * 18 - Added display names to the model output.
     * 19 - Added intermine version
     */
    public static final int WEB_SERVICE_VERSION = 19;

    /**
     * Current version of the InterMine code
     */
    public static final String INTERMINE_VERSION = "1.7.1";

    /**
     * Key for a Map from class name to Boolean.TRUE for all classes in the model that do not have
     * any class keys.
     */
    public static final String KEYLESS_CLASSES_MAP = "KEYLESS_CLASSES_MAP";

    /**
     * Key for the InterMine API object
     */
    public static final String INTERMINE_API = "INTERMINE_API";

    /** Key for the initialiser error **/
    public static final String INITIALISER_KEY_ERROR = "INITIALISER_KEY_ERROR";

    /**
     * key for the map saved in the session containing the status of saved bags
     */
    public static final String SAVED_BAG_STATUS = "SAVED_BAG_STATUS";

    /** Key for the upgrading bag on the session **/
    public static final String UPGRADING_BAG = "UPGRADING";

    /** The replay-attack prevention nonces **/
    public static final String NONCES = "NONCES";

    /** The display name of the current user **/
    public static final String USERNAME = "USERNAME";
    /** The name of the current open-id provider **/
    public static final String PROVIDER = "PROVIDER";

    /**
     * The key for the open-id providers located in the servlet context.
     */
    public static final String OPENID_PROVIDERS = "OPENID_PROVIDERS";

    /* The names of various user preferences known to the web application */

    /** The preference set if the user does not like lots of emails */
    public static final String NO_SPAM = "do_not_spam";

    /** The preference set if the user does not want to be found */
    public static final String HIDDEN = "hidden";

    /** The key under which OAuth2 providers are stored **/
    public static final String OAUTH2_PROVIDERS = "OAUTH2_PROVIDERS";
}
