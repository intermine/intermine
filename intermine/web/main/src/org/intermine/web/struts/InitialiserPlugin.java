package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ModuleConfig;
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
import org.intermine.util.TypeUtil;
import org.intermine.web.autocompletion.AutoCompleter;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.aspects.AspectBinding;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryHelper;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.profile.TagManager;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.search.SearchFilterEngine;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagNames;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateHelper;
import org.intermine.web.logic.template.TemplateQuery;

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

    private static final List<String> PUBLIC_TAG_LIST =
        Arrays.asList(new String[] {TagNames.IM_PUBLIC});

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
    public void init(ActionServlet servlet,
                     @SuppressWarnings("unused") ModuleConfig config) throws ServletException {
        try {
            final ServletContext servletContext = servlet.getServletContext();

            System.setProperty("java.awt.headless", "true");

            loadWebProperties(servletContext);

            ObjectStore os = null;
            Properties props = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
            String osAlias = (String) props.get("webapp.os.alias");
            try {
                os = ObjectStoreFactory.getObjectStore(osAlias);
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause != null) {
                    cause.printStackTrace();
                }
                throw new ServletException("Unable to instantiate ObjectStore " + osAlias, e);
            }
            servletContext.setAttribute(Constants.OBJECTSTORE, os);

            loadWebConfig(servletContext, os);
            loadAspectsConfig(servletContext, os);

            //loadClassCategories(servletContext, os);
            loadClassDescriptions(servletContext, os);

            summarizeObjectStore(servletContext, os);

            // load class keys
            loadClassKeys(servletContext, os);

            // load custom bag queries
            loadBagQueries(servletContext, os);

            final ProfileManager pm = createProfileManager(servletContext, os);

            // index global webSearchables
            SearchRepository searchRepository =
                new SearchRepository(TemplateHelper.GLOBAL_TEMPLATE);
            servletContext.setAttribute(Constants.GLOBAL_SEARCH_REPOSITORY, searchRepository);

            final Profile superProfile = SessionMethods.getSuperUserProfile(servletContext);

            final TagManager tagManager = SessionMethods.getTagManager(servletContext);
            AbstractMap<String, TemplateQuery> templateSearchableMap =
                new AbstractMap<String, TemplateQuery>() {
                    @Override
                    public Set<Map.Entry<String, TemplateQuery>> entrySet() {
                        return new SearchFilterEngine()
                        .filterByTags(superProfile.getSavedTemplates(),
                                PUBLIC_TAG_LIST, TagTypes.TEMPLATE, superProfile.getUsername(),
                                tagManager).entrySet();
                    }
                };
            searchRepository.addWebSearchables(TagTypes.TEMPLATE, templateSearchableMap);

            AbstractMap<String, InterMineBag> bagSearchableMap =
                new AbstractMap<String, InterMineBag>() {
                    @Override
                    public Set<Map.Entry<String, InterMineBag>> entrySet() {
                        return new SearchFilterEngine().filterByTags(superProfile.getSavedBags(),
                                PUBLIC_TAG_LIST, TagTypes.BAG, superProfile.getUsername(),
                                tagManager).entrySet();
                    }
                };
            searchRepository.addWebSearchables(TagTypes.BAG, bagSearchableMap);

            searchRepository.setProfile(superProfile);

            servletContext.setAttribute(Constants.GRAPH_CACHE, new HashMap());

            loadAutoCompleter(servletContext, os);

            cleanTags(SessionMethods.getTagManager(servletContext));
        } catch (ServletException e) {
            LOG.error("ServletException", e);
            destroy();
            throw e;
        } catch (RuntimeException e) {
            destroy();
            LOG.error("RuntimeException", e);
            throw e;
        }
    }

    /**
     * Load the Aspects configuration from aspects.xml
     * @param servletContext the servlet context
     * @param os the main objectstore
     */
    private void loadAspectsConfig(ServletContext servletContext,
                                   @SuppressWarnings("unused") ObjectStore os) {
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/aspects.xml");
        if (is == null) {
            LOG.info("Unable to find /WEB-INF/aspects.xml, there will be no aspects");
            servletContext.setAttribute(Constants.ASPECTS, Collections.EMPTY_MAP);
            servletContext.setAttribute(Constants.CATEGORIES, Collections.EMPTY_SET);
        } else {
            Map sets;
            try {
                sets = AspectBinding.unmarhsal(is);
            } catch (Exception e) {
                throw new RuntimeException("problem while reading aspect configuration file", e);
            }
            servletContext.setAttribute(Constants.ASPECTS, sets);
            servletContext.setAttribute(Constants.CATEGORIES,
                    Collections.unmodifiableSet(sets.keySet()));
        }
    }

    private void loadAutoCompleter(ServletContext servletContext, ObjectStore os)
            throws ServletException {

            if (os instanceof ObjectStoreInterMineImpl) {
                Database db = ((ObjectStoreInterMineImpl) os).getDatabase();
                try {
                    InputStream is = MetadataManager.retrieveBLOBInputStream(db,
                            MetadataManager.AUTOCOMPLETE_INDEX);
                    AutoCompleter ac;

                    if (is != null) {
                        ac = new AutoCompleter(is);
                        servletContext.setAttribute(Constants.AUTO_COMPLETER, ac);
                    } else {
                        ac = null;
                    }
                } catch (SQLException e) {
                    LOG.error("Problem with database", e);
                    throw new ServletException("Problem with database", e);
                }
            }
    }

    /**
     * Load the displayer configuration
     */
    private void loadWebConfig(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/webconfig-model.xml");
        if (is == null) {
            throw new ServletException("Unable to find webconfig-model.xml");
        }
        try {
            servletContext.setAttribute(Constants.WEBCONFIG,
                                        WebConfig.parse(is, os.getModel()));
        } catch (Exception e) {
            throw new ServletException("Unable to parse webconfig-model.xml", e);
        }
    }

    /**
     * Load the user-friendly class descriptions
     */
    private void loadClassDescriptions(ServletContext servletContext,
                                       @SuppressWarnings("unused") ObjectStore os)
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
    private void loadClassKeys(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        Properties classKeyProps = new Properties();
        try {
            classKeyProps.load(InitialiserPlugin.class.getClassLoader()
                    .getResourceAsStream("class_keys.properties"));
        } catch (Exception e) {
            throw new ServletException("Error loading class descriptions", e);
        }
        Map<String, List<FieldDescriptor>>  classKeys =
            ClassKeyHelper.readKeys(os.getModel(), classKeyProps);
        servletContext.setAttribute(Constants.CLASS_KEYS, classKeys);
    }

    /**
     * Load keys that describe how objects should be uniquely identified
     */
    private void loadBagQueries(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/bag-queries.xml");
        if (is != null) {
            try {
                BagQueryConfig bagQueryConfig = BagQueryHelper
                    .readBagQueryConfig(os.getModel(), is);
                servletContext.setAttribute(Constants.BAG_QUERY_CONFIG, bagQueryConfig);
            } catch (Exception e) {
                throw new ServletException("Error loading class bag queries", e);
            }
        } else {
            // can used defaults so just log a warning
            LOG.warn("No custom bag queries found - using default query");
        }
    }

    /**
     * Read the example queries into the EXAMPLE_QUERIES servlet context attribute.
     */
    private void loadWebProperties(ServletContext servletContext) throws ServletException {
        Properties webProperties = new Properties();
        InputStream globalPropertiesStream =
            servletContext.getResourceAsStream("/WEB-INF/global.web.properties");
        try {
            webProperties.load(globalPropertiesStream);
        } catch (Exception e) {
            throw new ServletException("Unable to find global.web.properties", e);
        }
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
        servletContext.setAttribute(Constants.WEB_PROPERTIES, webProperties);
    }

    /**
     * Summarize the ObjectStore to get class counts
     */
    private void summarizeObjectStore(ServletContext servletContext, final ObjectStore os)
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
        Model model = os.getModel();
        Map classes = new LinkedHashMap();
        Map classCounts = new LinkedHashMap();
        for (Iterator i = new TreeSet(model.getClassNames()).iterator(); i.hasNext();) {
            String className = (String) i.next();
            if (!className.equals(InterMineObject.class.getName())) {
                classes.put(className, TypeUtil.unqualifiedName(className));
            }
            try {
                classCounts.put(className, new Integer(oss.getClassCount(className)));
            } catch (Exception e) {
                throw new ServletException("Unable to get class count for " + className, e);
            }
        }
        servletContext.setAttribute(Constants.OBJECT_STORE_SUMMARY, oss);
        servletContext.setAttribute("classes", classes);
        servletContext.setAttribute("classCounts", classCounts);
        // Build subclass lists for JSPs
        Map subclassesMap = new LinkedHashMap();
        for (Iterator i = new TreeSet(model.getClassNames()).iterator(); i.hasNext();) {
            String className = TypeUtil.unqualifiedName((String) i.next());
            ClassDescriptor cld;
            try {
                cld = MainHelper.getClassDescriptor(className, model);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("unexpected exception", e);
            }
            ArrayList subclasses = new ArrayList();
            Iterator iter = new TreeSet(getChildren(cld)).iterator();
            while (iter.hasNext()) {
                String thisClassName = (String) iter.next();
                if (((Integer) classCounts.get(thisClassName)).intValue() > 0) {
                    subclasses.add(TypeUtil.unqualifiedName(thisClassName));
                }
            }
            subclassesMap.put(className, subclasses);
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
        // Build map interface that takes an object and returns set of leaf class descriptors
        Map leafDescriptorsMap = new AbstractMap() {
            public Set entrySet() {
                return null;
            }
            public Object get(Object key) {
                if (key == null) {
                    return Collections.EMPTY_SET;
                }
                return DisplayObject.getLeafClds(key.getClass(), os.getModel());
            }
        };
        servletContext.setAttribute(Constants.LEAF_DESCRIPTORS_MAP, leafDescriptorsMap);
    }

    
    /**
     * Create the profile manager and place it into to the servlet context.
     */
    private ProfileManager createProfileManager(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        if (profileManager == null) {
            try {
                Properties props =
                    (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
                String userProfileAlias = (String) props.get("webapp.userprofile.os.alias");
                ObjectStoreWriter userProfileOS =
                    ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
                profileManager = new ProfileManager(os, userProfileOS);
            } catch (ObjectStoreException e) {
                LOG.error("Unable to create profile manager - please check that the "
                        + "userprofile database is available", e);
                throw new ServletException("Unable to create profile manager - please check that "
                        + "the userprofile database is available", e);
            }
        }
        servletContext.setAttribute(Constants.PROFILE_MANAGER, profileManager);
        return profileManager;
    }

    /**
     * Destroy method called at Servlet destroy
     */
    public void destroy() {
        try {
            profileManager.close();
        } catch (ObjectStoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Remove class tags from the user profile that refer to classes that non longer exist
     * @param tagManager the ProfileManager to alter
     */
    /**
     * Remove class tags from the user profile that refer to classes that non longer exist
     * @param tagManager the tag manager
     */
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
    protected static Set getChildren(ClassDescriptor cld) {
        Set children = new HashSet();
        getChildren(cld, children);
        return children;
    }

    /**
     * Add the names of the descendents of a ClassDescriptor to a Set
     * @param cld the ClassDescriptor
     * @param children the Set of child names
     */
    protected static void getChildren(ClassDescriptor cld, Set children) {
        for (Iterator i = cld.getSubDescriptors().iterator(); i.hasNext();) {
            ClassDescriptor child = (ClassDescriptor) i.next();
            children.add(child.getName());
            getChildren(child, children);
        }
    }
}
