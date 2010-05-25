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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

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
            
            Properties props = new Properties(); 
            final ServletContext servletContext = servlet.getServletContext();
            
            InputStream is = 
                servletContext.getResourceAsStream("/WEB-INF/experimentCategory.properties");
            if (is == null) {
                LOG.info("Unable to find /WEB-INF/experimentCategory.properties!");
            } else {
                try {
                    props.load(is);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            Map <String, List<DisplayExperiment>> catExp =
                new HashMap<String, List<DisplayExperiment>>();
            
            for (List<DisplayExperiment> ll : experiments.values()) {
                for (DisplayExperiment de : ll) {
                    String cats = props.getProperty(de.getName());
                    // an experiment can be associated to more than 1 category
                    String[] cat = cats.split("#");
                    for (String c : cat) {
                        List<DisplayExperiment> des = catExp.get(c);
                        if (des == null) {
                            des = new ArrayList<DisplayExperiment>();
                            catExp.put(c, des);
                        }
                        des.add(de);
                        // LOG.info("DEXP: " + c + "|"+ de.getName());
                    }
                }
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
