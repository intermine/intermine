package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Container for size and gene length average (only for genes) for the whole population
 * @author Daniela Butano
 *
 */
public class PopulationInfo {
    private int size;
    private float geneLengthAverage;

    public PopulationInfo(int size, float geneLengthAverage) {
		super();
		this.size = size;
		this.geneLengthAverage = geneLengthAverage;
	}

	public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public float getGeneLengthAverage() {
        return geneLengthAverage;
    }

    public void setGeneLengthAverage(float geneLengthAverage) {
        this.geneLengthAverage = geneLengthAverage;
    }
}
