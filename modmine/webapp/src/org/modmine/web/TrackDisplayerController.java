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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.ResultFile;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.GBrowseParser.GBrowseTrack;

/**
 * Set up for the tracksDisplayer.jsp
 * @author Daniela Butano
 */
public class TrackDisplayerController extends TilesAction
{

    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();
        InterMineObject o = (InterMineObject) request.getAttribute("object");

        String dccId = ((Submission) o).getdCCid();

        List<GBrowseTrack> subTracks = MetadataCache.getTracksByDccId(dccId);
        request.setAttribute("subTracks", subTracks);

        List<ResultFile> files = MetadataCache.getFilesByDccId(os, dccId);
        for (ResultFile file : files) {
            String fileName = file.getName();
            int index = fileName.lastIndexOf(System.getProperty("file.separator"));
            file.setName(fileName.substring(index + 1));
        }

        request.setAttribute("files", files);
        request.setAttribute("filesNR", files.size());

        return null;
    }
}
