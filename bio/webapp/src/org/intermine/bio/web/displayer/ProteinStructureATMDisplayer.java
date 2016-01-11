package org.intermine.bio.web.displayer;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import javax.servlet.http.HttpServletRequest;

import org.intermine.api.InterMineAPI;
import org.intermine.web.displayer.ReportDisplayer;
import org.intermine.web.logic.config.ReportDisplayerConfig;
import org.intermine.web.logic.results.ReportObject;

/**
 *
 * Protein Structure ATM Displayer turning the field into a link that shows the shebang in new
 * window.
 * @author Radek
 *
 */
public class ProteinStructureATMDisplayer extends ReportDisplayer
{

    /**
     *
     * @param config ReportDisplayerConfig
     * @param im InterMineAPI
     */
    public ProteinStructureATMDisplayer(ReportDisplayerConfig config, InterMineAPI im) {
        super(config, im);
    }

    @Override
    public void display(HttpServletRequest request, ReportObject reportObject) {
        try {
            request.setAttribute("atm", reportObject.getObject().getFieldValue("atm"));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
