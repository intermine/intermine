package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.MetadataCache.GBrowseTrack;


public class ProjectsSummaryController extends TilesAction 
{
    private static final Logger LOG = Logger.getLogger(MetadataCache.class);

    
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        try {
            final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
            ObjectStore os = im.getObjectStore();
            
            Map<String, List<DisplayExperiment>> experiments = 
                MetadataCache.getProjectExperiments(os);
            request.setAttribute("experiments", experiments);
            
            Properties propCat = new Properties(); 
            Properties propOrd = new Properties(); 
            final ServletContext servletContext = servlet.getServletContext();
            
            InputStream is = 
                servletContext.getResourceAsStream("/WEB-INF/experimentCategory.properties");
            if (is == null) {
                LOG.info("Unable to find /WEB-INF/experimentCategory.properties!");
            } else {
                try {
                    propCat.load(is);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            InputStream is2 = 
                servletContext.getResourceAsStream("/WEB-INF/categoryOrder.properties");
            if (is == null) {
                LOG.info("Unable to find /WEB-INF/category.properties!");
            } else {
                try {
                    propOrd.load(is2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            
            Map <String, List<DisplayExperiment>> catExpUnordered =
                new HashMap<String, List<DisplayExperiment>>();
            
            for (List<DisplayExperiment> ll : experiments.values()) {
                for (DisplayExperiment de : ll) {
                    String cats = propCat.getProperty(de.getName());
                    // an experiment can be associated to more than 1 category
                    String[] cat = cats.split("#");
                    for (String c : cat) {
                        List<DisplayExperiment> des = catExpUnordered.get(c);
                        if (des == null) {
                            des = new ArrayList<DisplayExperiment>();
                            catExpUnordered.put(c, des);
                        }
                        des.add(de);
                        //LOG.info("DEXP: " + c + "|" + de.getName());
                    }
                }
            }
            
            Map <String, List<DisplayExperiment>> catExp =
                new LinkedHashMap<String, List<DisplayExperiment>>();
            
            for (Integer i = 1; i <= propOrd.size(); i++) {
                String ordCat = propOrd.getProperty(i.toString());
                catExp.put(ordCat, catExpUnordered.get(ordCat));
                //LOG.info("OC: " + ordCat + "|" + catExpUnordered.get(ordCat));
            }
            
            request.setAttribute("catExp", catExp);
            
            Map<String, List<GBrowseTrack>> tracks = MetadataCache.getExperimentGBrowseTracks(os);
            request.setAttribute("tracks", tracks);

        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
    
}
