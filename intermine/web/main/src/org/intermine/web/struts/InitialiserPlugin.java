package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ModuleConfig;
import org.apache.tools.ant.BuildException;
import org.intermine.api.InterMineAPI;
import org.intermine.api.LinkRedirectManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.BagQueryHelper;
import org.intermine.api.config.ClassKeyHelper;
import org.intermine.api.mines.FriendlyMineManager;
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.search.Scope;
import org.intermine.api.search.SearchRepository;
import org.intermine.api.tag.TagNames;
import org.intermine.api.tracker.Tracker;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.api.tracker.util.TrackerUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.aspects.Aspect;
import org.intermine.web.logic.aspects.AspectBinding;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Initialiser for the InterMine web application.
 * Anything that needs global initialisation goes here.
 *
 * @author Andrew Varley
 * @author Thomas Riley
 */
public class InitialiserPlugin implements PlugIn
{
    private static final Logger LOG = Logger.getLogger(InitialiserPlugin.class);

    ProfileManager profileManager;
    TrackerDelegate trackerDelegate;
    ObjectStore os;
    Set<String> blockingErrorKeys;
    /** The list of tags that mark something as public */
    public static final List<String> PUBLIC_TAG_LIST = Arrays.asList(TagNames.IM_PUBLIC);

    /**
     * Init method called at Servlet initialisation
     *
     * @param servlet ActionServlet that is managing all the modules
     * in this web application
     * @param config ModuleConfig for the module with which this
     * plug-in is associated
     *
     * @throws ServletException if this <code>PlugIn</code> cannot
     * be successfully initialized
     */
    public void init(ActionServlet servlet, ModuleConfig config) throws ServletException {

        // NOTE throwing exceptions other than a ServletException from this class causes the
        // webapp to fail to deploy with no error message.

        final ServletContext servletContext = servlet.getServletContext();
        blockingErrorKeys = new LinkedHashSet<String>();
        SessionMethods.setErrorOnInitialiser(servletContext, blockingErrorKeys);

        // initialise properties
        Properties webProperties = loadWebProperties(servletContext);
        SessionMethods.setWebProperties(servletContext, webProperties);

        loadOpenIDProviders(servletContext);

        // get link redirector
        LinkRedirectManager redirect = getLinkRedirector(webProperties);

        // set up core InterMine application
        os = getProductionObjectStore(webProperties);

        final ObjectStoreWriter userprofileOSW = getUserprofileWriter(webProperties);

        //verify if intermine_state exists in the savedbag table and if it has the right type
        if (!verifyListTables(userprofileOSW)) {
            return;
        }
        //verify if we the webapp needs to upgrade the lists
        verifyListUpgrade(userprofileOSW);

        final ObjectStoreSummary oss = summariseObjectStore(servletContext);
        final Map<String, List<FieldDescriptor>> classKeys = loadClassKeys(os.getModel());
        final BagQueryConfig bagQueryConfig = loadBagQueries(servletContext, os, webProperties);
        trackerDelegate = initTrackers(webProperties, userprofileOSW);
        final InterMineAPI im = new InterMineAPI(os, userprofileOSW, classKeys, bagQueryConfig,
                oss, trackerDelegate, redirect);
        SessionMethods.setInterMineAPI(servletContext, im);

        // need a global reference to ProfileManager so it can be closed cleanly on destroy
        profileManager = im.getProfileManager();

        // read in additional webapp specific information and put in servletContext
        WebConfig webConfig = loadWebConfig(servletContext, os);

        loadAspectsConfig(servletContext);
        loadClassDescriptions(servletContext);

        // index global webSearchables
        final Profile superProfile = im.getProfileManager().getSuperuserProfile();
        SearchRepository searchRepository =
            new SearchRepository(superProfile, Scope.GLOBAL);
        SessionMethods.setGlobalSearchRepository(servletContext, searchRepository);

        servletContext.setAttribute(Constants.GRAPH_CACHE, new HashMap<String, String>());

        loadAutoCompleter(servletContext, os);

        cleanTags(im.getTagManager());

        Map<String, Boolean> keylessClasses = new HashMap<String, Boolean>();
        for (ClassDescriptor cld : os.getModel().getClassDescriptors()) {
            boolean keyless = true;
            for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig, cld)) {
                if ((fc.getDisplayer() == null) && fc.getShowInSummary()) {
                    keyless = false;
                    break;
                }
            }
            if (keyless) {
                keylessClasses.put(TypeUtil.unqualifiedName(cld.getName()), Boolean.TRUE);
            }
        }
        servletContext.setAttribute(Constants.KEYLESS_CLASSES_MAP, keylessClasses);

        setupClassSummaryInformation(servletContext, oss, os.getModel());

        doRegistration(webProperties);

        FriendlyMineManager friendlyMineManager
            = FriendlyMineManager.getInstance(im, webProperties);
        im.setFriendlyMineManager(friendlyMineManager);
    }

    private void doRegistration(Properties webProperties) {
        Registrar reg = new Registrar(webProperties);
        reg.start();
    }

    private ObjectStore getProductionObjectStore(Properties webProperties) throws ServletException {
        String osAlias = (String) webProperties.get("webapp.os.alias");
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                cause.printStackTrace();
            }
            throw new ServletException("Unable to instantiate ObjectStore " + osAlias, e);
        }
        return os;
    }

    private void loadAspectsConfig(ServletContext servletContext) {
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/aspects.xml");
        if (is == null) {
            LOG.info("Unable to find /WEB-INF/aspects.xml, there will be no aspects");
            SessionMethods.setAspects(servletContext, Collections.EMPTY_MAP);
            SessionMethods.setCategories(servletContext, Collections.EMPTY_SET);
        } else {
            Map<String, Aspect> aspects;
            try {
                aspects = AspectBinding.unmarhsal(is);
            } catch (Exception e) {
                throw new RuntimeException("problem while reading aspect configuration file", e);
            }
            SessionMethods.setAspects(servletContext, aspects);
            SessionMethods.setCategories(servletContext, Collections.unmodifiableSet(aspects
                        .keySet()));
        }
    }

    private void loadAutoCompleter(ServletContext servletContext,
            ObjectStore os) throws ServletException {
        if (os instanceof ObjectStoreInterMineImpl) {
            Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
            try {
                InputStream is = MetadataManager.retrieveBLOBInputStream(db,
                        MetadataManager.AUTOCOMPLETE_INDEX);
                AutoCompleter ac;

                if (is != null) {
                    ac = new AutoCompleter(is);
                    SessionMethods.setAutoCompleter(servletContext, ac);
                } else {
                    ac = null;
                    LOG.warn("No AutoCompleter index found in database.");
                }
            } catch (SQLException e) {
                LOG.error("Problem with database", e);
                throw new ServletException("Problem with database", e);
            }
        }
    }

    /**
     * Object and widget display configuration
     */
    private WebConfig loadWebConfig(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        try {
            WebConfig retval = WebConfig.parse(servletContext, os.getModel());
            SessionMethods.setWebConfig(servletContext, retval);
            return retval;
        } catch (Exception e) {
            LOG.error("Problem generating WebConfig", e);
            throw new ServletException(e);
        }
    }

    /**
     *  Load user-friendly class description
     */
    private void loadClassDescriptions(ServletContext servletContext)
        throws ServletException {
        Properties classDescriptions = new Properties();
        try {
            classDescriptions.load(servletContext
                    .getResourceAsStream("/WEB-INF/classDescriptions.properties"));
        } catch (Exception e) {
            throw new ServletException("Error loading class descriptions", e);
        }
        servletContext.setAttribute("classDescriptions", classDescriptions);
    }

    /**
     * Load keys that describe how objects should be uniquely identified
     */
    private Map<String, List<FieldDescriptor>> loadClassKeys(Model model) throws ServletException {
        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(InitialiserPlugin.class.getClassLoader()
                    .getResourceAsStream("class_keys.properties"));
        } catch (Exception e) {
            throw new ServletException("Error loading class descriptions", e);
        }
        Map<String, List<FieldDescriptor>>  classKeys =
            ClassKeyHelper.readKeys(model, classKeyProps);
        return classKeys;
    }

    /**
     * Load keys that describe how objects should be uniquely identified
     */
    private BagQueryConfig loadBagQueries(ServletContext servletContext, ObjectStore os, Properties webProperties)
        throws ServletException {
        BagQueryConfig bagQueryConfig = null;
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/bag-queries.xml");
        if (is != null) {
            try {
                bagQueryConfig = BagQueryHelper.readBagQueryConfig(os.getModel(), is);
            } catch (Exception e) {
                throw new ServletException("Error loading class bag queries", e);
            }
            InputStream isBag = getClass().getClassLoader().getResourceAsStream("extraBag.properties");
            Properties bagProperties = new Properties();
            if (isBag != null) {
                try {
                    bagProperties.load(isBag);
                    bagQueryConfig.setConnectField(bagProperties.getProperty("extraBag.connectField"));
                    bagQueryConfig.setExtraConstraintClassName(bagProperties.getProperty("extraBag.className"));
                    bagQueryConfig.setConstrainField(bagProperties.getProperty("extraBag.constrainField"));
                } catch (IOException e) {
                      throw new ServletException(e);
                }
            } else {
                LOG.error("Could not find extraBag.properties file");
            }
        } else {
            // can used defaults so just log a warning
            LOG.warn("No custom bag queries found - using default query");
        }
        return bagQueryConfig;
    }

    /**
     * Read in the webapp configuration properties
     */
    private Properties loadWebProperties(ServletContext servletContext) throws ServletException {
        Properties webProperties = new Properties();
        InputStream globalPropertiesStream =
            servletContext.getResourceAsStream("/WEB-INF/global.web.properties");
        try {
            webProperties.load(globalPropertiesStream);
        } catch (Exception e) {
            throw new ServletException("Unable to find global.web.properties", e);
        }

        LOG.info("Looking for extra property files");
        Pattern pattern = Pattern.compile(
            "/WEB-INF/(?!global)\\w+\\.web\\.properties$");
        ResourceFinder finder = new ResourceFinder(servletContext);

        Collection<String> otherResources = finder.findResourcesMatching(pattern);
        for (String resource : otherResources) {
            LOG.info("Loading extra resources from " + resource);
            InputStream otherResourceStream =
                servletContext.getResourceAsStream(resource);
            try {
                webProperties.load(otherResourceStream);
            } catch (Exception e) {
                throw new ServletException("Unable to load " + resource, e);
            }
        }

        // Load these last, as they always take precedence.
        InputStream modelPropertiesStream =
            servletContext.getResourceAsStream("/WEB-INF/web.properties");
        if (modelPropertiesStream == null) {
            // there are no model specific properties
        } else {
            try {
                webProperties.load(modelPropertiesStream);
            } catch (Exception e) {
                throw new ServletException("Unable to find web.properties", e);
            }
        }

        return webProperties;
    }

    private void loadOpenIDProviders(ServletContext context) throws ServletException {
        Set<String> providers = new HashSet<String>();
        Properties providerProps = new Properties();

        InputStream is = getClass().getClassLoader().getResourceAsStream("openid-providers.properties");
        if (is == null) {
            LOG.info("couldn't find openid providers, using system class-loader");
            is = ClassLoader.getSystemClassLoader().getResourceAsStream("openid-properties.properties");
        }
        if (is != null) {
            try {
                providerProps.load(is);
            } catch (IOException e) {
                throw new ServletException(e);
            }
        } else {
            LOG.error("Could not find openid-providers.properties");
        }

        for (Object key: providerProps.keySet()) {
            String keyString = (String) key;
            if (!keyString.endsWith(".alias")) {
                providers.add(keyString);
                LOG.info("Added " + keyString);
            }
        }

        SessionMethods.setOpenIdProviders(context, providers);
    }


    private LinkRedirectManager getLinkRedirector(Properties webProperties) {
        final String err = "Initialisation of link redirector failed: ";
        String linkRedirector = (String) webProperties.get("webapp.linkRedirect");
        if (linkRedirector == null) {
            return null;
        }
        Class<?> c = TypeUtil.instantiate(linkRedirector);
        Constructor<?> constr = null;
        try {
            constr = c.getConstructor(new Class[] {Properties.class});
        } catch (NoSuchMethodException e) {
            LOG.error(err, e);
            return null;
        }
        LinkRedirectManager redirector = null;
        try {
            redirector = (LinkRedirectManager) constr.newInstance(
                    new Object[] {webProperties});
        } catch (IllegalArgumentException e) {
            LOG.error(err, e);
        } catch (InstantiationException e) {
            LOG.error(err, e);
        } catch (IllegalAccessException e) {
            LOG.error(err, e);
        } catch (InvocationTargetException e) {
            LOG.error(err, e);
        }
        return redirector;
    }

    /**
     * Summarize the ObjectStore to get class counts
     */
    private ObjectStoreSummary summariseObjectStore(ServletContext servletContext)
        throws ServletException {
        Properties objectStoreSummaryProperties = new Properties();
        InputStream objectStoreSummaryPropertiesStream =
            servletContext.getResourceAsStream("/WEB-INF/objectstoresummary.properties");
        if (objectStoreSummaryPropertiesStream == null) {
            // there are no model specific properties
            throw new ServletException("Unable to find objectstoresummary.properties");
        }
        try {
            objectStoreSummaryProperties.load(objectStoreSummaryPropertiesStream);
        } catch (Exception e) {
            throw new ServletException("Unable to read objectstoresummary.properties", e);
        }

        final ObjectStoreSummary oss = new ObjectStoreSummary(objectStoreSummaryProperties);
        return oss;
    }

    private void setupClassSummaryInformation(ServletContext servletContext, ObjectStoreSummary oss,
            final Model model) throws ServletException {
        Map<String, String> classes = new LinkedHashMap<String, String>();
        Map<String, Integer> classCounts = new LinkedHashMap<String, Integer>();

        for (String className : new TreeSet<String>(model.getClassNames())) {
            if (!className.equals(InterMineObject.class.getName())) {
                classes.put(className, TypeUtil.unqualifiedName(className));
            }
            try {
                classCounts.put(className, new Integer(oss.getClassCount(className)));
            } catch (Exception e) {
                throw new ServletException("Unable to get class count for " + className, e);
            }
        }
        servletContext.setAttribute("classes", classes);
        servletContext.setAttribute("classCounts", classCounts);
        // Build subclass lists for JSPs
        Map<String, List<String>> subclassesMap = new LinkedHashMap<String, List<String>>();
        for (ClassDescriptor cld : model.getClassDescriptors()) {
            ArrayList<String> subclasses = new ArrayList<String>();
            for (String thisClassName : new TreeSet<String>(getChildren(cld))) {
                if (classCounts.get(thisClassName).intValue() > 0) {
                    subclasses.add(TypeUtil.unqualifiedName(thisClassName));
                }
            }
            subclassesMap.put(TypeUtil.unqualifiedName(cld.getName()), subclasses);
        }
        servletContext.setAttribute(Constants.SUBCLASSES, subclassesMap);
        // Map from class name to Map from reference name to Boolean.TRUE if empty ref/collection
        Map emptyFields = new HashMap();
        for (Iterator iter = model.getClassNames().iterator(); iter.hasNext();) {
            String classname = (String) iter.next();
            Set nullFields = oss.getNullReferencesAndCollections(classname);
            Map boolMap = new HashMap();
            emptyFields.put(TypeUtil.unqualifiedName(classname), boolMap);
            if (nullFields != null && nullFields.size() > 0) {
                for (Iterator fiter = nullFields.iterator(); fiter.hasNext();) {
                    boolMap.put(fiter.next(), Boolean.TRUE);
                }
            }
        }
        servletContext.setAttribute(Constants.EMPTY_FIELD_MAP, emptyFields);
    }


    private ObjectStoreWriter getUserprofileWriter(Properties webProperties)
        throws ServletException {
        ObjectStoreWriter userprofileOSW;
        try {
            String userProfileAlias = (String) webProperties.get("webapp.userprofile.os.alias");
            userprofileOSW = ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
        } catch (ObjectStoreException e) {
            LOG.error("Unable to create userprofile - please check that the "
                    + "userprofile database is available", e);
            throw new ServletException("Unable to create profile manager - please check that "
                    + "the userprofile database is available", e);
        }

        applyUserProfileUpgrades(userprofileOSW);
        return userprofileOSW;
    }

    private void applyUserProfileUpgrades(ObjectStoreWriter osw) throws ServletException {
        Connection con = null;
        try {
            con = ((ObjectStoreInterMineImpl) osw).getConnection();
            DatabaseUtil.addColumn(con, "userprofile", "apikey", DatabaseUtil.Type.text);
            if (!DatabaseUtil.columnExists(con, "userprofile", "localaccount")) {
                DatabaseUtil.addColumn(con, "userprofile", "localaccount",
                        DatabaseUtil.Type.boolean_type);
                DatabaseUtil.updateColumnValue(con, "userprofile", "localaccount", true);
            }
        } catch (SQLException sqle) {
            LOG.error("Problem retrieving connection", sqle);
            throw new ServletException("Unable to upgrade UserProfile DB");
        } finally {
            ((ObjectStoreInterMineImpl) osw).releaseConnection(con);
        }
    }

    /**
     * Destroy method called at Servlet destroy
     */
    public void destroy() {
        try {
            if (profileManager != null) {
                profileManager.close();
            }
            if (trackerDelegate != null) {
                trackerDelegate.close();
            }
            ((ObjectStoreInterMineImpl) os).close();
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Remove class tags from the user profile that refer to classes that non longer exist
     * @param tagManager tag manager
     */
    protected static void cleanTags(TagManager tagManager) {
        List<Tag> classTags = tagManager.getTags(null, null, "class", null);

        for (Tag tag : classTags) {
            // check that class exists
            try {
                Class.forName(tag.getObjectIdentifier());
            } catch (ClassNotFoundException e) {
                tagManager.deleteTag(tag);
            }
        }
    }

    /**
     * Get the names of the type of this ClassDescriptor and all its descendants
     * @param cld the ClassDescriptor
     * @return a Set of class names
     */
    protected static Set<String> getChildren(ClassDescriptor cld) {
        Set<String> children = new HashSet<String>();
        getChildren(cld, children);
        return children;
    }

    /**
     * Add the names of the descendents of a ClassDescriptor to a Set
     * @param cld the ClassDescriptor
     * @param children the Set of child names
     */
    protected static void getChildren(ClassDescriptor cld, Set<String> children) {
        for (ClassDescriptor child : cld.getSubDescriptors()) {
            children.add(child.getName());
            getChildren(child, children);
        }
    }

    private TrackerDelegate initTrackers(Properties webProperties,
            ObjectStoreWriter userprofileOSW) {
        if (!verifyTrackTables(userprofileOSW.getObjectStore())) {
            blockingErrorKeys.add("errors.tracktable.runAnt");
        }
        return getTrackerDelegate(webProperties, userprofileOSW);
    }

    private boolean verifyTrackTables(ObjectStore uos) {
        Connection con = null;
        try {
            con = ((ObjectStoreInterMineImpl) uos).getConnection();
            if (DatabaseUtil.tableExists(con, TrackerUtil.TEMPLATE_TRACKER_TABLE)) {
                ResultSet res = con.getMetaData().getColumns(null, null,
                                TrackerUtil.TEMPLATE_TRACKER_TABLE, "timestamp");

                while (res.next()) {
                    if (res.getString(3).equals(TrackerUtil.TEMPLATE_TRACKER_TABLE)
                        && res.getString(4).equals("timestamp")
                        && res.getInt(5) == Types.TIMESTAMP) {
                        return true;
                    }
                    return false;
                }
            }
        } catch (SQLException sqle) {
            LOG.error("Probelm retriving connection", sqle);
        } finally {
            ((ObjectStoreInterMineImpl) uos).releaseConnection(con);
        }
        return true;
    }
    /**
     * Returns the tracker manager of all trackers defined into the webapp configuration properties
     * @param webProperties the webapp configuration properties where the trackers are defined
     * @param userprofileOSW the object store writer to retrieve the database connection
     * @return TrackerManager the trackers manager
     */
    private TrackerDelegate getTrackerDelegate(Properties webProperties,
            ObjectStoreWriter userprofileOSW) {
        Map<String, Tracker> trackers = new HashMap<String, Tracker>();
        String trackerList = (String) webProperties.get("webapp.trackers");
        LOG.warn("initializeTrackers: trackerList is" + trackerList);
        if (trackerList != null) {
            String[] trackerClassNames = trackerList.split(",");
            TrackerDelegate td = new TrackerDelegate(trackerClassNames, userprofileOSW);
            return td;
        }
        return null;
    }

    private boolean verifyListTables(ObjectStore uos) {
        Connection con = null;
        try {
            con = ((ObjectStoreInterMineImpl) uos).getConnection();
            if (!DatabaseUtil.tableExists(con, "bagvalues")) {
                blockingErrorKeys.add("errors.savedbagtable.runLoadBagValuesTableAnt");
                return false;
            } else {
                if (!DatabaseUtil.columnExists(con, "bagvalues", "extra")
                    || DatabaseUtil.columnExists(con, "savedbag", "intermine_current")) {
                    blockingErrorKeys.add("errors.savedbagtable.runListTablesAnt");
                    return false;
                }
            }
        } catch (SQLException sqle) {
            LOG.error("Probelm retrieving connection", sqle);
        } finally {
            ((ObjectStoreInterMineImpl) uos).releaseConnection(con);
        }
        return true;
    }

    /**
     * Verify if we need to upgrade the list
     */
    private void verifyListUpgrade(ObjectStore uosw) {
        try {
            boolean listUpgrade = false;
            String productionSerialNumber = MetadataManager.retrieve(((ObjectStoreInterMineImpl) os)
                .getDatabase(), MetadataManager.SERIAL_NUMBER);
            String userprofileSerialNumber = MetadataManager.retrieve(
                ((ObjectStoreInterMineImpl) uosw).getDatabase(), MetadataManager.SERIAL_NUMBER);
            LOG.info("Production database has serialNumber \"" + productionSerialNumber + "\"");
            LOG.info("Userprofile database has serialNumber \"" + userprofileSerialNumber + "\"");
            if (productionSerialNumber != null) {
                if (userprofileSerialNumber == null
                    || !userprofileSerialNumber.equals(productionSerialNumber)) {
                    listUpgrade = true;
                }
            }
            if (productionSerialNumber == null && userprofileSerialNumber != null) {
                listUpgrade = true;
            }
            if (listUpgrade) {
                    LOG.warn("Serial numbers not equal: list upgrate needed");
                    //set current attribute to false
                    Connection conn = null;
                    try {
                        conn = ((ObjectStoreInterMineImpl) uosw).getDatabase().getConnection();
                        if (DatabaseUtil.columnExists(conn, "savedbag", "intermine_state")) {
                            DatabaseUtil.updateColumnValue(
                                         ((ObjectStoreInterMineImpl) uosw).getDatabase(),
                                         "savedbag", "intermine_state", BagState.NOT_CURRENT.toString());
                        }
                    } catch (SQLException sqle) {
                        throw new BuildException("Problems connecting bagvalues table", sqle);
                    } finally {
                        try {
                            if (conn != null) {
                                conn.close();
                            }
                        } catch (SQLException sqle) {
                        }
                    }
                    // update the userprofileSerialNumber
                    MetadataManager.store(((ObjectStoreInterMineImpl) uosw).getDatabase(),
                            MetadataManager.SERIAL_NUMBER, productionSerialNumber);
            }
        } catch (SQLException sqle) {
            throw new IllegalStateException("Error verifying list upgrading", sqle);
        }
    }
}
