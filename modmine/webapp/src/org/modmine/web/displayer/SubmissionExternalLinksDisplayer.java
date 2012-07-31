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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.model.bio.ResultFile;
import org.intermine.model.bio.Submission;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;
import org.json.JSONArray;
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
    // Map<DCCid, Map<database, List<accession, url>>>
    private static Map<String, Map<String, List<String>>> submissionDatabaseRecordsInfo = null;

    protected static final Logger LOG = Logger.getLogger(SubmissionExternalLinksDisplayer.class);

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
        Submission o = (Submission) reportObject.getObject();
        Profile profile = SessionMethods.getProfile(request.getSession());

        String dccId = o.getdCCid();

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
        request.setAttribute("dbRecordsJSON", getDbRecordsJSON(os, profile, im, dccId));
    }

    /**
     * Fetch experiment details for display.
     *
     * @param im InterMineAPI
     * @param profile Profile
     * @return a list of experiments
     */
    public static synchronized Map<String, Map<String, List<String>>> getSubmissionDatabaseRecords(
            InterMineAPI im, Profile profile) {
        if (submissionDatabaseRecordsInfo == null) {
            PathQuery query = new PathQuery(im.getObjectStore().getModel());

            query.addViews("Submission.DCCid",
                    "Submission.databaseRecords.database",
                    "Submission.databaseRecords.accession",
                    "Submission.databaseRecords.url");

            query.addOrderBy("Submission.DCCid", OrderDirection.ASC);

            ExportResultsIterator results = im.getPathQueryExecutor(profile).execute(query);

            if (results == null || !results.hasNext()) {
//          throw new Exception("None of the submissions has database records information...");
            } else {
                while (results.hasNext()) {
                    List<ResultElement> row = results.next();

                    String dCCId = (String) row.get(0).getField();
                    String db = (String) row.get(1).getField();
                    String acc = (String) row.get(2).getField();
                    String url = (String) row.get(3).getField();

//                    if (dbRecordMap.containsKey(db)) {
//                        dbRecordMap.get(db).add(a);
//                    } else {
//                        List<String> ll = new ArrayList<String>();
//                        ll.add(a);
//
//                        dbRecordMap.put(db, ll);
//                    }
                }
            }
        }
        return submissionDatabaseRecordsInfo;
    }


    private static synchronized String getDbRecordsJSON(ObjectStore os, Profile profile,
            InterMineAPI im, String dccId) {

        String dbRecordsJSON = null;

        PathQuery query = new PathQuery(os.getModel());

        // Add views
        query.addViews("Submission.databaseRecords.database",
                "Submission.databaseRecords.accession",
                "Submission.databaseRecords.url");

        // Add constraints and you can edit the constraint values below
        query.addConstraint(Constraints.eq("Submission.DCCid", dccId));

        ExportResultsIterator results = im.getPathQueryExecutor(profile).execute(query);

        if (results == null || !results.hasNext()) {
            return null;
        } else {
            Map<String, Set<String>> dbRecordMap =
                new LinkedHashMap<String, Set<String>>();

            while (results.hasNext()) {
                List<ResultElement> row = results.next();

                String db = (String) row.get(0).getField();
                String acc = (String) row.get(1).getField();
                String url = (String) row.get(2).getField();

                String a;

                if ("To be confirmed".equals(acc)) {
                    a = acc;
                } else {
                    // To escape double qoute in json
                    a = "<a target=\\\"_blank\\\" href=\\\"" + url + "\\\">" + acc + "</a>";
                }

                if (dbRecordMap.containsKey(db)) {
                    dbRecordMap.get(db).add(a);
                } else {
                    Set<String> ll = new LinkedHashSet<String>();
                    ll.add(a);

                    dbRecordMap.put(db, ll);
                }
            }

            // Parse map to JSON
            List<Object> dbl = new ArrayList<Object>();
            for (Entry<String, Set<String>> e : dbRecordMap.entrySet()) {
                Map<String, Object> dbm = new LinkedHashMap<String, Object>();
                dbm.put("dbName", e.getKey());
                List<Object> r = new ArrayList<Object>();
                for (String s : e.getValue()) {
                    r.add(s);
                }
                dbm.put("dbRecords", r);
                dbl.add(dbm);
            }

            JSONArray ja = new JSONArray(dbl);

            // Note: replace "\" in java -> "\\\\"
            dbRecordsJSON = ja.toString().replaceAll("<\\\\/a>", "</a>");
            return dbRecordsJSON;
        }
    }
}
