package org.intermine.bio.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.WebResultsExecutor;
import org.intermine.api.results.WebResults;
import org.intermine.bio.web.logic.CytoscapeNetworkService;
import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OrderDirection;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.export.http.TableExporterFactory;
import org.intermine.web.logic.export.http.TableHttpExporter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.web.struts.TableExportForm;

/**
 * Export network from Cytoscape Web as in different formats:
 * "png", "pdf", "xgmml", "graphml", "sif", "svg", "tsv", "csv".
 *
 * @author Fengyuan Hu
 */
public class CytoscapeNetworkExportAction extends Action
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        String type = request.getParameter("type"); // tab or csv

        if ("sif".equals(type)) {
            response.setContentType("text/plain");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.sif\"");
        }
        if ("xgmml".equals(type)) {
            response.setContentType("text/xml");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.xgmml\"");
        }
        if ("graphml".equals(type)) {
            response.setContentType("text/xml");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.graphml\"");
        }
        if ("svg".equals(type)) {
            response.setContentType("image/svg+xml");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.svg\"");
        }
        if ("png".equals(type)) {
            response.setContentType("image/png");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.png\"");
        }
        if ("pdf".equals(type)) { // might not be supported in the future
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.pdf\"");
        }

        if ("tab".equals(type) || "csv".equals(type)) {
            // A comma delimited string of ids
            String fullInteractingGeneSetStr = request.getParameter("fullInteractingGeneSet");
            toExportNetworkAsList(type, fullInteractingGeneSetStr, request, response);
            return null;
        }

        // Handle large network
        if ("large_network".equals(type)) {
            String fullInteractingGeneSetStr = request.getParameter("fullInteractingGeneSet");

            CytoscapeNetworkService networkSrv = new CytoscapeNetworkService();
            String networkdata = networkSrv.getNetwork(
                    fullInteractingGeneSetStr, request.getSession(), true);

            response.setContentType("text/xml");
            response.setHeader("Content-Disposition", "attachment; filename=\"network.xgmml\"");
            PrintWriter out = response.getWriter();
            out.println(networkdata);
            out.flush();
            out.close();

            return null;
        }

        ServletInputStream is = request.getInputStream();
        ServletOutputStream out = response.getOutputStream();

        byte[] b = new byte[16384];

        int i = 0;
        while ((i = is.read(b)) != -1) {
            out.write(b, 0, i);
        }

        out.flush();
        out.close();

        return null;
    }

    /**
     * To export network as tab or csv. Run a query which gives the interaction information.
     *
     * @param format tab or csv
     * @param hub the central gene
     * @param request http request
     * @param response http response
     * @throws Exception
     */
    private void toExportNetworkAsList(String format, String fullInteractingGeneSetStr,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Model model = im.getModel();
        Profile profile = SessionMethods.getProfile(session);

        //=== Parse string to a set of integer of ids ===
        Set<Integer> fullInteractingGeneSet = new HashSet<Integer>();
        String[] fullInteractingGeneSetStrArr = StringUtil.split(fullInteractingGeneSetStr, ",");
        for (String s : fullInteractingGeneSetStrArr) {
            fullInteractingGeneSet.add(Integer.valueOf(s));
        }

        //=== Create and run a query ===
        PathQuery q = new PathQuery(model);
        q.addViews("Gene.symbol",
                "Gene.primaryIdentifier",
                "Gene.interactions.interactionType",
                "Gene.interactions.interactingGenes.symbol",
                "Gene.interactions.interactingGenes.primaryIdentifier",
                "Gene.interactions.dataSets.dataSource.name",
                "Gene.interactions.experiment.publication.title",
                "Gene.interactions.experiment.publication.pubMedId");

        q.addOrderBy("Gene.symbol", OrderDirection.ASC);
        q.addConstraint(Constraints.inIds("Gene", fullInteractingGeneSet), "B");
        q.addConstraint(Constraints.inIds("Gene.interactions.interactingGenes",
                fullInteractingGeneSet), "A");
        q.setConstraintLogic("B and A");

        WebResultsExecutor wrExecutor = im.getWebResultsExecutor(profile);
        PagedTable pt = new PagedTable(wrExecutor.execute(q));

        if (pt.getWebTable() instanceof WebResults) {
            ((WebResults) pt.getWebTable()).goFaster();
        }

        //=== Export data ===
        WebConfig webConfig = SessionMethods.getWebConfig(request);
        TableExporterFactory factory = new TableExporterFactory(webConfig);

        TableHttpExporter exporter = factory.getExporter(format);

        if (exporter == null) {
            throw new RuntimeException("unknown export format: " + format);
        }

        TableExportForm exportForm = new TableExportForm();
        // Ref class - StandardHttpExporter
        exportForm.setIncludeHeaders(true);

        exporter.export(pt, request, response, exportForm, null, null);
    }
}
