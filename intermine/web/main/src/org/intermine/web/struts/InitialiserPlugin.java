package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
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
import org.intermine.api.profile.BagState;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.api.profile.TagManager;
import org.intermine.api.profile.UserNotFoundException;
import org.intermine.api.search.GlobalRepository;
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
import org.intermine.model.userprofile.UserProfile;
import org.intermine.modelproduction.MetadataManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.context.InterMineContext;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.aspects.Aspect;
import org.intermine.web.logic.aspects.AspectBinding;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.FieldConfigHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.query.result.XMLValidator;
import org.jfree.util.Log;

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
    Map<String, String> blockingErrorKeys;
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
        initBlockingErrors(servletContext);

        // initialise properties
        Properties webProperties = loadWebProperties(servletContext);

        // read in additional webapp specific information and put in servletContext
        loadAspectsConfig(servletContext);
        loadClassDescriptions(servletContext);
        loadOpenIDProviders(servletContext);

        // get link redirector
        LinkRedirectManager redirect = getLinkRedirector(webProperties);

        // set up core InterMine application
        os = getProductionObjectStore(webProperties);
        WebConfig webConfig = null;
        if (os != null) {
            webConfig = loadWebConfig(servletContext, os);
        }

        final ObjectStoreWriter userprofileOSW = getUserprofileWriter(webProperties);

        if (userprofileOSW != null) {
            //verify all table mapping classes exist in the userprofile db
            if (!verifyTablesExist(userprofileOSW)) {
                return;
            }
            //verify if intermine_state exists in the savedbag table and if it has the right type
            if (!verifyListTables(userprofileOSW)) {
                return;
            }
            //verify if we the webapp needs to upgrade the lists
            verifyListUpgrade(userprofileOSW);
        }

        final ObjectStoreSummary oss = summariseObjectStore(servletContext);

        if (webProperties != null && userprofileOSW != null) {
            trackerDelegate = initTrackers(webProperties, userprofileOSW);
        }

        if (os != null && webProperties != null) {
            final Map<String, List<FieldDescriptor>> classKeys = loadClassKeys(os.getModel());
            final BagQueryConfig bagQueryConfig = loadBagQueries(servletContext, os, webProperties);

            if (userprofileOSW != null) {
                final InterMineAPI im;
                try {
                    im = new InterMineAPI(os, userprofileOSW, classKeys, bagQueryConfig,
                                          oss, trackerDelegate, redirect);
                } catch (UserNotFoundException unfe) {
                    blockingErrorKeys.put("errors.init.superuser", null);
                    return;
                }
                SessionMethods.setInterMineAPI(servletContext, im);

                InterMineContext.initilise(im, webProperties, webConfig);

                // need a global reference to ProfileManager so it can be closed cleanly on destroy
                profileManager = im.getProfileManager();

                //verify superuser setted in the db matches with the user in the properties file
                final Profile superProfile = im.getProfileManager().getSuperuserProfile();
                if (!superProfile.getUsername()
                    .equals(PropertiesUtil.getProperties().getProperty("superuser.account").trim())) {
                    blockingErrorKeys.put("errors.init.superuser", null);
                }
                // index global webSearchables
                SearchRepository searchRepository = new GlobalRepository(superProfile);
                SessionMethods.setGlobalSearchRepository(servletContext, searchRepository);

                servletContext.setAttribute(Constants.GRAPH_CACHE, new HashMap<String, String>());

                loadAutoCompleter(servletContext, os);

                cleanTags(im.getTagManager());

                if (webConfig != null) {
                    Map<String, Boolean> keylessClasses = new HashMap<String, Boolean>();
                    for (ClassDescriptor cld : os.getModel().getClassDescriptors()) {
                        boolean keyless = true;
                        for (FieldConfig fc : FieldConfigHelper.getClassFieldConfigs(webConfig,
                                                                                     cld)) {
                            if ((fc.getDisplayer() == null) && fc.getShowInSummary()) {
                                keyless = false;
                                break;
                            }
                        }
                        if (keyless) {
                            keylessClasses.put(TypeUtil.unqualifiedName(cld.getName()),
                                                                        Boolean.TRUE);
                        }
                    }
                    servletContext.setAttribute(Constants.KEYLESS_CLASSES_MAP, keylessClasses);
                }

                if (oss != null) {
                    setupClassSummaryInformation(servletContext, oss, os.getModel());
                }

                doRegistration(webProperties);
            }
        }
    }

    private void initBlockingErrors(ServletContext servletContext) {
        blockingErrorKeys = new HashMap<String, String>();
        SessionMethods.setErrorOnInitialiser(servletContext, blockingErrorKeys);
    }

    private void doRegistration(Properties webProperties) {
        Registrar reg = new Registrar(webProperties);
        reg.start();
    }

    private ObjectStore getProductionObjectStore(Properties webProperties) {
        String osAlias = (String) webProperties.get("webapp.os.alias");
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
        } catch (Exception e) {
            LOG.error("Unable to create ObjectStore - " + osAlias + " " + e.getMessage() , e);
            blockingErrorKeys.put("errors.init.objectstoreconnection", e.getMessage());
        }
        return os;
    }

    private void loadAspectsConfig(ServletContext servletContext) {
        InputStream xmlInputStream = servletContext.getResourceAsStream("/WEB-INF/aspects.xml");
        InputStream xmlInputStreamForValidate = servletContext
            .getResourceAsStream("/WEB-INF/aspects.xml");
        if (xmlInputStream == null) {
            LOG.info("Unable to find /WEB-INF/aspects.xml, there will be no aspects");
            SessionMethods.setAspects(servletContext, Collections.EMPTY_MAP);
            SessionMethods.setCategories(servletContext, Collections.EMPTY_SET);
        } else {
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(xmlInputStreamForValidate, writer);
            } catch (IOException ioe) {
                LOG.error("Problems converting xmlInputStream into a String ", ioe);
                blockingErrorKeys.put("errors.init.aspects.generic", ioe.getMessage());
                return;
            }
            String xml = writer.toString();
            String xmlSchemaUrl = "";
            try {
                xmlSchemaUrl = servletContext.getResource("/WEB-INF/aspects.xsd").toString();
            } catch (MalformedURLException mue) {
                LOG.warn("Problems retrieving url fo aspects.xsd ", mue);
            }
            Map<String, Aspect> aspects;
            if (validateXML(xml, xmlSchemaUrl, "errors.init.aspects.validation")) {
                try {
                    aspects = AspectBinding.unmarhsal(xmlInputStream);
                } catch (Exception e) {
                    LOG.error("problem while reading aspect configuration file", e);
                    blockingErrorKeys.put("errors.init.aspects", e.getMessage());
                    return;
                }
                SessionMethods.setAspects(servletContext, aspects);
                SessionMethods.setCategories(servletContext, Collections.unmodifiableSet(aspects
                                            .keySet()));
            }
        }
    }

    private boolean validateXML(String xml, String schemaUrl, String errorCode) {
/*        Source xmlFile = new StreamSource(xmlInputStream);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(new StreamSource(xsdInputStream));
            Validator validator = schema.newValidator();
            try {
                validator.validate(xmlFile);
                return true;
            } catch (SAXException se) {
                LOG.error(xmlFile.getSystemId() + " is NOT valid");
                blockingErrorKeys.put(errorCode, se.getMessage());
            } catch (IOException ioe) {
                LOG.error("Problems find file ", ioe);
            }
        } catch (SAXException se) {
            LOG.error("Problems parsing xsd file", se);
        }
        return false;*/
        XMLValidator validator = new XMLValidator();
        validator.validate(xml, schemaUrl);
        if (validator.getErrorsAndWarnings().size() == 0) {
            return true;
        }
        blockingErrorKeys.put(errorCode, validator.getErrorsAndWarnings().get(0));
        return false;
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
    private WebConfig loadWebConfig(ServletContext servletContext, ObjectStore os) {
        WebConfig retval = null;
        InputStream xmlInputStream = servletContext
            .getResourceAsStream("/WEB-INF/webconfig-model.xml");
        InputStream xmlInputStreamForValidation = servletContext
        .getResourceAsStream("/WEB-INF/webconfig-model.xml");
        if (xmlInputStream == null) {
            LOG.error("Unable to find /WEB-INF/webconfig-model.xml.");
            blockingErrorKeys.put("errors.init.webconfig.notfound", null);
        } else {
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(xmlInputStreamForValidation, writer);
            } catch (IOException ioe) {
                LOG.error("Problems converting xmlInputStream into a String ", ioe);
                blockingErrorKeys.put("errors.init.webconfig.generic", ioe.getMessage());
            }
            String xml = writer.toString();
            String xmlSchemaUrl = "";
            try {
                xmlSchemaUrl = servletContext.getResource("/WEB-INF/webconfig-model.xsd")
                                             .toString();
            } catch (MalformedURLException mue) {
                LOG.warn("Problems retrieving url fo aspects.xsd ", mue);
            }
            if (validateXML(xml, xmlSchemaUrl, "errors.init.webconfig.validation")) {
                try {
                    retval = WebConfig.parse(servletContext, os.getModel());
                    String validationMessage = retval.validateWidgetsConfig(os.getModel());
                    if ( validationMessage.isEmpty()) {
                        SessionMethods.setWebConfig(servletContext, retval);
                    } else {
                        blockingErrorKeys.put("errors.init.webconfig.validation", validationMessage);
                    }
                } catch (FileNotFoundException fnf) {
                    LOG.error("Problem to find the webconfig-model.xml file.", fnf);
                    blockingErrorKeys.put("errors.init.webconfig.notfound", null);
                } catch (ClassNotFoundException cnf) {
                    LOG.error("Classes mentioned in the webconfig-model.xml"
                            + " file aren't in the Model", cnf);
                    blockingErrorKeys.put("errors.init.webconfig.classnotfound", cnf.getMessage());
                } catch (Exception e) {
                    LOG.error("Problem to parse the webconfig-model.xml file", e);
                    blockingErrorKeys.put("errors.init.webconfig.parsing", e.getMessage());
                }
            }
        }
        return retval;
    }

    /**
     *  Load user-friendly class description
     */
    private void loadClassDescriptions(ServletContext servletContext) {
        Properties classDescriptions = new Properties();
        try {
            classDescriptions.load(servletContext
                    .getResourceAsStream("/WEB-INF/classDescriptions.properties"));
        } catch (Exception e) {
            LOG.error("Error loading class descriptions", e);
            blockingErrorKeys.put("errors.init.classDescriptions", null);
        }
        servletContext.setAttribute("classDescriptions", classDescriptions);
    }

    /**
     * Load keys that describe how objects should be uniquely identified
     */
    private Map<String, List<FieldDescriptor>> loadClassKeys(Model model) {
        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(InitialiserPlugin.class.getClassLoader()
                    .getResourceAsStream("class_keys.properties"));
        } catch (Exception e) {
            LOG.error("Error loading class descriptions", e);
            blockingErrorKeys.put("errors.init.classkeys", null);
        }
        Map<String, List<FieldDescriptor>>  classKeys =
            ClassKeyHelper.readKeys(model, classKeyProps);
        return classKeys;
    }

    /**
     * Load keys that describe how objects should be uniquely identified
     */
    private BagQueryConfig loadBagQueries(ServletContext servletContext, ObjectStore os,
        Properties webProperties) {
        BagQueryConfig bagQueryConfig = null;
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/bag-queries.xml");
        if (is != null) {
            try {
                bagQueryConfig = BagQueryHelper.readBagQueryConfig(os.getModel(), is);
            } catch (Exception e) {
                Log.error("Error loading class bag queries. ", e);
                blockingErrorKeys.put("errors.init.bagqueries", e.getMessage());
            }
            InputStream isBag = getClass().getClassLoader()
                .getResourceAsStream("extraBag.properties");
            Properties bagProperties = new Properties();
            if (isBag != null) {
                try {
                    bagProperties.load(isBag);
                    bagQueryConfig.setConnectField(bagProperties
                        .getProperty("extraBag.connectField"));
                    bagQueryConfig.setExtraConstraintClassName(bagProperties
                        .getProperty("extraBag.className"));
                    bagQueryConfig.setConstrainField(bagProperties
                        .getProperty("extraBag.constrainField"));
                } catch (IOException e) {
                    Log.error("Error loading extraBag.properties. ", e);
                    blockingErrorKeys.put("errors.init.extrabagloading", null);
                }
            } else {
                LOG.error("Could not find extraBag.properties file");
                blockingErrorKeys.put("errors.init.extrabag", null);
            }
        } else {
            // can used defaults so just log a warning
            LOG.warn("No custom bag queries found - using default query");
        }
        return bagQueryConfig;
    }

    /**
     * Update the origins and lastState maps if there are any new properties in the
     * current state, or if any of the properties we know about has a new value.
     *
     * @param lastState The way things looked last time we were here.
     * @param origins The places things come from.
     * @param currentSource What to record if a property has just appeared or changed.
     * @param currentState The way things look now.
     */
    private void updateOrigins(
            Map<String, String> lastState,
            Map<String, List<String>> origins,
            String currentSource,
            Properties currentState ) {
        for (Entry<Object, Object> pair: currentState.entrySet()) {
            if (!origins.containsKey(pair.getKey())) {
                origins.put(String.valueOf(pair.getKey()), new ArrayList<String>());
            }
            if (!lastState.containsKey(pair.getKey())
                    || !lastState.get(pair.getKey()).equals(((String) pair.getValue()).trim())) {
                origins.get(pair.getKey()).add(currentSource);
            }
            lastState.put((String) pair.getKey(), ((String) pair.getValue()).trim());
        }
    }

    /**
     * Read in the webapp configuration properties
     */
    private Properties loadWebProperties(ServletContext servletContext) {
        Map<String, String> lastState = new HashMap<String, String>();
        Map<String, List<String>> origins = new TreeMap<String, List<String>>();
        Properties webProperties = new Properties();
        InputStream globalPropertiesStream =
            servletContext.getResourceAsStream("/WEB-INF/global.web.properties");
        try {
            webProperties.load(globalPropertiesStream);
        } catch (Exception e) {
            LOG.error("Unable to find global.web.properties", e);
            blockingErrorKeys.put("errors.init.globalweb", null);
            return webProperties;
        }
        updateOrigins(lastState, origins, "/WEB-INF/global.web.properties", webProperties);

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
                LOG.error("Unable to load " + resource, e);
                blockingErrorKeys.put("errors.init.globalweb", null);
                return webProperties;
            }
            updateOrigins(lastState, origins, resource, webProperties);
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
                LOG.error("Unable to load web.properties", e);
                blockingErrorKeys.put("errors.init.webproperties", null);
                return webProperties;
            }
            updateOrigins(lastState, origins, "/WEB-INF/web.properties", webProperties);
        }
        SessionMethods.setPropertiesOrigins(servletContext, origins);
        Properties trimProperties = trimProperties(webProperties);
        SessionMethods.setWebProperties(servletContext, trimProperties);
        return trimProperties;
    }

    private Properties trimProperties(Properties webProperties) {
        Properties trimProperties = new Properties();
        for (Entry<Object, Object> property: webProperties.entrySet()) {
            trimProperties.put(property.getKey(), ((String) property.getValue()).trim());
        }
        return trimProperties;
    }

    private void loadOpenIDProviders(ServletContext context) {
        Set<String> providers = new HashSet<String>();
        Properties providerProps = new Properties();

        InputStream is = getClass().getClassLoader()
            .getResourceAsStream("openid-providers.properties");
        if (is == null) {
            LOG.info("couldn't find openid providers, using system class-loader");
            is = ClassLoader.getSystemClassLoader()
                .getResourceAsStream("openid-properties.properties");
        }
        if (is != null) {
            try {
                providerProps.load(is);
            } catch (IOException e) {
                LOG.error("Could not load openid-providers.properties", e);
                blockingErrorKeys.put("errors.init.openidprovidersloading", null);
                return;
            }
        } else {
            LOG.error("Could not find openid-providers.properties");
            blockingErrorKeys.put("errors.init.openidproviders", null);
            return;
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
    private ObjectStoreSummary summariseObjectStore(ServletContext servletContext) {
        Properties objectStoreSummaryProperties = new Properties();
        InputStream objectStoreSummaryPropertiesStream =
            servletContext.getResourceAsStream("/WEB-INF/objectstoresummary.properties");
        if (objectStoreSummaryPropertiesStream == null) {
            // there are no model specific properties
            LOG.error("Unable to find objectstoresummary.properties");
            blockingErrorKeys.put("errors.init.objectstoresummary", null);
        }
        try {
            objectStoreSummaryProperties.load(objectStoreSummaryPropertiesStream);
        } catch (Exception e) {
            LOG.error("Unable to read objectstoresummary.properties", e);
            blockingErrorKeys.put("errors.init.objectstoresummary.loading", null);
        }

        final ObjectStoreSummary oss = new ObjectStoreSummary(objectStoreSummaryProperties);
        return oss;
    }

    private void setupClassSummaryInformation(ServletContext servletContext, ObjectStoreSummary oss,
            final Model model) {
        Map<String, String> classes = new LinkedHashMap<String, String>();
        Map<String, Integer> classCounts = new LinkedHashMap<String, Integer>();

        for (String className : new TreeSet<String>(model.getClassNames())) {
            if (!className.equals(InterMineObject.class.getName())) {
                classes.put(className, TypeUtil.unqualifiedName(className));
            }
            try {
                classCounts.put(className, new Integer(oss.getClassCount(className)));
            } catch (Exception e) {
                LOG.error("Unable to get class count for " + className, e);
                blockingErrorKeys.put("errors.init.objectstoresummary.classcount", e.getMessage());
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
    }

    private ObjectStoreWriter getUserprofileWriter(Properties webProperties) {
        ObjectStoreWriter userprofileOSW = null;
        try {
            String userProfileAlias = (String) webProperties.get("webapp.userprofile.os.alias");
            userprofileOSW = ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
        } catch (ObjectStoreException e) {
            LOG.error("Unable to create userprofile - " + e.getMessage(), e);
            blockingErrorKeys.put("errors.init.userprofileconnection", e.getMessage());
            return userprofileOSW;
        }

        applyUserProfileUpgrades(userprofileOSW, blockingErrorKeys);
        return userprofileOSW;
    }

    private void applyUserProfileUpgrades(ObjectStoreWriter osw,
                                          Map<String, String> blockingErrorKeys) {
        Connection con = null;
        boolean setSuperUser = false;
        try {
            con = ((ObjectStoreInterMineImpl) osw).getConnection();
            DatabaseUtil.addColumn(con, "userprofile", "apikey", DatabaseUtil.Type.text);
            if (!DatabaseUtil.columnExists(con, "userprofile", "localaccount")) {
                DatabaseUtil.addColumn(con, "userprofile", "localaccount",
                        DatabaseUtil.Type.boolean_type);
                DatabaseUtil.updateColumnValue(con, "userprofile", "localaccount", true);
            }
            if (!DatabaseUtil.columnExists(con, "userprofile", "superuser")) {
                DatabaseUtil.addColumn(con, "userprofile", "superuser",
                        DatabaseUtil.Type.boolean_type);
                DatabaseUtil.updateColumnValue(con, "userprofile", "superuser", false);
                setSuperUser = true;
            }
        } catch (SQLException sqle) {
            LOG.error("Problem retrieving connection", sqle);
            blockingErrorKeys.put("errors.init.userprofileconnection", sqle.getMessage());
        } finally {
            ((ObjectStoreInterMineImpl) osw).releaseConnection(con);
        }
        if (setSuperUser) {
            setSuperUser(osw);
        }
    }

    private void setSuperUser(ObjectStoreWriter uosw) {
        String superuser = PropertiesUtil.getProperties().getProperty("superuser.account");
        UserProfile superuserProfile = new UserProfile();
        superuserProfile.setUsername(superuser);
        Set<String> fieldNames = new HashSet<String>();
        fieldNames.add("username");
        try {
            superuserProfile = (UserProfile) uosw.getObjectByExample(superuserProfile, fieldNames);
            superuserProfile.setSuperuser(true);
            uosw.store(superuserProfile);
        } catch (ObjectStoreException e) {
            throw new RuntimeException("Unable to load user profile", e);
        }
    }

    /**
     * Destroy method called at Servlet destroy. Close connection pool
     */
    public void destroy() {
        if (profileManager != null) {
            ((ObjectStoreWriterInterMineImpl) profileManager.getProfileObjectStoreWriter())
                .getDatabase().shutdown();
        }
        ((ObjectStoreInterMineImpl) os).getDatabase().shutdown();
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
            blockingErrorKeys.put("errors.init.tracktable.runAnt", null);
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
                        && "timestamp".equals(res.getString(4))
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

    private boolean verifyTablesExist(ObjectStore uos) {
        Connection con = null;
        Set<ClassDescriptor> classDescritpors = uos.getModel().getClassDescriptors();
        try {
            con = ((ObjectStoreInterMineImpl) uos).getConnection();
            for (ClassDescriptor cd : classDescritpors) {
                if (!cd.isInterface()) {
                    String tableNameToVerify = cd.getSimpleName().toLowerCase();
                    if (!DatabaseUtil.tableExists(con, tableNameToVerify)) {
                        LOG.error("In the userprofile database, the table " + tableNameToVerify
                            + " doesn't exist.");
                        blockingErrorKeys.put("errors.init.tablesNotExisting", tableNameToVerify);
                        return false;
                    }
                }
            }
        } catch (SQLException sqle) {
            LOG.error("Probelm retrieving connection", sqle);
        } finally {
            ((ObjectStoreInterMineImpl) uos).releaseConnection(con);
        }
        return true;
    }

    private boolean verifyListTables(ObjectStore uos) {
        Connection con = null;
        try {
            con = ((ObjectStoreInterMineImpl) uos).getConnection();
            if (!DatabaseUtil.tableExists(con, "bagvalues")) {
                blockingErrorKeys.put("errors.init.savedbagtable.runLoadBagValuesTableAnt", null);
                return false;
            } else {
                if (!DatabaseUtil.columnExists(con, "bagvalues", "extra")
                    || DatabaseUtil.columnExists(con, "savedbag", "intermine_current")) {
                    blockingErrorKeys.put("errors.init.savedbagtable.runListTablesAnt", null);
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
                                     "savedbag", "intermine_state",
                                     BagState.NOT_CURRENT.toString());
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
