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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.tiles.actions.TilesAction;

import org.intermine.objectstore.query.ConstraintOp;

/**
 * Controller Action for begin.jsp
 *
 * @author Thomas Riley
 */

public class BeginController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(BeginController.class);
    
    private List categories;
    private Map subcategories;
    
    /**
     * Populate request with category data etc.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        if (categories == null) {
            loadCategories();
        }
            
        HttpSession session = request.getSession();
        Properties properties = (Properties)
            request.getSession().getServletContext().getAttribute(Constants.WEB_PROPERTIES);
        if (properties != null) {
            session.setAttribute("queryName", properties.getProperty("begin.browse.template"));
            // might want to make the operator a model web.properties property
            request.setAttribute("browseOperator", ConstraintOp.MATCHES.getIndex());
        }
        request.setAttribute("categories", categories);
        request.setAttribute("subcategories", subcategories);
        
        return null;
    }
    
    /**
     * Loads cateogires and subcateogires from properties file
     * /WEB-INF/classCategories.properties<p>
     *
     * The properties file should look something like:
     * <pre>
     *   category.0.name = People
     *   category.0.subcategories = Employee Manager CEO Contractor Secretary
     *   category.1.name = Entities
     *   category.1.subcategories = Bank Address Department
     * </pre>
     */
    private void loadCategories() {
        categories = new ArrayList();
        subcategories = new HashMap();
        InputStream in = getServlet().getServletContext().
                        getResourceAsStream("/WEB-INF/classCategories.properties");
        if (in == null) {
            return;
        }
        Properties properties = new Properties();
        
        try {
            properties.load(in);
        } catch (IOException err) {
            LOG.warn(err);
            return;
        }
        
        int n = 0;
        String catname;
        
        while ((catname = properties.getProperty("category." + n + ".name")) != null) {
            String sc = properties.getProperty("category." + n + ".subcategories");
            String subcats[] = StringUtils.split(sc, ' ');
            subcats = StringUtils.stripAll(subcats);
            
            categories.add(catname);
            subcategories.put(catname, Arrays.asList(subcats));
            
            n++;
        }
    }
}
