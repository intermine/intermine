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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.export.BEDExporter;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil.InvalidQueryException;
import org.intermine.pathquery.PathQuery;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * A service for exporting query results in UCSC BED format.
 *
 * @author Fengyuan Hu
 *
 */
public class BEDQueryService extends BioQueryService
{
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
    protected String getSuffix() {
        return ".bed";
    }

    @Override
    protected String getContentType() {
        return "text/x-ucsc-bed";
    }

    @Override
    protected BEDExporter getExporter(PathQuery pq) {
        String sourceName = webProperties.getProperty("project.title");
        String sourceReleaseVersion = webProperties.getProperty("project.releaseVersion");
        boolean makeUcscCompatible = !"no".equalsIgnoreCase(
                getOptionalParameter(UCSC_COMPATIBLE, "yes"));
        String trackDescription = getOptionalParameter(TRACK_DESCRIPTION,
                sourceName + " " + sourceReleaseVersion + " Custom Track");
        Set<String> orgs = SequenceFeatureExportUtil.getOrganisms(pq, im,
                getPermission().getProfile());
        List<Integer> indexes = new ArrayList<Integer>();
        List<String> viewColumns = new ArrayList<String>(pq.getView());
        for (int i = 0; i < viewColumns.size(); i++) {
            indexes.add(Integer.valueOf(i));
        }

        return new BEDExporter(getPrintWriter(), indexes, sourceName,
            StringUtils.join(orgs, ","), makeUcscCompatible, trackDescription);
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
