package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.ArrayList;

import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.data.category.CategoryDataset;
/**
 *
 * @author Julie Sullivan
 */
public class ToolTipGenerator implements CategoryToolTipGenerator
{
    Object [] geneMap;

    /**
     * Constructs a ToolTipGenerator
     * @param geneMap the map of genes
     */
    public ToolTipGenerator(Object[] geneMap) {
        super();
        this.geneMap = geneMap;
    }

    /**
     * {@inheritDoc}
     * @see org.jfree.chart.labels.CategoryToolTipGenerator#generateToolTip(
     *      org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateToolTip(@SuppressWarnings("unused") CategoryDataset dataset, int series,
                                  int category) {
        ArrayList geneList = (ArrayList) ((Object[]) geneMap[category])[series];
        // charts may have results but no tool tip, ie "expected" results
        if (geneList == null) {
            return null;
        }
        return "Click here to view all " + geneList.size() + " objects";
    }
}
