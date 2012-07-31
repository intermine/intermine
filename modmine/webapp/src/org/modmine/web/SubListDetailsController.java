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
import org.modmine.web.logic.ModMineUtil;

/**
 * Class that generates GBrowse track links for a List of submissions.
 * @author fh
 */
public class SubListDetailsController extends TilesAction
{
    protected static final Logger LOG = Logger.getLogger(SubListDetailsController.class);
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

        Set<String> orgSet = new LinkedHashSet<String>();
        for (Submission sub : subs) {
            orgSet.add(sub.getOrganism().getShortName());
        }


        /* ======== FILES ========== */
        // do the same for files associated with a submission
        // note: we need submission and not dccId because the gbrowse displayer uses
        // submissions titles.
        Map<Submission, List<ResultFile>> subFiles =
            new LinkedHashMap<Submission, List<ResultFile>>();
        for (Submission sub : subs) {
            List<ResultFile> files =
                MetadataCache.getFilesByDccId(im.getObjectStore(), sub.getdCCid());
            for (ResultFile file : files) {
                String fileName = file.getName();
                int index = fileName.lastIndexOf(System.getProperty("file.separator"));
                file.setName(fileName.substring(index + 1));
            }
            subFiles.put(sub, files);
        }
        request.setAttribute("files", subFiles);


//        /* ======== PROPERTIES ========== */
//        // do the same for properties associated with a submission
//        // note: we need submission and not dccId because the gbrowse displayer uses
//        // submissions titles.
        Map<Submission, List<SubmissionProperty>> subProps =
            new LinkedHashMap<Submission, List<SubmissionProperty>>();
        for (Submission sub : subs) {
            for (SubmissionProperty prop : sub.getProperties()) {
//                String subName =  prop.getName();
//                int index = fileName.lastIndexOf(System.getProperty("file.separator"));
//                file.setName(fileName.substring(index + 1));
                Util.addToListMap(subProps, sub, prop);
            }
        }
        request.setAttribute("props", subProps);


        /* ======== REPOSITORY ENTRIES ========== */
        // do the same for files associated with a submission
        // note: we need submission and not dccId because the gbrowse displayer uses
        // submissions titles.
        Map<Submission, List<String[]>> subReposited =
            new LinkedHashMap<Submission, List<String[]>>();
        for (Submission sub : subs) {
            List<String[]> reposited =
                MetadataCache.getRepositoryEntriesByDccId(im.getObjectStore(), sub.getdCCid());
            subReposited.put(sub, reposited);
        }
        request.setAttribute("reposited", subReposited);

        return null;
    }
}
