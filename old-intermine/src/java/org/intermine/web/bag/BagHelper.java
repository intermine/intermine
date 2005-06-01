package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

/**
 * Helper methods for bags.
 *
 * @author Kim Rutherford
 */
public class BagHelper
{
    private static final String BAG_NAME_PREFIX = "bag_";

    /**
     * Return a bag name that isn't currently in use.
     *
     * @param savedBags the Map of current saved bags
     * @return the new bag name
     */
    public static String findNewBagName(Map savedBags) {
        for (int i = 1;; i++) {
            String testName = BAG_NAME_PREFIX + i;
            if (savedBags == null || savedBags.get(testName) == null) {
                return testName;
            }
        }
    }
}