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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.export.BEDExporter;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil.InvalidQueryException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.export.Exporter;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 *
 * @author Alexis Kalderimis.
 *
 */
public class GenomicRegionBedService extends AbstractRegionExportService
{
    protected static final String SUFFIX = ".fasta";
    private static final String UCSC_COMPATIBLE = "ucscCompatible";
    private static final String TRACK_DESCRIPTION = "trackDescription";

    /**
     * Constructor.
     * @param im A reference to an InterMine API settings bundle.
     */
    public GenomicRegionBedService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected Exporter getExporter(PathQuery pq) {
        boolean isUcsc = !"no".equalsIgnoreCase(getOptionalParameter(UCSC_COMPATIBLE, "yes"));

        // get the project title to be written in BED records
        String sourceName = webProperties.getProperty("project.title");
        String sourceReleaseVersion = webProperties.getProperty("project.releaseVersion");
        String descr = sourceName + " " + sourceReleaseVersion + " Custom Track";
        String trackDescription = getOptionalParameter(TRACK_DESCRIPTION, descr);

        String organisms = StringUtils.join(
            SequenceFeatureExportUtil.getOrganisms(pq, im, getPermission().getProfile()), ",");
        List<Integer> indexes = Arrays.asList(new Integer(0));

        return new BEDExporter(getPrintWriter(), indexes, sourceName, organisms, isUcsc,
                trackDescription);
    }

    @Override
    protected String getContentType() {
        return "text/x-ucsc-bed";
    }

    @Override
    protected String getSuffix() {
        return ".bed";
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
