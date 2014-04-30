package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.model.bio.SequenceFeature;
import org.json.*;

/**
 * Displayer for JBrowse and GBrowse used with jgbrowseDisplayer.jsp
 * jsp Will display GBrowse if user is on a mobile device otherwise displays
 * JBrowse since JBrowse doesn't work on mobile devices
 * Requires report displayer config in webconfig-model.xml and 
 * GBrowse configs in ./intermine/XXXmine.properties
 * @author rns & sbn
 * 
 */
public class JGBrowseDisplayer extends ReportDisplayer
{

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public JGBrowseDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        String type = reportObject.getType();
	JSONObject params = config.getParameterJson();
        String baseURL = null;
        String tracks = "";

	// for GBrowse
        String className = reportObject.getClassDescriptor().getUnqualifiedName();

        try{
           baseURL = params.getString("baseURL");
           try{
              tracks = params.getString(type);
           }catch(Exception e){}

           if(tracks == null || tracks.trim().length()==0){
              tracks = params.getString("defaultTrack");
           }
        }catch (JSONException jse){
           throw new RuntimeException("Error with JBrowseDisplayer parameters ");
        }
        if(baseURL == null){
           throw new RuntimeException("baseURL for JBrowse is null. Check report displayer config");
         }
	String species = ((SequenceFeature)reportObject.getObject()).getOrganism().getSpecies();
        if("sapiens".equals(species)){
         baseURL =  baseURL.replace("mouse","GRCh38");
        }
        // JBrowse 
        request.setAttribute("baseURL",baseURL);         
        request.setAttribute("tracks", tracks);
   
        // GBrowse
        request.setAttribute("className",className);


    }
}
