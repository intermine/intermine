package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.web.logic.export.ResponseUtil;
import org.intermine.webservice.server.lists.ListInput;
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
        // Allow anyone to use this service, even though it
        // uses a list to do its dirty work.
        return true;
    }

    @Override
    protected void makeList(ListInput input, String type, Profile profile,
            Set<String> temporaryBagNamesAccumulator) throws Exception {
        // Delete the list on end.
        temporaryBagNamesAccumulator.add(input.getListName());
        GenomicRegionSearchListInput searchInput = (GenomicRegionSearchListInput) input;
        InterMineBag tempBag = doListCreation(searchInput, profile, type);

        PathQuery pq = makePathQuery(tempBag);
        export(pq, profile);
    }

    /**
     * Make a path-query from a bag.
     * @param tempBag The bag to constrain this query on.
     * @return A path-query.
     */
    protected PathQuery makePathQuery(InterMineBag tempBag) {
        PathQuery pq = new PathQuery(im.getModel());
        pq.addView(tempBag.getType() + ".primaryIdentifier");
        pq.addConstraint(Constraints.in(tempBag.getType(), tempBag.getName()));
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

    @Override
    protected Output getDefaultOutput(PrintWriter out, OutputStream os, String separator) {
        this.pw = out;
        this.os = os;
        output = new StreamedOutput(out, new TabFormatter(), separator);
        if (isUncompressed()) {
            ResponseUtil.setPlainTextHeader(response,
                    getDefaultFileName());
        }
        return output;
    }

    @Override
    public int getFormat() {
        return UNKNOWN_FORMAT;
    }


}
