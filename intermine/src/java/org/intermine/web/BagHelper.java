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
import java.util.LinkedHashMap;

import javax.servlet.http.HttpSession;

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
        int i = 1;

        while (true) {
            String testName = BAG_NAME_PREFIX + i;

            if (savedBags == null || savedBags.get(testName) == null) {
                return testName;
            }

            i++;
        }
    }

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

    /**
     * Get the SAVED_BAGS attribute from the session, creating it if necessary.
     * @param session the session to get the saved bags from
     * @return Map the saved bags Map
     */
    public static Map getSavedBags(HttpSession session) {
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);

        if (savedBags == null) {
            savedBags = new LinkedHashMap();
            session.setAttribute(Constants.SAVED_BAGS, savedBags);
        }

        return savedBags;
    }
}
