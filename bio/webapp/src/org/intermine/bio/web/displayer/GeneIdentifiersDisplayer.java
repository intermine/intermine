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

import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 *
 * @author radek
 *
 */
public class GeneIdentifiersDisplayer extends ReportDisplayer
{

    protected static final Logger LOG = Logger.getLogger(GeneIdentifiersDisplayer.class);

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public GeneIdentifiersDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    /** @String[] which identifiers to use? */
    private final String[] identifiers = new String []{"primaryIdentifier", "secondaryIdentifier",
        "symbol"};

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        InterMineObject imObj = reportObject.getObject();
        try {
            for (String identifier : identifiers) {
                Object id = imObj.getFieldValue(identifier);
                if (id != null) {
                    String value = id.toString();
                    if (!result.values().contains(value)) {
                        result.put(identifier, value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        request.setAttribute("identifiers", result);
    }

}
