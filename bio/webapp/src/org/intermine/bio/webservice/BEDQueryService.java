package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.bio.web.export.BEDExporter;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.TabFormatter;
import org.intermine.webservice.server.query.AbstractQueryService;
import org.intermine.webservice.server.query.result.PathQueryBuilder;

/**
 * A service for exporting query results in UCSC BED format.
 *
 * @author Fengyuan Hu
 *
 */
public class BEDQueryService extends AbstractQueryService
{
    private static final String XML_PARAM = "query";
    private static final String TRACK_DESCRIPTION = "trackDescription";
    private static final String UCSC_COMPATIBLE = "ucscCompatible";

    /**
     * Constructor.
     *
     * @param im A reference to an InterMine API settings bundle.
     */
    public BEDQueryService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getDefaultFileName() {
        return "results" + StringUtil.uniqueString() + ".bed";
    }

    protected PrintWriter pw;

    @Override
    protected Output getDefaultOutput(PrintWriter pw, OutputStream os, String separator) {
        this.pw = pw;
        output = new StreamedOutput(pw, new TabFormatter(), separator);
        if (isUncompressed()) {
            ResponseUtil.setPlainTextHeader(response, getDefaultFileName());
        }
        return output;
    }

    @Override
    public int getFormat() {
        return UNKNOWN_FORMAT;
    }

    @Override
    protected void execute() throws Exception {
        PathQuery pathQuery = getQuery();

        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        // get the project title to be written in BED records
        Properties props = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String sourceName = props.getProperty("project.title");
        String sourceReleaseVersion = props.getProperty("project.releaseVersion");

        String trackDescription = request.getParameter(TRACK_DESCRIPTION);
        if ("".equals(trackDescription) || trackDescription == null) {
            trackDescription = sourceName + " " + sourceReleaseVersion + " Custom Track";
        }

        String organisms = null;
        try {
            Set<String> orgSet = SequenceFeatureExportUtil.getOrganisms(pathQuery, session);
            organisms = StringUtil.join(orgSet, ",");
        } catch (Exception e) {
            throw new RuntimeException(pathQuery.getRootClass()
                + " does not have organism as reference. "
                + "Non sequnce feature type is not supported...",
                    e);
        }

        boolean makeUcscCompatible = true;

        String ucscCompatible = request.getParameter(UCSC_COMPATIBLE);
        if ("no".equals(ucscCompatible)) {
            makeUcscCompatible = false;
        }

        Exporter exporter;
        try {
            List<Integer> indexes = new ArrayList<Integer>();
            List<String> viewColumns = new ArrayList<String>(pathQuery.getView());
            for (int i = 0; i < viewColumns.size(); i++) {
                indexes.add(Integer.valueOf(i));
            }

            exporter = new BEDExporter(pw, indexes, sourceName, organisms,
                    makeUcscCompatible, trackDescription);
            ExportResultsIterator iter = null;
            try {
                Profile profile = SessionMethods.getProfile(session);
                PathQueryExecutor executor = this.im.getPathQueryExecutor(profile);
                iter = executor.execute(pathQuery, 0, WebServiceRequestParser.DEFAULT_MAX_COUNT);
                iter.goFaster();
                exporter.export(iter);
            } finally {
                if (iter != null) {
                    iter.releaseGoFaster();
                }
            }
        } catch (Exception e) {
            throw new InternalErrorException("Service failed:" + e, e);
        }

    }

    /**
     * Return the query specified in the request, shorn of all duplicate
     * classes in the view. Note, it is the users responsibility to ensure
     * that there are only SequenceFeatures in the view.
     * @return A suitable pathquery for getting GFF3 data from.
     */
    protected PathQuery getQuery() {
        String xml = request.getParameter(XML_PARAM);

        if (StringUtils.isEmpty(xml)) {
            throw new BadRequestException("query is blank");
        }

        PathQueryBuilder builder = getQueryBuilder(xml);
        PathQuery pq = builder.getQuery();

        List<String> newView = new ArrayList<String>();
        Set<ClassDescriptor> seenTypes = new HashSet<ClassDescriptor>();

        for (String viewPath: pq.getView()) {
            Path p;
            try {
                p = new Path(pq.getModel(), viewPath);
            } catch (PathException e) {
                throw new BadRequestException("Query is invalid", e);
            }
            ClassDescriptor cd = p.getLastClassDescriptor();
            if (!seenTypes.contains(cd)) {
                newView.add(viewPath);
            }
            seenTypes.add(cd);
        }
        if (!newView.equals(pq.getView())) {
            pq.clearView();
            pq.addViews(newView);
        }

        return pq;
    }
}
