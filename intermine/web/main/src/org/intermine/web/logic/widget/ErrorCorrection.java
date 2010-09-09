package org.intermine.web.logic.widget;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.math.BigDecimal;
import java.util.HashMap;

/**
 *
 * @author Julie Sullivan
 */
public interface ErrorCorrection
{
    /**
     * @param maxValue maximum value to display
     */
    void calculate(Double maxValue);

    /**
     * @return adjusted map
     */
    HashMap<String, BigDecimal> getAdjustedMap();
}
