package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.export.GAFExporter;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil.InvalidQueryException;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.StringUtil;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * A service for exporting query results in GAF format.
 *
 * @author Fengyuan Hu
 *
 */
public class GAFQueryService extends BioQueryService
{
    /**
     * Constructor.
     *
     * @param im A reference to an InterMine API settings bundle.
     */
    public GAFQueryService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getSuffix() {
        return ".gaf";
    }

    @Override
    protected String getContentType() {
        return "text/x-gaf";
    }

    @Override
    protected GAFExporter getExporter(PathQuery pq) {
        String taxonIds = null;
        Set<String> orgSet = SequenceFeatureExportUtil.getTaxonIds(pq, im, getPermission().getProfile());
        taxonIds = StringUtil.join(orgSet, ",");
        List<Integer> indexes = new ArrayList<Integer>();
        List<String> viewColumns = new ArrayList<String>(pq.getView());
        for (int i = 0; i < viewColumns.size(); i++) {
            indexes.add(Integer.valueOf(i));
        }

        return new GAFExporter(getPrintWriter(), indexes, taxonIds);
    }

    @Override
    protected void checkPathQuery(PathQuery pq) throws Exception {
        try {
            SequenceFeatureExportUtil.isValidSequenceFeatureQuery(pq);
        } catch (InvalidQueryException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

}
