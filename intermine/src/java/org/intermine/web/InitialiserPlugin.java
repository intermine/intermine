package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.ServletException;
import javax.servlet.ServletContext;

import java.util.Properties;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;

import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.PlugIn;
import org.apache.struts.config.ModuleConfig;

import org.intermine.util.TypeUtil;
import org.intermine.metadata.Model;
import org.intermine.web.config.WebConfig;
import org.intermine.objectstore.ObjectStore;
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
        
        loadClassDescriptions(servletContext);
        loadWebProperties(servletContext);
        readExampleQueries(servletContext);
        loadWebConfig(servletContext);

        ObjectStore os = null;
        try {
            os = ObjectStoreFactory.getObjectStore();
        } catch (Exception e) {
            throw new ServletException("Unable to instantiate ObjectStore", e);
        }

        servletContext.setAttribute(Constants.OBJECTSTORE, os); 
        summarizeObjectStore(servletContext, os);   
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
            servletContext.setAttribute("webconfig", WebConfig.parse(is));
        } catch (Exception e) {
            throw new ServletException("Unable to parse webconfig-model.xml", e);
        }
    }

    /**
     * Load the user-friendly class descriptions
     */
    private void loadClassDescriptions(ServletContext servletContext) throws ServletException {
        InputStream is =
            servletContext.getResourceAsStream("/WEB-INF/classDescriptions.properties");
        if (is == null) {
            return;
        }
        Properties classDescriptions = new Properties();
        try {
            classDescriptions.load(is);
        } catch (IOException e) {
            throw new ServletException("Error loading classDescriptions.properties", e);
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
        } catch (IOException e) {
            throw new ServletException("Unable to find global.web.properties", e);
        }
        InputStream modelPropertiesStream =
            servletContext.getResourceAsStream("/WEB-INF/web.properties");
        try {
            webProperties.load(modelPropertiesStream);
        } catch (IOException e) {
            throw new ServletException("Unable to find web.properties", e);
        }
        servletContext.setAttribute(Constants.WEB_PROPERTIES, webProperties);
    }

    /**
     * Summarize the ObjectStore to get class counts
     */
    private void summarizeObjectStore(ServletContext servletContext, ObjectStore os)
        throws ServletException {
        ObjectStoreSummary oss = new ObjectStoreSummary(os);
        Model model = os.getModel();
        Map classes = new LinkedHashMap();
        Map classCounts = new LinkedHashMap();
        for (Iterator i = new TreeSet(model.getClassNames()).iterator(); i.hasNext();) {
            String className = (String) i.next();
            classes.put(className, TypeUtil.unqualifiedName(className));
            try {
                classCounts.put(className, new Integer(oss.getClassCount(className)));
            } catch (Exception e) {
                throw new ServletException("Unable to get class count for " + className, e);
            }
        }
        servletContext.setAttribute("classes", classes);
        servletContext.setAttribute("classCounts", classCounts);
    }

    /**
     * Read the example queries into the EXAMPLE_QUERIES servlet context attribute.
     */
    private void readExampleQueries(ServletContext servletContext) throws ServletException {
        InputStream exampleQueriesStream =
            servletContext.getResourceAsStream("/WEB-INF/example-queries.xml");

        Reader exampleQueriesReader = new InputStreamReader(exampleQueriesStream);
        SavedQueryParser savedQueryParser = new SavedQueryParser();
        Map exampleQueries = null;
        try {
            exampleQueries = savedQueryParser.process(exampleQueriesReader);
        } catch (Exception e) {
            throw new ServletException("Unable to parse example-queries.xml", e);
        }
        servletContext.setAttribute(Constants.EXAMPLE_QUERIES, exampleQueries);
    }

    /**
     * Destroy method called at Servlet destroy
     */
    public void destroy() {
    }
}
