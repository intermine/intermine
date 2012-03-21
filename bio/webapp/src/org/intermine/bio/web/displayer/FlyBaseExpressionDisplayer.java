package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.pathqueryresult.PathQueryResultHelper;
import org.intermine.web.logic.results.InlineResultsTable;
import org.intermine.web.logic.results.ReportObject;
import org.intermine.web.logic.session.SessionMethods;

/**
 * FlyBase modENCODE expression data
 *
 * @author Julie
 */
public class FlyBaseExpressionDisplayer extends ReportDisplayer
{

    /**
     * Constructor
     * @param config .
     * @param im .
     */
    public FlyBaseExpressionDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        // get the gene/protein in question from the request
        InterMineObject object = reportObject.getObject();

        // API connection
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        PathQuery query = new PathQuery(model);

        // cast me Gene
        Gene gene = (Gene) object;
        Object id = gene.getId();
        if (id != null) {
            // fetch the expression
            String geneId = String.valueOf(id);
            query = getQuery(geneId, query);

            // execute the query
            Profile profile = SessionMethods.getProfile(session);
            PathQueryExecutor executor = im.getPathQueryExecutor(profile);
            ExportResultsIterator values = executor.execute(query);

            Map results = processResults(values);
            request.setAttribute("flybaseResults", results);

            // inline collection table to toggle
            InlineResultsTable table = processTable(request, reportObject);
            request.setAttribute("flybaseCollection", table);

        }
    }

    private InlineResultsTable processTable(HttpServletRequest request, ReportObject reportObject) {
        // get the corresponding collection
        for (FieldDescriptor fd : reportObject.getClassDescriptor().getAllFieldDescriptors()) {
            if ("rnaSeqResults".equals(fd.getName()) && fd.isCollection()) {
                // fetch the collection
                Collection<?> collection = null;
                try {
                    collection = (Collection<?>)
                        reportObject.getObject().getFieldValue("rnaSeqResults");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                List<Class<?>> lc = PathQueryResultHelper.
                        queryForTypesInCollection(reportObject.getObject(), "rnaSeqResults",
                                im.getObjectStore());

                // create an InlineResultsTable
                InlineResultsTable t = new InlineResultsTable(collection,
                        fd.getClassDescriptor().getModel(),
                        SessionMethods.getWebConfig(request), im.getClassKeys(),
                        collection.size(), false, lc);
                return t;
            }
        }
        return null;
    }

    private Map<String, String> processResults(ExportResultsIterator it) {
        Map<String, String> results = new LinkedHashMap<String, String>();
        while (it.hasNext()) {
            List<ResultElement> row = it.next();
            String stage =  (String) row.get(0).getField();
            String expressionLevel =  (String) row.get(1).getField();
            results.put(stage, expressionLevel);
        }
        return results;
    }

    private PathQuery getQuery(String geneId, PathQuery query) {
        query.addViews(
                "Gene.rnaSeqResults.stage",
                "Gene.rnaSeqResults.expressionLevel",
                "Gene.rnaSeqResults.expressionScore",
                "Gene.primaryIdentifier");
        query.addConstraint(Constraints.eq("Gene.id", geneId));
        query.addOrderBy("Gene.rnaSeqResults.stage", OrderDirection.ASC);
        return query;
    }

}
