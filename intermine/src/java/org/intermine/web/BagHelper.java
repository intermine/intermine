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
import javax.servlet.http.HttpServletRequest;

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
     * @param request The HTTP request we are processing
     * @param bagName the bag to save to
     * @param newBag the bag contents to save
     */
    public static void saveBag(HttpServletRequest request, String bagName, Collection newBag) {
        HttpSession session = request.getSession();
        
        Map savedBags = (Map) session.getAttribute(Constants.SAVED_BAGS);
        if (savedBags == null) {
            savedBags = new LinkedHashMap();
            session.setAttribute(Constants.SAVED_BAGS, savedBags);
        }
        
        Collection bag = (Collection) savedBags.get(bagName);

        if (bag == null) {
            bag = new InterMineBag(newBag);
            savedBags.put(bagName, bag);
        } else {
            bag.addAll(newBag);
        }
    }
}
