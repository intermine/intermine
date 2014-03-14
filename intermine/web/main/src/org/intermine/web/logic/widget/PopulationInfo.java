package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Container for size and extra attribute (e.g. gene length average) for the whole population
 * @author Daniela Butano
 *
 */
public class PopulationInfo {
    private int size;
    private Object extraAttribute;

    /**
     * Constructor
     * @param size
     * @param extraAttribute
     */
    public PopulationInfo(int size, Object extraAttribute) {
        this.size = size;
        this.extraAttribute = extraAttribute;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Object getExtraAttribute() {
        return extraAttribute;
    }

    public void setExtraAttribute(float extraAttribute) {
        this.extraAttribute = extraAttribute;
    }
}
