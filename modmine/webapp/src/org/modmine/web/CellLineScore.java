package org.modmine.web;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * A Java class to store cellline score info
 */
public class CellLineScore
{
    // the cell line name
    private String cellLine;
    // the object id of the stored Item
    private Double score;
    
    public CellLineScore(String x, Double y) {
        cellLine = x;
        score = y;
    }  
    
    /**
     * @return the experimentType
     */
    public Double getScore() {
        return score;
    }
    
    /**
     * @return the experimentType
     */
    public String getCellLine() {
        return cellLine;
    }
    
    
}



