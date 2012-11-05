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

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.query.PathQueryExecutor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.bio.web.export.BEDExporter;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.export.Exporter;
import org.intermine.webservice.server.WebServiceRequestParser;
import org.intermine.webservice.server.exceptions.InternalErrorException;

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
    protected void export(PathQuery pq, Profile profile) {
        boolean makeUcscCompatible = true;

        String ucscCompatible = request.getParameter(UCSC_COMPATIBLE);
        if ("no".equals(ucscCompatible)) {
            makeUcscCompatible = false;
        }

        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        // get the project title to be written in BED records
        Properties props = (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        String sourceName = props.getProperty("project.title");
        String sourceReleaseVersion = props.getProperty("project.releaseVersion");

        String trackDescription = request.getParameter(TRACK_DESCRIPTION);
        if (StringUtils.isBlank(trackDescription)) {
            trackDescription = sourceName + " " + sourceReleaseVersion + " Custom Track";
        }

        String organisms = null;
        try {
            Set<String> orgSet = SequenceFeatureExportUtil.getOrganisms(pq, session);
            organisms = StringUtils.join(orgSet, ",");
        } catch (Exception e) {
            throw new RuntimeException(pq.toString()
                + " does not have organism as reference - "
                + "Only the sequence-feature type is supported.", e);
        }

        Exporter exporter;
        try {
            List<Integer> indexes = Arrays.asList(new Integer(0));

            exporter = new BEDExporter(pw, indexes, sourceName, organisms,
                    makeUcscCompatible, trackDescription);
            ExportResultsIterator iter = null;
            try {
                PathQueryExecutor executor = this.im.getPathQueryExecutor(profile);
                iter = executor.execute(pq, 0, WebServiceRequestParser.DEFAULT_MAX_COUNT);
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

    @Override
    protected String getContentType() {
        return "text/x-ucsc-bed";
    }

}
