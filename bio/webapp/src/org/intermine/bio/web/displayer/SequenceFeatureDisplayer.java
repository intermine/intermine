package org.intermine.bio.web.displayer;

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
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * Displayer for gene sequence feature
 * @author rns, radek
 *
 */
public class SequenceFeatureDisplayer extends ReportDisplayer
{
    /** @var sets the max number of locations to show in a table, TODO: match with DisplayObj*/
    private Integer maximumNumberOfLocations = 27;

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public SequenceFeatureDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void display(HttpServletRequest request, ReportObject reportObject) {
        InterMineObject imObj = reportObject.getObject();
        Object loc = null;

        String imoClassName = imObj.getClass().getSimpleName();
        if (imoClassName.endsWith("Shadow")) {
            imoClassName = imoClassName.substring(0, imoClassName.indexOf("Shadow"));
        }
        request.setAttribute("objectClass", imoClassName);

        try {
            loc = imObj.getFieldValue("chromosomeLocation");
            // if the chromosomeLocation reference is null iterate over the contents of the
            //  locations collection and display each location where the locatedOn reference points
            //  to a Chromosome object
            if (loc == null) {
                Collection col = (Collection) imObj.getFieldValue("locations");
                List results = new ArrayList();
                Integer i = 0;
                for (Object item : col) {
                    // early exit
                    if (i == maximumNumberOfLocations) {
                        break;
                    }

                    InterMineObject imLocation = (InterMineObject) item;
                    // fetch where this object is located
                    Object locatedOnObject = imLocation.getFieldValue("locatedOn");
                    if (locatedOnObject != null) {
                        // are we Chromosome?
                        if ("Chromosome".equals(DynamicUtil.getSimpleClass(
                                (InterMineObject) locatedOnObject).getSimpleName())) {
                            results.add(item);
                        }
                    }
                    i++;
                }

                if (!results.isEmpty()) {
                    request.setAttribute("locationsCollection", results);
                    request.setAttribute("locationsCollectionSize", col.size());
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        // check if cytoLocation field exists and display it
        try {
            String cytoLocation = (String) imObj.getFieldValue("cytoLocation");
            if (!StringUtils.isBlank(cytoLocation)) {
                request.setAttribute("cytoLocation", cytoLocation);
            }
        } catch (IllegalAccessException e) {
            // this is expected for classes that don't have a cytoLocation attribute
        }

        // check if mapLocation field exists and display it
        try {
            String mapLocation = (String) imObj.getFieldValue("mapLocation");
            if (!StringUtils.isBlank(mapLocation)) {
                request.setAttribute("mapLocation", mapLocation);
            }
        } catch (IllegalAccessException e) {
            // this is expected for classes that don't have a cytoLocation attribute
        }
    }
}
