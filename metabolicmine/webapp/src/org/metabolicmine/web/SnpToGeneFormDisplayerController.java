package org.metabolicmine.web;

/*
 * Copyright (C) 2002-2012 metabolicMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Display form for metabolicMine SNP list to nearby Genes results/list and fill the input with <option>s
 * @author radek
 *
 */
public class SnpToGeneFormDisplayerController extends TilesAction {

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        try {
            // distance types
            LinkedHashMap<String, String> distanceTypes = new LinkedHashMap<String, String>();
            distanceTypes.put("0.5kb", ".5kb");
            distanceTypes.put("1.0kb", "1kb");
            distanceTypes.put("2.0kb", "2kb");
            distanceTypes.put("5.0kb", "5kb");
            distanceTypes.put("10.0kb", "10kb");
            request.setAttribute("distanceTypes", distanceTypes);

            // direction types
            LinkedHashMap<String, String> directionTypes = new LinkedHashMap<String, String>();
            directionTypes.put("upstream", "upstream");
            directionTypes.put("downstream", "downstream");
            directionTypes.put("both", "both ways");
            request.setAttribute("directionTypes", directionTypes);

        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

}