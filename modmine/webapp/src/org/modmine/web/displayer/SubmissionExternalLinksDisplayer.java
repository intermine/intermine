package org.modmine.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.ResultFile;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;
import org.modmine.web.GBrowseParser.GBrowseTrack;
import org.modmine.web.MetadataCache;

/**
 * Set up for the submissionGBrowseTracksDisplayer.jsp
 *
 * @author Daniela Butano
 * @author Fengyuan Hu
 *
 */
public class SubmissionExternalLinksDisplayer extends ReportDisplayer
{
    /**
     *
     * @param config ReportDisplayerConfig
     * @param im InterMineAPI
     */
    public SubmissionExternalLinksDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        // Removed logics from TrackDisplayerController

        final InterMineAPI im = SessionMethods.getInterMineAPI(request.getSession());
        ObjectStore os = im.getObjectStore();
        InterMineObject o = (InterMineObject) reportObject.getObject();
        Profile profile = SessionMethods.getProfile(request.getSession());

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

        // Database records
        PathQuery query = new PathQuery(os.getModel());

        // Add views
        query.addViews("Submission.databaseRecords.database",
                "Submission.databaseRecords.accession",
                "Submission.databaseRecords.url");

        // Add constraints and you can edit the constraint values below
        query.addConstraint(Constraints.eq("Submission.DCCid", dccId));

        ExportResultsIterator results = im.getPathQueryExecutor(profile).execute(query);

        if (results == null || !results.hasNext()) {
            request.setAttribute("dbRecords", null);
        } else {

            Map<String, List<String>> dbRecordMap =
                new LinkedHashMap<String, List<String>>();

            while (results.hasNext()) {
                List<ResultElement> row = results.next();

                String db = (String) row.get(0).getField();
                String acc = (String) row.get(1).getField();
                String url = (String) row.get(2).getField();

                String a;

                if ("To be confirmed".equals(acc)) {
                    a = acc;
                } else {
                    a = "<a href=\"" + url + "\">" + acc + "</a>";
                }

                if (dbRecordMap.containsKey(db)) {
                    dbRecordMap.get(db).add(a);
                } else {
                    List<String> ll = new ArrayList<String>();
                    ll.add(a);

                    dbRecordMap.put(db, ll);
                }
            }

            request.setAttribute("dbRecords", dbRecordMap);
        }
    }
}
