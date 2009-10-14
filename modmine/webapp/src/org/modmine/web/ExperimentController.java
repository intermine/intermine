package org.modmine.web;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.MetadataCache.GBrowseTrack;

/**
 * Set up modENCODE experiments for display.
 * @author Richard Smith
 *
 */

public class ExperimentController extends TilesAction 
{    
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ObjectStore os = SessionMethods.getObjectStore(session.getServletContext());
        
        List<DisplayExperiment> experiments;
        
        String experimentName = request.getParameter("experiment");
        if (experimentName != null) {
            experiments = new ArrayList<DisplayExperiment>();
            experiments.add(MetadataCache.getExperimentByName(os, experimentName));
        } else {
            experiments = MetadataCache.getExperiments(os);
        }
        request.setAttribute("experiments", experiments);
        
        Map<String, List<GBrowseTrack>> tracks = MetadataCache.getExperimentGBrowseTracks(os);
        request.setAttribute("tracks", tracks);
        
        return null;
    }
}
