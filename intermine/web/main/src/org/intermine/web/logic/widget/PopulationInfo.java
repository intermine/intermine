package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2016 FlyMine
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
public final class PopulationInfo
{
    private final int size;
    private final float extraAttribute;

    /**
     * Constructor
     * @param size the size of the population.
     * @param extraAttribute The extra attribute.
     */
    public PopulationInfo(int size, float extraAttribute) {
        this.size = size;
        this.extraAttribute = extraAttribute;
    }

    /** @return the size of the population **/
    public int getSize() {
        return size;
    }

    /** @return the value of the extra attribute. **/
    public float getExtraAttribute() {
        return extraAttribute;
    }
}
