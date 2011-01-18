package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.data.category.CategoryDataset;

/**
 *
 * @author Julie Sullivan
 */
public class ToolTipGenerator implements CategoryToolTipGenerator
{
    /**
     * Constructs a ToolTipGenerator
     */
    public ToolTipGenerator() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public String generateToolTip(@SuppressWarnings("unused") CategoryDataset dataset,
            @SuppressWarnings("unused") int series, @SuppressWarnings("unused") int category) {
        return "Click here to view all objects";
    }
}
