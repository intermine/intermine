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

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

/**
 * Form for metabolicMine SNP list to nearby Genes results/list
 * @author radek
 *
 */
@SuppressWarnings("serial")
public class SnpToGeneForm extends ActionForm {

    private String direction;
    private String distance;
    private String bagName;

    /**
     * @return the bagName
     */
    public String getBagName() {
        return bagName;
    }

    /**
     * @param bagName the bagName to set
     */
    public void setBagName(String bagName) {
        this.bagName = bagName;
    }

    /**
     * @return the distance
     */
    public String getDistance() {
        return distance;
    }

    /**
     * @param distance the distance to set
     */
    public void setDistance(String distance) {
        this.distance = distance;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(String direction) {
        this.direction = direction;
    }

    /**
     * Get the bag type
     * @return the bag type string
     */
    public String getDirection() {
        return direction;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        direction = null;
    }
}
