package org.modmine.web;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.model.bio.ResultFile;
import org.intermine.model.bio.Submission;
import org.intermine.model.bio.SubmissionProperty;
import org.intermine.util.Util;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.GBrowseParser.GBrowseTrack;
import org.modmine.web.logic.ModMineUtil;

/**
 * Class that generates GBrowse track links for a List of submissions.
 * @author Fengyuan Hu
 */
public class SubListGBrowseTrackController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(SubListGBrowseTrackController.class);
    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        InterMineBag bag = (InterMineBag) request.getAttribute("bag");

        Class c = null;
        try {
            // or use -
            // Class.forName(im.getObjectStore().getModel().getPackageName() + "." + bag.getType());
            // Class is: interface org.intermine.model.bio.Submission
            c = Class.forName(bag.getQualifiedType());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        if (!"Submission".equals(bag.getType())) { return null; }

        // Logic 1: query all the DccId for the list of submission in the bag, refer to
        //          OrthologueLinkController and BioUtil

        Set<Submission> subs = ModMineUtil.getSubmissions(im.getObjectStore(), bag);

        Set<String> subDCCids = new LinkedHashSet<String>();
        for (Submission sub : subs) {
            subDCCids.add(sub.getdCCid());
        }

        // Logic 2: find all the GBrowseTrack from MetadataCache
        // Map key: Organism
        Map<String, Map<String, List<GBrowseTrack>>> tracks =
            new LinkedHashMap<String, Map<String, List<GBrowseTrack>>>();

        Set<String> orgSet = new LinkedHashSet<String>();
        for (Submission sub : subs) {
            orgSet.add(sub.getOrganism().getShortName());
        }

        for (String orgName : orgSet) {
            // Map Key: Submission DCCid
            Map<String, List<GBrowseTrack>> track =
                new LinkedHashMap<String, List<GBrowseTrack>>();
            for (Submission sub : subs) {
                // DCCid 2530, 2607 - D. Pseudoobscura
                if (sub.getOrganism().getShortName() != null
                        && sub.getOrganism().getShortName().equals(orgName)) {
                    List<GBrowseTrack> trackList = MetadataCache.getTracksByDccId(sub.getdCCid());
                    if (!trackList.isEmpty()) {
                        track.put(sub.getTitle(), trackList);
                    }
                }
                tracks.put(orgName, track);
            }
        }

        for (Map.Entry<String, Map<String, List<GBrowseTrack>>> entry : tracks.entrySet()) {
            if (entry.getValue().isEmpty() || entry.getValue().equals(null)) {
                tracks.remove(entry.getKey());
            }
        }

        request.setAttribute("tracks", tracks);

        String GBROWSE_DEFAULT_URL =
            "http://modencode.oicr.on.ca/fgb2/gbrowse/";
        String GBROWSE_BASE_URL = GBrowseParser.getGBrowsePrefix();

        if (GBROWSE_BASE_URL.equals(null) || GBROWSE_BASE_URL.isEmpty()) {
            request.setAttribute("GBROWSE_BASE_URL", GBROWSE_DEFAULT_URL);
        } else {
            request.setAttribute("GBROWSE_BASE_URL", GBROWSE_BASE_URL);
        }

        
        return null;
    }
}
