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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.metadata.StringUtil;
import org.intermine.web.logic.export.Exporter;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Format;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.ServiceException;
import org.intermine.webservice.server.lists.ListInput;
import org.intermine.webservice.server.output.Formatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.PlainFormatter;
import org.intermine.webservice.server.output.StreamedOutput;

/**
 * Base class for Biological region export services.
 * @author Alex Kalderimis
 *
 */
public abstract class AbstractRegionExportService extends GenomicRegionSearchService
{
    /**
     * Constructor.
     * @param im The InterMine API settings object.
     */
    public AbstractRegionExportService(InterMineAPI im) {
        super(im);
    }

    @Override
    public boolean isAuthenticated() {
        // Allow anyone to use this service, as it doesn't use a list, but an id-list query.
        return true;
    }

    @Override
    protected void validateState() {
        // what does this do
    }

    @Override
    protected void makeList(ListInput input, String type, Profile profile,
            Set<String> temporaryBagNamesAccumulator) throws Exception {

        GenomicRegionSearchListInput searchInput = (GenomicRegionSearchListInput) input;

        Set<Integer> objectIds = new HashSet<Integer>();
        Map<GenomicRegion, Query> queries = createQueries(searchInput.getSearchInfo());
        for (Entry<GenomicRegion, Query> e: queries.entrySet()) {
            Query q = e.getValue();
            ObjectStore objectstore = im.getObjectStore();
            Results rs = objectstore.execute(q);
            Iterator<Object> it = rs.iterator();
            while (it.hasNext()) {
                ResultsRow rr = (ResultsRow) it.next();
                Integer id = (Integer) rr.get(0);
                objectIds.add(id);
            }
        }

        PathQuery pq = makePathQuery(type, objectIds);
        export(pq, profile);
    }

    /**
     * Make a path-query from a bag.
     *
     * @param ids list of ids
     * @param type The bag to constrain this query on.
     * @return A path-query.
     * @throws Exception if something goes wrong
     */
    protected PathQuery makePathQuery(String type, Collection<Integer> ids) throws Exception {
        PathQuery pq = new PathQuery(im.getModel());
        pq.addView(type + ".primaryIdentifier");
        pq.addConstraint(Constraints.inIds(type, ids));
        checkPathQuery(pq);
        return pq;
    }

    /**
     * No-op stub. Override to implement format checks.
     * @param pq pathquery
     * @throws Exception if something goes wrong
     */
    protected void checkPathQuery(PathQuery pq) throws Exception {
        // No-op stub. Override to implement format checks.
    }

    /**
     * @param pq pathquery
     * @return the exporter
     */
    protected abstract Exporter getExporter(PathQuery pq);

    /**
     * Method that carries out the logic for this exporter.
     * @param pq The pathquery.
     * @param profile A profile to lookup saved bags in.
     */
    protected void export(PathQuery pq, Profile profile) {
        Exporter exporter = getExporter(pq);
        ExportResultsIterator iter = null;
        try {
            PathQueryExecutor executor = this.im.getPathQueryExecutor(profile);
            iter = executor.execute(pq, 0, WebServiceRequestParser.DEFAULT_LIMIT);
            iter.goFaster();
            exporter.export(iter);
        } catch (ObjectStoreQueryDurationException e) {
            throw new ServiceException("Query would take too long to run.", e);
        } catch (ObjectStoreException e) {
            throw new ServiceException("Could not run query.", e);
        } finally {
            if (iter != null) {
                iter.releaseGoFaster();
            }
        }
    }

    /**
     * @return The suffix for the file name.
     */
    protected abstract String getSuffix();

    @Override
    protected String getDefaultFileName() {
        return "results" + StringUtil.uniqueString() + getSuffix();
    }

    private PrintWriter pw;
    private OutputStream os;

    /**
     * @return printwriter
     */
    protected PrintWriter getPrintWriter() {
        return pw;
    }

    /**
     * @return outputstream
     */
    protected OutputStream getOutputStream() {
        return os;
    }

    /**
     * @return content type
     */
    protected abstract String getContentType();

    /**
     * @return formatter
     */
    protected Formatter getFormatter() {
        return new PlainFormatter();
    }

    @Override
    protected Output getDefaultOutput(PrintWriter out, OutputStream outputstream,
            String separator) {
        this.pw = out;
        this.os = outputstream;
        output = new StreamedOutput(out, getFormatter(), separator);
        if (isUncompressed()) {
            ResponseUtil.setCustomTypeHeader(response, getDefaultFileName(), getContentType());
        }
        return output;
    }

    @Override
    public Format getDefaultFormat() {
        return Format.UNKNOWN;
    }

}
