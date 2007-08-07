package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

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

import org.intermine.cache.InterMineCache;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.aspects.AspectBinding;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryHelper;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.MainHelper;
import org.intermine.web.logic.results.DisplayObject;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.logic.tagging.TagNames;
import org.intermine.web.logic.tagging.TagTypes;
import org.intermine.web.logic.template.TemplateHelper;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ModuleConfig;

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
    public void init(ActionServlet servlet, ModuleConfig config) throws ServletException {
        final ServletContext servletContext = servlet.getServletContext();
 
        System.setProperty("java.awt.headless", "true");
        
        loadWebProperties(servletContext);
        
        ObjectStore os = null;
        Properties props = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String osAlias = (String) props.get("webapp.os.alias");
        try {
            os = ObjectStoreFactory.getObjectStore(osAlias);
        } catch (Exception e) {
            e.getCause().printStackTrace();
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
        // Loading shared template queries requires profile manager
        loadSuperUserDetails(servletContext);

        // index global webSearchables
        SearchRepository searchRepository = new SearchRepository(TemplateHelper.GLOBAL_TEMPLATE);
        servletContext.setAttribute(Constants.GLOBAL_SEARCH_REPOSITORY, searchRepository);
        
        final Profile superProfile = SessionMethods.getSuperUserProfile(servletContext);
        
        AbstractMap<String, WebSearchable> templateSearchableMap =
            new AbstractMap<String, WebSearchable>() {
                @Override
                public Set<Map.Entry<String, WebSearchable>> entrySet() {
                    return pm.filterByTags(superProfile.getSavedTemplates(), PUBLIC_TAG_LIST,
                                           TagTypes.TEMPLATE,
                                           superProfile.getUsername()).entrySet();
                }
            };
        searchRepository.addWebSearchables(TagTypes.TEMPLATE, templateSearchableMap);
        
        AbstractMap<String, WebSearchable> bagSearchableMap =
            new AbstractMap<String, WebSearchable>() {
                @Override
                public Set<Map.Entry<String, WebSearchable>> entrySet() {
                    return pm.filterByTags(superProfile.getSavedBags(), PUBLIC_TAG_LIST,
                                           TagTypes.BAG,
                                           superProfile.getUsername()).entrySet();
                }
            };
        searchRepository.addWebSearchables(TagTypes.BAG, bagSearchableMap);
 
        searchRepository.setProfile(superProfile);
        
        servletContext.setAttribute(Constants.GRAPH_CACHE, new HashMap());

        makeCache(servletContext, os);
        
        cleanTags(pm);
    }

    /**
     * Load the Aspects configuration from aspects.xml
     * @param servletContext the servlet cnotext
     * @param os the main objectstore
     */
    private void loadAspectsConfig(ServletContext servletContext, ObjectStore os) {
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
    private void loadClassDescriptions(ServletContext servletContext, ObjectStore os)
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
            classKeyProps.load(servletContext
                    .getResourceAsStream("/WEB-INF/class_keys.properties"));
        } catch (Exception e) {
            throw new ServletException("Error loading class descriptions", e);
        }
        Map classKeys = ClassKeyHelper.readKeys(os.getModel(), classKeyProps);
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
        String archived = webProperties.getProperty("project.standalone");
        if (StringUtils.isEmpty(archived)) {
            throw new ServletException("project.standalone not defined, please define in build "
                    + "properties as true or false");
        }
        servletContext.setAttribute(Constants.ARCHIVED, Boolean.valueOf(archived));
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
        } else {
            try {
                objectStoreSummaryProperties.load(objectStoreSummaryPropertiesStream);
            } catch (Exception e) {
                throw new ServletException("Unable to read objectstoresummary.properties", e);
            }
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
     * Load superuser account name into servlet context attribute SUPERUSER_ACCOUNT
     *
     * @param servetContext  servlet context in which to place attribute
     */
    private void loadSuperUserDetails(ServletContext servletContext) {
        Properties props = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String superuser = (String) props.get("superuser.account");
        servletContext.setAttribute(Constants.SUPERUSER_ACCOUNT, superuser);
    }

    /**
     * Load the Aspects configuration from aspects.xml
     * @param servletContext the servlet cnotext
     * @param os the main objectstore
     */
    private void makeCache(ServletContext servletContext, ObjectStore os) {
        InterMineCache cache = new InterMineCache();
        TemplateHelper.registerTemplateTableCreator(cache, servletContext);
        servletContext.setAttribute(Constants.GLOBAL_CACHE, cache);
    }
    /**
     * Load the CATEGORY_CLASSES and CATEGORIES servlet context attribute. Loads cateogires and
     * subcateogires from roperties file /WEB-INF/classCategories.properties<p>
     *
     * The properties file should look something like:
     * <pre>
     *   category.0.name = People
     *   category.0.classes = Employee Manager CEO Contractor Secretary
     *   category.1.name = Entities
     *   category.1.classes = Bank Address Department
     * </pre>
     *
     * If a specified class cannot be found in the model, the class is ignored and not added to
     * the category.
     *
     * @param servletContext  the servlet context
     * @param os              the main object store
     *
    private void loadClassCategories(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        List categories = new ArrayList();
        Map subcategories = new HashMap();
        InputStream in = servletContext.getResourceAsStream("/WEB-INF/classCategories.properties");
        if (in == null) {
            return;
        }
        Properties properties = new Properties();
        
        try {
            properties.load(in);
        } catch (IOException err) {
            throw new ServletException(err);
        }
        
        int n = 0;
        String catname;
        
        while ((catname = properties.getProperty("category." + n + ".name")) != null) {
            String sc = properties.getProperty("category." + n + ".classes");
            String subcats[] = StringUtils.split(sc, ' ');
            List subcatlist = new ArrayList();
            
            subcats = StringUtils.stripAll(subcats);
            
            for (int i = 0; subcats != null && i < subcats.length; i++) {
                String className = os.getModel().getPackageName() + "." + subcats[i];
                if (os.getModel().hasClassDescriptor(className)) {
                    subcatlist.add(subcats[i]);
                } else {
                    LOG.warn("Category \"" + catname + "\" contains unknown class \"" + subcats[i]
                        + "\"");
                }
            }
            categories.add(catname);
            subcategories.put(catname, subcatlist);
            n++;
        }
        
        //servletContext.setAttribute(Constants.CATEGORIES, categories);
        //servletContext.setAttribute(Constants.CATEGORY_CLASSES, subcategories);
    }*/

    /**
     * Create the profile manager and place it into to the servlet context.
     */
    private ProfileManager createProfileManager(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        try {
            Properties props = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
            String userProfileAlias = (String) props.get("webapp.userprofile.os.alias");
            Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
            ObjectStoreWriter userProfileOS =
                ObjectStoreWriterFactory.getObjectStoreWriter(userProfileAlias);
            profileManager = new ProfileManager(os, userProfileOS, servletContext);
        } catch (ObjectStoreException e) {
            throw new ServletException("Unable to create profile manager - please check that the "
                    + "userprofile database is available", e);
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
     * @param pm the ProfileManager to alter
     */
    protected static void cleanTags(ProfileManager pm) {
        List classTags = pm.getTags(null, null, "class", null);
        List tagsToDelete = new ArrayList();
        
        Iterator iter = classTags.iterator();
        while (iter.hasNext()) {
            Tag tag = (Tag) iter.next();
            // check that class exists
            try {
                Class.forName(tag.getObjectIdentifier());
            } catch (ClassNotFoundException e) {
                tagsToDelete.add(tag);
            }
        }
        
        iter = tagsToDelete.iterator();
        while (iter.hasNext()) {
            Tag tag = (Tag) iter.next();
            pm.deleteTag(tag);
        }
    }
    
    /**
     * Get the names of the type of this ClassDescriptor and all its descendents
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
