package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ModuleConfig;

import org.intermine.util.TypeUtil;
import org.intermine.metadata.Model;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetadataManager;
import org.intermine.web.config.WebConfig;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreFactory;

/**
 * Initialiser for the InterMine web application
 * Anything that needs global initialisation goes here.
 *
 * @author Andrew Varley
 */
public class InitialiserPlugin implements PlugIn
{
    private static final Logger LOG = Logger.getLogger(InitialiserPlugin.class);
    
    ProfileManager profileManager;

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
        ServletContext servletContext = servlet.getServletContext();
        
        loadWebProperties(servletContext);
        loadExampleQueries(servletContext);
        loadWebConfig(servletContext);
        
        ObjectStore os = null;
        try {
            os = ObjectStoreFactory.getObjectStore();
        } catch (Exception e) {
            throw new ServletException("Unable to instantiate ObjectStore", e);
        }
        servletContext.setAttribute(Constants.OBJECTSTORE, os); 
        
        loadClassCategories(servletContext, os);
        loadClassDescriptions(servletContext, os);
        
        processWebConfig(servletContext, os);
        summarizeObjectStore(servletContext, os);
        createProfileManager(servletContext, os);
        // Loading shared template queries requires profile manager
        loadSuperUserDetails(servletContext);
        // Load default templates if required
        loadDefaultGlobalTemplateQueries(servletContext);
        loadGlobalTemplateQueries(servletContext);
    }

    /**
     * Load the displayer configuration
     */
    private void loadWebConfig(ServletContext servletContext) throws ServletException {
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/webconfig-model.xml");
        if (is == null) {
            throw new ServletException("Unable to find webconfig-model.xml");
        }
        try {
            servletContext.setAttribute(Constants.WEBCONFIG, WebConfig.parse(is));
        } catch (Exception e) {
            throw new ServletException("Unable to parse webconfig-model.xml", e);
        }
    }

    /**
     * Load the user-friendly class descriptions
     */
    private void loadClassDescriptions(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        Properties classDescriptions = null;
        try {
            classDescriptions = MetadataManager.loadClassDescriptions(os.getModel().getName());
        } catch (Exception e) {
            throw new ServletException("Error loading class descriptions", e);
        }
        servletContext.setAttribute("classDescriptions", classDescriptions);
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
    private void summarizeObjectStore(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        Properties objectStoreSummaryProperties = new Properties();
        InputStream objectStoreSummaryPropertiesStream =
            servletContext.getResourceAsStream("/WEB-INF/objectstoresummary.properties");
        if (objectStoreSummaryPropertiesStream == null) {
            // there are no model specific properties
        } else {
            try {
                objectStoreSummaryProperties.load(objectStoreSummaryPropertiesStream);
            } catch (Exception e) {
                throw new ServletException("Unable to read objectstoresummary.properties", e);
            }
        }
        ObjectStoreSummary oss = new ObjectStoreSummary(objectStoreSummaryProperties);
        Model model = os.getModel();
        Map classes = new LinkedHashMap();
        Map classCounts = new LinkedHashMap();
        for (Iterator i = new TreeSet(model.getClassNames()).iterator(); i.hasNext();) {
            String className = (String) i.next();
            classes.put(className, TypeUtil.unqualifiedName(className));
            try {
                //classCounts.put(className, new Integer(1));
                classCounts.put(className, new Integer(oss.getClassCount(className)));
            } catch (Exception e) {
                throw new ServletException("Unable to get class count for " + className, e);
            }
        }
        servletContext.setAttribute(Constants.OBJECT_STORE_SUMMARY, oss);
        servletContext.setAttribute("classes", classes);
        servletContext.setAttribute("classCounts", classCounts);
    }

    /**
     * Read the example queries into the EXAMPLE_QUERIES servlet context attribute.
     */
    private void loadExampleQueries(ServletContext servletContext) throws ServletException {
        InputStream exampleQueriesStream =
            servletContext.getResourceAsStream("/WEB-INF/example-queries.xml");
        if (exampleQueriesStream == null) {
            return;
        }
        Reader exampleQueriesReader = new InputStreamReader(exampleQueriesStream);
        Map exampleQueries = null;
        try {
            exampleQueries = new PathQueryBinding().unmarshal(exampleQueriesReader);
        } catch (Exception e) {
            throw new ServletException("Unable to parse example-queries.xml", e);
        }
        servletContext.setAttribute(Constants.EXAMPLE_QUERIES, exampleQueries);
    }

    /**
     * Loads the default template queries into the superuser's account.
     *
     * @param servletContext  current servlet context
     */
    private void loadDefaultGlobalTemplateQueries(ServletContext servletContext)
        throws ServletException {
        InputStream templateQueriesStream =
            servletContext.getResourceAsStream("/WEB-INF/template-queries.xml");
        if (templateQueriesStream == null) {
            return;
        }
        Reader templateQueriesReader = new InputStreamReader(templateQueriesStream);
        Map templateQueries = null;
        try {
            templateQueries = new TemplateQueryBinding().unmarshal(templateQueriesReader);
        } catch (Exception e) {
            throw new ServletException("Unable to parse template-queries.xml", e);
        }
        // Add them to superuser profile
        String superuser = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        if (superuser == null) {
            LOG.warn("No superuser account specified");
            return;
        }
        Profile profile = null;
        if (!pm.hasProfile(superuser)) {
            LOG.info("Creating profile for superuser " + superuser);
            profile = new Profile(pm, superuser, new HashMap(), new HashMap(), new HashMap());
            String password = RequestPasswordAction.generatePassword();
            Map webProperties = (Map) servletContext.getAttribute(Constants.WEB_PROPERTIES);
            pm.saveProfile(profile);
            pm.setPassword(superuser, password);
        } else {
            profile = pm.getProfile(superuser, pm.getPassword(superuser));
        }
        if (profile.getSavedTemplates().size() == 0) {
            Iterator iter = templateQueries.values().iterator();
            while (iter.hasNext()) {
                TemplateQuery template = (TemplateQuery) iter.next();
                profile.saveTemplate(template.getName(), template);
            }
        }
    }
    
    /**
     * Load superuser account name into servlet context attribute SUPERUSER_ACCOUNT
     *
     * @param servetContext  servlet context in which to place attribute
     */
    private void loadSuperUserDetails(ServletContext servletContext)
        throws ServletException {
        Properties properties = new Properties();
        InputStream propertiesStream =
            servletContext.getResourceAsStream("/WEB-INF/global.web.properties");
        try {
            properties.load(propertiesStream);
        } catch (Exception e) {
            throw new ServletException("Unable to find model.properties", e);
        }
        
        String superuser = properties.getProperty("superuser.account");
        servletContext.setAttribute(Constants.SUPERUSER_ACCOUNT, superuser);
    }
    
    /**
     * Read the template queries into the GLOBAL_TEMPLATE_QUERIES servlet context attribute and set
     * CATEGORY_TEMPLATES and CLASS_CATEGORY_TEMPLATES. 
     * This is also called when the superuser updates his or her templates.
     *
     * @param servletContext  servlet context in which to place template map
     * @throws ServletException if something goes wrong
     */
    public static void loadGlobalTemplateQueries(ServletContext servletContext)
        throws ServletException {   
        Map templateMap = Collections.synchronizedMap(new HashMap());
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        String superuser = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);
        
        if (superuser != null && pm.hasProfile(superuser)) {
            Profile profile = pm.getProfile(superuser, pm.getPassword(superuser));
            if (profile != null) {
                templateMap = Collections.synchronizedMap(new HashMap(profile.getSavedTemplates()));
            } else {
                LOG.warn("failed to getch profile for superuser " + superuser);
            }
        } else {
            LOG.warn("superuser.account not specified");
        }
        servletContext.setAttribute(Constants.GLOBAL_TEMPLATE_QUERIES, templateMap);
        
        // Sort into categories
        Map categoryTemplates = new HashMap();
        // a Map from class name to a Map from category to template
        Map classCategoryTemplates = new HashMap();
        // a Map from class name to a Map from template name to field name List - the field
        // names/expressions are the ones that should be set when a template is linked to from the
        // object details page eg. Gene.identifier
        Map classTemplateExprs = new HashMap();
        Iterator iter = templateMap.values().iterator();
        while (iter.hasNext()) {
            TemplateQuery template = (TemplateQuery) iter.next();
            List list = (List) categoryTemplates.get(template.getCategory());
            if (list == null) {
                list = new ArrayList();
                categoryTemplates.put(template.getCategory(), list);
            }
            list.add(template);

            Object osObject = servletContext.getAttribute(Constants.OBJECTSTORE);
            ObjectStore os = (ObjectStore) osObject;

            
            setClassesForTemplate(os, template, classCategoryTemplates, classTemplateExprs);
        }
        servletContext.setAttribute(Constants.CATEGORY_TEMPLATES, categoryTemplates);
        servletContext.setAttribute(Constants.CLASS_CATEGORY_TEMPLATES, classCategoryTemplates);
        servletContext.setAttribute(Constants.CLASS_TEMPLATE_EXPRS, classTemplateExprs);
    }
    
    /**
     * Return two Maps with information about the relations between classnames, a given template and
     * its template categories.
     * @param classCategoryTemplates a Map from class name to a Map from category to template
     * @param classTemplateExprs a Map from class name to a Map from template name to field name
     * List - the field names/expressions are the ones that should be set when a template is linked
     * to from the object details page eg. Gene.identifier
     */
    private static void setClassesForTemplate(ObjectStore os, TemplateQuery template,
                                              Map classCategoryTemplates,
                                              Map classTemplateExprs) {
        List constraints = template.getAllConstraints();
        Model model = os.getModel();
        Iterator constraintIter = constraints.iterator();
        
        // look for ClassName.fieldname  (Gene.identifier)
        // or ClassName.fieldname.fieldname.fieldname...  (eg. Gene.organism.name)
        while (constraintIter.hasNext()) {
            Constraint c = (Constraint) constraintIter.next();

            String constraintIdentifier = c.getIdentifier();
            String[] bits = constraintIdentifier.split("\\.");

            if (bits.length == 2) {
                String className = model.getPackageName() + "." + bits[0];
                String fieldName = bits[1];
                String fieldExpr = TypeUtil.unqualifiedName(className) + "." + fieldName;
                ClassDescriptor cd = model.getClassDescriptorByName(className);

                if (cd != null && cd.getFieldDescriptorByName(fieldName) != null) {
                    Set subClasses = model.getAllSubs(cd);

                    Set thisAndSubClasses = new HashSet();
                    thisAndSubClasses.addAll(subClasses);
                    thisAndSubClasses.add(cd);

                    Iterator thisAndSubClassesIterator = thisAndSubClasses.iterator();
                    
                    while (thisAndSubClassesIterator.hasNext()) {
                        ClassDescriptor thisCD = (ClassDescriptor) thisAndSubClassesIterator.next();
                        String thisClassName = thisCD.getName();
                        if (!classCategoryTemplates.containsKey(thisClassName)) {
                            classCategoryTemplates.put(thisClassName, new HashMap());
                        }
                    
                        Map categoryTemplatesMap = (Map) classCategoryTemplates.get(thisClassName);
                    
                        if (!categoryTemplatesMap.containsKey(template.getCategory())) {
                            categoryTemplatesMap.put(template.getCategory(), new ArrayList());
                        }
                    
                        ((List) categoryTemplatesMap.get(template.getCategory())).add(template);
                    
                        if (!classTemplateExprs.containsKey(thisClassName)) {
                            classTemplateExprs.put(thisClassName, new HashMap());
                        }
                    
                        Map fieldNameTemplatesMap = (Map) classTemplateExprs.get(thisClassName);
                    
                        if (!fieldNameTemplatesMap.containsKey(template.getName())) {
                            fieldNameTemplatesMap.put(template.getName(), new ArrayList());
                        }
                    
                        ((List) fieldNameTemplatesMap.get(template.getName())).add(fieldExpr);
                    }
               }
            }
        }
    }

    /**
     * Load the CATEGORY_CLASSES and CATEGORIES servlet context attribute. Loads cateogires and
     * subcateogires from roperties file /WEB-INF/classCategories.properties<p>
     *
     * The properties file should look something like:
     * <pre>
     *   category.0.name = People
     *   category.0.subcategories = Employee Manager CEO Contractor Secretary
     *   category.1.name = Entities
     *   category.1.subcategories = Bank Address Department
     * </pre>
     *
     * If a specified class cannot be found in the model, the class is ignored and not added to
     * the category.
     *
     * @param servletContext  the servlet context
     * @param os              the main object store
     */
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
        
        servletContext.setAttribute(Constants.CATEGORIES, categories);
        servletContext.setAttribute(Constants.CATEGORY_CLASSES, subcategories);
    }

    /**
     * Create the DISPLAYERS ServletContext attribute by looking at the model and the WebConfig.
     *
     * @param servletContext  the servlet context
     * @param os              the main object store
     */
    private void processWebConfig(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        try {
            Model model = os.getModel();
            WebConfig wc = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);

            Map wcTypeMap = (Map) wc.getTypes();
            Map displayersMap = new HashMap();

            for (Iterator modelIter = new TreeSet(model.getClassNames()).iterator();
                 modelIter.hasNext();) {
                String className = (String) modelIter.next();

                Set cds = model.getClassDescriptorsForClass(Class.forName(className));
                List cdList = new ArrayList(cds);

                Collections.reverse(cdList);
            
                for (Iterator cdIter = cdList.iterator(); cdIter.hasNext(); ) {
                    ClassDescriptor cd = (ClassDescriptor) cdIter.next();

                    if (wcTypeMap.get(cd.getName()) != null) {
                        displayersMap.put(className, wcTypeMap.get(cd.getName()));
                    }

                    for (Iterator fdIter = cd.getFieldDescriptors().iterator(); fdIter.hasNext();) {
                        FieldDescriptor fd = (FieldDescriptor) fdIter.next();
                        String newKey = cd.getName() + " " + fd.getName();

                        if (wcTypeMap.get(newKey) != null) {
                            displayersMap.put(className + " " + fd.getName(),
                                              wcTypeMap.get(newKey));
                        }
                    }
                }
            }
            
            servletContext.setAttribute(Constants.DISPLAYERS, displayersMap);
        } catch (ClassNotFoundException e) {
            throw new ServletException("Unable to process webconfig", e);
        }
    }

    /**
     * Create the profile manager and place it into to the servlet context.
     */
    private void createProfileManager(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        try {
            profileManager = new ProfileManager(os);
        } catch (ObjectStoreException e) {
            //throw new ServletException("Unable to create profile manager", e);
        }
        servletContext.setAttribute(Constants.PROFILE_MANAGER, profileManager);
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
}
