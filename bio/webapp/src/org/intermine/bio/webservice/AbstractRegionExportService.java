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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.model.GenomicRegion;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.Formats;
import org.intermine.webservice.server.lists.ListInput;
import org.intermine.webservice.server.output.Formatter;
import org.intermine.webservice.server.output.Output;
import org.intermine.webservice.server.output.StreamedOutput;
import org.intermine.webservice.server.output.TabFormatter;

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
        // Allow anyone to use this service, as it doesn't use a list, but 
        // an id-list query.
        return true;
    }

    @Override
    protected void makeList(ListInput input, String type, Profile profile,
            Set<String> temporaryBagNamesAccumulator) throws Exception {
        
        GenomicRegionSearchListInput searchInput = (GenomicRegionSearchListInput) input;

        Set<Integer> objectIds = new HashSet<Integer>();
        Map<GenomicRegion, Query> queries = createQueries(searchInput.getSearchInfo());
        for (Entry<GenomicRegion, Query> e: queries.entrySet()) {
            Query q = e.getValue();
            ObjectStore os = im.getObjectStore();
            Results rs = os.execute(q);
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
     * @param tempBag The bag to constrain this query on.
     * @return A path-query.
     */
    protected PathQuery makePathQuery(String type, Collection<Integer> ids) {
        PathQuery pq = new PathQuery(im.getModel());
        pq.addView(type + ".primaryIdentifier");
        pq.addConstraint(Constraints.inIds(type, ids));
        return pq;
    }

    /**
     * Method that carries out the logic for this exporter.
     * @param pq The pathquery.
     * @param profile A profile to lookup saved bags in.
     */
    protected abstract void export(PathQuery pq, Profile profile);

    /**
     * The suffix for the file name.
     */
    protected static String suffix;

    @Override
    protected String getDefaultFileName() {
        return "results" + StringUtil.uniqueString() + suffix;
    }

    protected PrintWriter pw;
    protected OutputStream os;

    protected abstract String getContentType();

    protected Formatter getFormatter() {
        return new TabFormatter();
    }

    @Override
    protected Output getDefaultOutput(PrintWriter out, OutputStream os, String separator) {
        this.pw = out;
        this.os = os;
        output = new StreamedOutput(out, getFormatter(), separator);
        if (isUncompressed()) {
            ResponseUtil.setCustomTypeHeader(response, getDefaultFileName(), getContentType());
        }
        return output;
    }

    @Override
    public int getDefaultFormat() {
        return Formats.UNKNOWN;
    }


}
