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

import java.util.Collection;
import java.util.Set;

import org.intermine.api.bag.InterMineBag;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.util.TypeUtil;

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
     * @param bagNames the existing bagNames
     * @param nameWanted the desired name
     * @return the new bag name
     */
    public static String findNewBagName(Collection<String> bagNames, String nameWanted) {
        if (!bagNames.contains(nameWanted)) {
            return nameWanted;
        }
        for (int i = 1;; i++) {
            String testName = nameWanted + "_" + i;
            if (bagNames == null || bagNames.contains(testName)) {
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
     * @param model the model
     * @return a boolean
     */
    public static boolean isOfBagType (InterMineBag bag, InterMineObject o, Model model) {
        Set<ClassDescriptor> classDescriptors = model.getClassDescriptorsForClass(o.getClass());
        for (ClassDescriptor cld: classDescriptors) {
            String className = cld.getName();
            if (TypeUtil.unqualifiedName(className).equals(bag.getType())) {
                return true;
            }
        }
        return false;
    }
}