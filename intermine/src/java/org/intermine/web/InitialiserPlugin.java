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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ModuleConfig;
import org.intermine.metadata.MetadataManager;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.util.TypeUtil;
import org.intermine.web.config.WebConfig;

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
        
        ObjectStore os = null;
        try {
            os = ObjectStoreFactory.getObjectStore();
        } catch (Exception e) {
            throw new ServletException("Unable to instantiate ObjectStore", e);
        }
        servletContext.setAttribute(Constants.OBJECTSTORE, os); 
        
        loadWebConfig(servletContext, os);

        loadClassCategories(servletContext, os);
        loadClassDescriptions(servletContext, os);
        
        summarizeObjectStore(servletContext, os);
        createProfileManager(servletContext, os);
        // Loading shared template queries requires profile manager
        loadSuperUserDetails(servletContext);
        servletContext.setAttribute(Constants.TEMPLATE_REPOSITORY,
                new TemplateRepository(servletContext));
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
