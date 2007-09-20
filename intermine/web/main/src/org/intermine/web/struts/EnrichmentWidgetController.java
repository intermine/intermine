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

import java.util.ArrayList;
import java.util.Map;

import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.widget.EnrichmentWidgetLdr;

import java.lang.reflect.Constructor;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * calculates p-values of goterms
 * @author Julie Sullivan
 */
public class EnrichmentWidgetController extends TilesAction
{

    /**
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
     public ActionForward execute(@SuppressWarnings("unused") ActionMapping mapping,
                                  @SuppressWarnings("unused") ActionForm form,
                                  @SuppressWarnings("unused") HttpServletRequest request,
                                  @SuppressWarnings("unused") HttpServletResponse response) 
     throws Exception {

        
             HttpSession session = request.getSession();
             Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
             ServletContext servletContext = session.getServletContext();
             ObjectStoreInterMineImpl os =
                 (ObjectStoreInterMineImpl) servletContext.getAttribute(Constants.OBJECTSTORE);

             String bagName = request.getParameter("bagName");
             Map<String, InterMineBag> allBags =
                 WebUtil.getAllBags(profile.getSavedBags(), servletContext);
             InterMineBag bag = allBags.get(bagName);
             
             // TODO get these from request form
             Double maxValue = new Double("0.10");

             
             // TODO get these from properties files
             String namespace = (request.getParameter("ontology") != null
                               ? request.getParameter("ontology") : "biological_process");
             
             // put in request for display on the .jsp page
             request.setAttribute("bagName", bagName);
             request.setAttribute("ontology", namespace);
             
             String title = request.getParameter("title");
             String controller = request.getParameter("controller");
             String description = request.getParameter("description");
             
             Class clazz = TypeUtil.instantiate(controller);
             Constructor constr = clazz.getConstructor(new Class[]
                                                                 {
                 HttpServletRequest.class
                                                                 });

             EnrichmentWidgetLdr ldr = (EnrichmentWidgetLdr) constr.newInstance(new Object[]
                                                                                {
                 request
                                                                                });
             
             // run both queries and compare the results 
             ArrayList results = WebUtil.statsCalc(os, ldr.getPopulation(), ldr.getSample(), bag, 
                                       ldr.getTotal(os), maxValue);

             request.setAttribute("title", title);
             request.setAttribute("description", description);
             if (results.isEmpty()) {
                 return null;
             }
             request.setAttribute("pvalues", results.get(0));
             request.setAttribute("totals", results.get(1));
             request.setAttribute("labelToId", results.get(2));
             request.setAttribute("referencePopulation", "All genes from:  " 
                                  + ldr.getReferencePopulation().toString());
                          
             return null;
     }
}

