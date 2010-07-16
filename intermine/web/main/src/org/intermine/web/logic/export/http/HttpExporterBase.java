package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.WebResults;
import org.intermine.api.results.WebTable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Abstract class with functionality common for all classes implementing TableHttpExporter
 * interface like getting result from paged table.
 *
 * @author Jakub Kulaviak
 *
 */
public abstract class HttpExporterBase
{
    private static final int BATCH_SIZE = 5000;

    private PathQueryExecutor executor;

    /**
     * @param pt paged table
     * @param request request
     * @return all results of pathquery corresponding specified paged table.
     */
    public ExportResultsIterator getResultRows(PagedTable pt, HttpServletRequest request) {
        PathQuery pathQuery = pt.getWebTable().getPathQuery();
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);

        int batchSize = BATCH_SIZE;
        boolean matching = false;
        WebTable wt = pt.getWebTable();
        if (wt instanceof WebResults) {
            Results r = ((WebResults) wt).getInterMineResults();
            if (r.isSingleBatch()) {
                batchSize = r.getBatchSize();
                matching = true;
            }
        }

        executor = im.getPathQueryExecutor(profile);
        executor.setBatchSize(batchSize);
        executor.setMatchingExistingResults(matching);
        return executor.execute(pathQuery);
    }
}
