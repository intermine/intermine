package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.Map;

/**
 * Helper methods for bags.
 *
 * @author Kim Rutherford
 */

public class BagHelper
{
    /**
     * Save a new Collection with the given name.  If a bag exists with the given name the contents
     * of newBag will be appended to the saved bag.
     * @param newBag the bag contents to save
     * @param bagName the bag to save to
     * @param savedBags the current Map of saved bags
     */
    public static void saveBag(Collection newBag, String bagName, Map savedBags) {
        Collection bag = (Collection) savedBags.get(bagName);
        if (bag == null) {
            savedBags.put(bagName, new InterMineBag(newBag));
        } else {
            bag.addAll(newBag);
        }
    }
}
