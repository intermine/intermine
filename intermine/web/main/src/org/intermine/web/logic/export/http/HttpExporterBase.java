package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.query.PathQueryExecutor;
import org.intermine.web.logic.results.ExportResultsIterator;
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
        executor = SessionMethods.getPathQueryExecutor(request.getSession());
        executor.setBatchSize(BATCH_SIZE);
        return executor.execute(pathQuery);
    }
}
