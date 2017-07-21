package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2017 FlyMine
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.StringUtil;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.query.AbstractQueryService;
import org.intermine.webservice.server.query.result.PathQueryBuilder;

/**
 * A service for exporting query results as gff3.
 * @author Alexis Kalderimis.
 *
 */
public abstract class BioQueryService extends AbstractQueryService
{
    private static final String XML_PARAM = "query";
    private static final String VIEW_PARAM = "view";

    private PrintWriter pw;

    /**
     * @return print writer
     */
    public PrintWriter getPrintWriter() {
        return pw;
    }

    private OutputStream os;

    /**
     * @return output stream
     */
    public OutputStream getOutputStream() {
        return os;
    }

    /**
     * Constructor.
     * @param im A reference to an InterMine API settings bundle.
     */
    public BioQueryService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getDefaultFileName() {
        return "results" + StringUtil.uniqueString() + getSuffix();
    }

    /**
     * @return suffix
     */
    protected abstract String getSuffix();

    /**
     * @return content type
     */
    protected abstract String getContentType();

    @Override
    protected Output getDefaultOutput(PrintWriter out, OutputStream outputstream, String sep) {
        // Most exporters need direct access to these.
        this.os = outputstream;
        this.pw = out;
        output = new StreamedOutput(out, new PlainFormatter(), sep);
        if (isUncompressed()) {
            ResponseUtil.setCustomTypeHeader(response, getDefaultFileName(), getContentType());
        }
        return output;
    }

    @Override
    public Format getDefaultFormat() {
        return Format.UNKNOWN;
    }


    /**
     * Return the query specified in the request, shorn of all duplicate
     * classes in the view. Note, it is the users responsibility to ensure
     * that there are only SequenceFeatures in the view.
     *
     * @return A query.
     */
    protected PathQuery getQuery() {
        String xml = getRequiredParameter(XML_PARAM);
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

    /**
     * @param pq pathquery
     * @return exporter
     */
    protected abstract Exporter getExporter(PathQuery pq);

    /**
     * No-op stub. Put query validation here.
     * @param pq pathquery
     * @throws Exception if something goes wrong
     */
    protected void checkPathQuery(PathQuery pq) throws Exception {
        // No-op stub. Put query validation here.
    }

    @Override
    protected void execute() throws Exception {

        Profile profile = getPermission().getProfile();
        PathQueryExecutor executor = this.im.getPathQueryExecutor(profile);
        // For FASTA/BED/GFF only set Gene.id in the view in im-tables system
        PathQuery pathQuery = getQuery();
        checkPathQuery(pathQuery);

        // Bring back original views for extra fields to be included in data export
        // NB: Functional but bad practice?
        // view in http request will look like: view=Gene.name&view=Gene.length...
        // Support the standard mechanism for accepting multiple parameter values
        List<String> views = getPathQueryViews(request.getParameterValues(VIEW_PARAM));
        if (views != null) {
            try {
                pathQuery.addViews(views);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Bad value for view parameter", e);
            }
            // Remove duplicates in views
            ArrayList<String> al = new ArrayList<String>();
            al.clear();
            al.addAll(new LinkedHashSet<String>(pathQuery.getView()));
            pathQuery.clearView();
            pathQuery.addViews(al);
        }

        Exporter exporter = getExporter(pathQuery);

        ExportResultsIterator iter = null;
        try {
            iter = executor.execute(pathQuery, 0, WebServiceRequestParser.DEFAULT_LIMIT);
            iter.goFaster();
            exporter.export(iter);
        } finally {
            if (iter != null) {
                iter.releaseGoFaster();
            }
        }
    }

    /**
     * Parse path query views from request parameter "view" comma-separated
     *
     * @param views views in pathquery
     * @return a list of query view as string
     */
    protected static List<String> getPathQueryViews(String[] views) {
        if (views == null || views.length < 1) {
            return null;
        }

        List<String> viewList = new ArrayList<String>();
        for (String view : views) {
            viewList.add(view.trim());
        }

        return viewList;
    }

    /**
     * Parse view strings to Path objects
     *
     * @param pq pathquery
     * @return a list of query path
     */
    protected List<Path> getQueryPaths(PathQuery pq) {
        List<Path> paths = new ArrayList<Path>();
        for (String view : pq.getView()) {
            try {
                paths.add(pq.makePath(view));
            } catch (PathException e) {
                e.printStackTrace();
            }
        }
        return paths;
    }
}
