package org.intermine.web.logic.bag;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.InterMineException;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.search.WebSearchable;
import org.intermine.web.logic.tagging.TagTypes;

/**
 * Helper methods for bags.
 *
 * @author Kim Rutherford
 */
public class BagHelper
{
    /** When generating new bag names, this is used as a prefix. */
    public static final String BAG_NAME_PREFIX = "bag";

    /**
     * Return a bag name that isn't currently in use.
     *
     * @param savedBags the Map of current saved bags
     * @param nameWanted the desired name
     * @return the new bag name
     */
    public static String findNewBagName(Map savedBags, String nameWanted) {
        if (!savedBags.containsKey(nameWanted)) {
            return nameWanted;
        }
        for (int i = 1;; i++) {
            String testName = nameWanted + "_" + i;
            if (savedBags == null || savedBags.get(testName) == null) {
                return testName;
            }
        }
    }

    /**
     * For a given InterMineObject and an InterMineIdBag return true if
     * the types correspond
     *
     * @param bag the InterMineIdBag
     * @param o the InterMineObject
     * @param os the ObjectStore
     * @return a boolean
     */
    public static boolean isOfBagType (InterMineBag bag, InterMineObject o, ObjectStore os) {
        Set classDescriptors = os.getModel().getClassDescriptorsForClass(o.getClass());
        for (Iterator iter = classDescriptors.iterator(); iter.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            String className = cld.getName();
            if (TypeUtil.unqualifiedName(className).equals(bag.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * For a given bag name, return the bag whether it's in the profile or
     * is a shared bag
     *
     * @param profile the user profile
     * @param searchRepository the SearchRepository
     * @param bagName the bag name
     * @return the InterMineBag
     * @throws InterMineException if the bag is not found
     */
    public static InterMineBag getBag(Profile profile, SearchRepository searchRepository,
                                      String bagName) throws InterMineException {
        InterMineBag imBag = null;
        if (profile != null && profile.getSavedBags() != null) {
            imBag = profile.getSavedBags().get(bagName);
        }
        if (imBag == null) {
            Map<String, ? extends WebSearchable> publicBagMap = searchRepository
                                                               .getWebSearchableMap(TagTypes.BAG);
            if (publicBagMap.get(bagName) != null) {
                imBag = (InterMineBag) publicBagMap.get(bagName);
            }
        }
        if (imBag == null) {
            throw new InterMineException("Bag not found with name:" + bagName);
        }
        return imBag;
    }
}