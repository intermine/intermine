package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.intermine.api.InterMineAPI;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 * displayer for esyn
 * @author Julie Sullivan
 */
public class EsynDisplayer extends ReportDisplayer
{

    protected static final Logger LOG = Logger.getLogger(EsynDisplayer.class);

    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public EsynDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }


    @SuppressWarnings("unchecked")
    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
    }

}
