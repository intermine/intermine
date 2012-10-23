package org.modmine.web.displayer;

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

import org.intermine.api.InterMineAPI;
import org.intermine.model.bio.ResultFile;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.MetadataCache;

/**
 *
 * @author Fengyuan Hu
 *
 */
public class SubmissionFileDownloadDisplayer extends ReportDisplayer
{
    /**
    *
    * @param config ReportDisplayerConfig
    * @param im InterMineAPI
    */
    public SubmissionFileDownloadDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();

        Submission s = (Submission) reportObject.getObject();

        String dccId = s.getdCCid();

        List<ResultFile> files = MetadataCache.getFilesByDccId(os, dccId);
        for (ResultFile file : files) {
            String fileName = file.getName();
            int index = fileName.lastIndexOf(System.getProperty("file.separator"));
            file.setName(fileName.substring(index + 1));
        }

        request.setAttribute("files", files);
        request.setAttribute("filesNR", files.size());
        request.setAttribute("dCCid", s.getdCCid());
    }
}
