package org.flymine.web.widget;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;

import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 * @author Xavier Watkins
 *
 */
public class FlyAtlasToolTipGenerator implements CategoryToolTipGenerator
{
    Object [] geneMap;
    
    /**
     * Constructs a FlyaAtlasToolTipGenerator
     * @param geneMap the map of genes
     */
    public FlyAtlasToolTipGenerator(Object[] geneMap) {
        super();
        this.geneMap = geneMap;
    }

    /**
     * {@inheritDoc}
     * @see org.jfree.chart.labels.CategoryToolTipGenerator#generateToolTip(
     *      org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateToolTip(@SuppressWarnings("unused") CategoryDataset dataset, 
                                  int series, int category) {
        ArrayList geneList = (ArrayList) ((Object[]) geneMap[category])[series];
        StringBuffer sb = new StringBuffer();
        Iterator iter = geneList.iterator();
        int count = 0;
        while (iter.hasNext() && count <= 5) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append((String) iter.next());
            count++;
        }
        if (count > 5) {
            sb.append("...");
        }
        sb.append("\nClick on the column to view.");
        return sb.toString();
    }
    
    

}
