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
    public void init(ActionServlet servlet, ModuleConfig config)
        throws ServletException {
        ServletContext servletContext = servlet.getServletContext();

        InputStream is = servletContext.getResourceAsStream("/WEB-INF/webconfig-model.xml");
        if (is == null) {
            throw new ServletException("There is no model in the webapp");
        }

        try {
            servletContext.setAttribute("webconfig", WebConfig.parse(is));
            
            Properties webProperties = new Properties();
            InputStream globalPropertiesStream =
                servletContext.getResourceAsStream("/WEB-INF/global.web.properties");
            webProperties.load(globalPropertiesStream);
            InputStream modelPropertiesStream =
                servletContext.getResourceAsStream("/WEB-INF/web.properties");
            webProperties.load(modelPropertiesStream);
            servletContext.setAttribute(Constants.WEB_PROPERTIES, webProperties);
            
            ObjectStore os = ObjectStoreFactory.getObjectStore();
            servletContext.setAttribute(Constants.OBJECTSTORE, os); 
            
            ObjectStoreSummary oss = new ObjectStoreSummary(os);
            Model model = os.getModel();
            Map classes = new LinkedHashMap();
            Map classCounts = new LinkedHashMap();
            for (Iterator i = new TreeSet(model.getClassNames()).iterator(); i.hasNext();) {
                String className = (String) i.next();
                classes.put(className, TypeUtil.unqualifiedName(className));
                classCounts.put(className, new Integer(oss.getClassCount(className)));
            }
            servletContext.setAttribute("classes", classes);
            servletContext.setAttribute("classCounts", classCounts);
            
            readExampleQueries(servletContext);            
        } catch (Exception e) {
            throw new ServletException("Initialisation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Read the example queries into the EXAMPLE_QUERIES servlet context attribute.
     */
    private void readExampleQueries(ServletContext servletContext) throws Exception {
        InputStream exampleQueriesStream =
            servletContext.getResourceAsStream("/WEB-INF/example-queries.xml");

        Reader exampleQueriesReader = new InputStreamReader(exampleQueriesStream);
        SavedQueryParser savedQueryParser = new SavedQueryParser();
        Map exampleQueries = savedQueryParser.process(exampleQueriesReader);
        
        servletContext.setAttribute(Constants.EXAMPLE_QUERIES, exampleQueries);
    }

    /**
     * Destroy method called at Servlet destroy
     */
    public void destroy() {
    }
}
