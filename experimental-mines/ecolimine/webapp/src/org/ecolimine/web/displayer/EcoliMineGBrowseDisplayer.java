package org.ecolimine.web.displayer;

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.NcRNA;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Displayer for GBrowse in ecoliMine
 * @author Fengyuan Hu
 */
public class EcoliMineGBrowseDisplayer extends ReportDisplayer
{
    /**
     * Construct with config and the InterMineAPI.
     * @param config to describe the report displayer
     * @param im the InterMine API
     */
    public EcoliMineGBrowseDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {

        InterMineObject object = reportObject.getObject();

        if (object instanceof Gene || object instanceof NcRNA) {
            int start = ((SequenceFeature) object).getChromosomeLocation().getStart();
            int end = ((SequenceFeature) object).getChromosomeLocation().getEnd();

            request.setAttribute("cord", start + ".." + end);
            request.setAttribute("cord_ext", (start - 1000) + ".." + (end + 1000));
        }

        request.setAttribute("object", object);

    }
}
