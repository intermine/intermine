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

import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.export.SequenceExporter;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil.InvalidQueryException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.export.Exporter;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
* A class for exposing the region search as a FASTA resource.
* @author Alexis Kalderimis.
*
*/
public class GenomicRegionFastaService extends AbstractRegionExportService
{

    protected static final String SUFFIX = ".fasta";

    /**
     * Constructor.
     * @param im A reference to an InterMine API settings bundle.
     */
    public GenomicRegionFastaService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getContentType() {
        return "text/x-fasta";
    }

    @Override
    protected Exporter getExporter(PathQuery pq) {
        return new SequenceExporter(im.getObjectStore(), getOutputStream(), 0,
                im.getClassKeys(), 0);
    }

    @Override
    protected String getSuffix() {
        return SUFFIX;
    }

    @Override
    protected void checkPathQuery(PathQuery pq) throws Exception {
        try {
            SequenceFeatureExportUtil.isValidFastaQuery(pq);
        } catch (InvalidQueryException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

}
