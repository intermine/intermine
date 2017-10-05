package org.intermine.bio.webservice;

/*
 * Copyright (C) 2002-2016 FlyMine
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
import org.intermine.bio.web.export.GFF3Exporter;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil.InvalidQueryException;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
*
* @author Alexis Kalderimis.
*
*/
public class GenomicRegionGFF3Service extends AbstractRegionExportService
{
    /**
     * Constructor.
     * @param im A reference to an InterMine API settings bundle.
     */
    public GenomicRegionGFF3Service(InterMineAPI im) {
        super(im);
    }

    /**
     * @param pq pathquery
     * @return the exporter
     */
    protected GFF3Exporter getExporter(PathQuery pq) {
        String sourceName = webProperties.getProperty("project.title");
        Set<Integer> organisms = null;
        List<Integer> indexes = new ArrayList<Integer>();
        List<String> viewColumns = new ArrayList<String>(pq.getView());
        for (int i = 0; i < viewColumns.size(); i++) {
            indexes.add(Integer.valueOf(i));
        }
        GFFQueryService.removeFirstItemInPaths(viewColumns);
        return new GFF3Exporter(
            getPrintWriter(), indexes, GFFQueryService.getSoClassNames(),
            viewColumns, sourceName, organisms, false);
    }

    @Override
    protected String getSuffix() {
        return ".gff3";
    }

    @Override
    protected String getContentType() {
        return "text/x-gff3";
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
