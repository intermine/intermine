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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.InterMineAPI;
import org.intermine.bio.web.export.GFF3Exporter;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil;
import org.intermine.bio.web.logic.SequenceFeatureExportUtil.InvalidQueryException;
import org.intermine.pathquery.PathQuery;
import org.intermine.web.context.InterMineContext;
import org.intermine.webservice.server.exceptions.BadRequestException;

/**
 * A service for exporting query results as gff3.
 * @author Alexis Kalderimis.
 *
 */
public class GFFQueryService extends BioQueryService
{
    /**
     * Constructor.
     * @param im A reference to an InterMine API settings bundle.
     */
    public GFFQueryService(InterMineAPI im) {
        super(im);
    }

    @Override
    protected String getSuffix() {
        return ".gff3";
    }

    @Override
    protected String getContentType() {
        return "text/x-gff3";
    }

    /**
     * @param pq pathquery
     * @return the exporter
     */
    @Override
    protected GFF3Exporter getExporter(PathQuery pq) {
        String sourceName = webProperties.getProperty("project.title");
        Set<Integer> organisms = null;
        List<Integer> indexes = new ArrayList<Integer>();
        List<String> viewColumns = new ArrayList<String>(pq.getView());
        for (int i = 0; i < viewColumns.size(); i++) {
            indexes.add(Integer.valueOf(i));
        }
        removeFirstItemInPaths(viewColumns);
        return new GFF3Exporter(
            getPrintWriter(), indexes, getSoClassNames(),
            viewColumns, sourceName, organisms, false, getQueryPaths(pq));
    }

    /**
     *
     * @return map of SO class names
     */
    static Map<String, String> getSoClassNames() {
        return new HashMap<String, String>(
            (Map) InterMineContext.getAttribute(GFF3QueryServlet.SO_CLASS_NAMES));
    }

    /**
     * @param paths paths
     */
    static void removeFirstItemInPaths(List<String> paths) {
        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            paths.set(i, path.substring(path.indexOf(".") + 1, path.length()));
        }
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
