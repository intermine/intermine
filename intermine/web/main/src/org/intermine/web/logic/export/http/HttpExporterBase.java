package org.intermine.web.logic.export.http;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.WebUtil;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.ResultElement;
import org.intermine.webservice.server.core.PathQueryExecutor;

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
    public Iterator<List<ResultElement>> getResultRows(PagedTable pt, HttpServletRequest request) {
        PathQuery query = pt.getWebTable().getPathQuery();
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        Map<String, InterMineBag> bags = WebUtil.getAllBags(profile
                .getSavedBags(), session.getServletContext());
        executor = new PathQueryExecutor(request, query, bags);
        executor.setBatchSize(BATCH_SIZE);
        return executor.getResults();
    }
    
    /**
     * Releases go faster. Must be called in finally block of block where method getResultRows
     * is called. 
     */
    public void releaseGoFaster() {
        // goFaster is not used now
    }
}
