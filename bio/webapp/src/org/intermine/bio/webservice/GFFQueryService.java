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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.bio.web.export.GFF3Exporter;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.webservice.server.Formats;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.BadRequestException;
import org.intermine.webservice.server.exceptions.InternalErrorException;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.TabFormatter;
import org.intermine.webservice.server.query.AbstractQueryService;
import org.intermine.webservice.server.query.result.PathQueryBuilder;

/**
 * A service for exporting query results as gff3.
 * @author Alexis Kalderimis.
 *
 */
public class GFFQueryService extends AbstractQueryService
{

    private static final String XML_PARAM = "query";

    /**
     * Constructor.
     * @param im A reference to an InterMine API settings bundle.
     */
    public GFFQueryService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getDefaultFileName() {
        return "results" + StringUtil.uniqueString() + ".gff3";
    }

    protected PrintWriter pw;

    @Override
    protected Output getDefaultOutput(PrintWriter out, OutputStream os, String sep) {
        this.pw = out;
        output = new StreamedOutput(out, new TabFormatter(), sep);
        if (isUncompressed()) {
            ResponseUtil.setPlainTextHeader(response,
                    getDefaultFileName());
        }
        return output;
    }

    @Override
    public int getDefaultFormat() {
        return Formats.UNKNOWN;
    }

    @Override
    protected void execute() throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        // get the project title to be written in GFF3 records
        Properties props = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String sourceName = props.getProperty("project.title");

        Set<Integer> organisms = null;

        PathQuery pathQuery = getQuery();

        Exporter exporter;
        try {
            List<Integer> indexes = new ArrayList<Integer>();
            List<String> viewColumns = new ArrayList<String>(pathQuery.getView());
            for (int i = 0; i < viewColumns.size(); i++) {
                indexes.add(Integer.valueOf(i));
            }

            removeFirstItemInPaths(viewColumns);
            exporter = new GFF3Exporter(pw, indexes,
                    getSoClassNames(servletContext), viewColumns,
                    sourceName, organisms, false);
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
     * Read the SO term name to class name mapping file and return it as a Map from class name to
     * SO term name.  The Map is cached as the SO_CLASS_NAMES attribute in the servlet context.
     *
     * @throws ServletException if the SO class names properties file cannot be found
     * @param servletContext the ServletContext
     * @return a map
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map<String, String> getSoClassNames(ServletContext servletContext)
        throws ServletException {
        final String soClassNames = "SO_CLASS_NAMES";
        Properties soNameProperties;
        if (servletContext.getAttribute(soClassNames) == null) {
            soNameProperties = new Properties();
            try {
                InputStream is =
                    servletContext.getResourceAsStream("/WEB-INF/soClassName.properties");
                soNameProperties.load(is);
            } catch (Exception e) {
                throw new ServletException("Error loading so class name mapping file", e);
            }

            servletContext.setAttribute(soClassNames, soNameProperties);
        } else {
            soNameProperties = (Properties) servletContext.getAttribute(soClassNames);
        }

        return new HashMap<String, String>((Map) soNameProperties);
    }

    /**
     *
     * @param paths list of path views
     */
    public static void removeFirstItemInPaths(List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            paths.set(i, path.substring(path.indexOf(".") + 1, path.length()));
        }
    }

    /**
     * Return the query specified in the request, shorn of all duplicate
     * classes in the view. Note, it is the users responsibility to ensure
     * that there are only SequenceFeatures in the view.
     *
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
