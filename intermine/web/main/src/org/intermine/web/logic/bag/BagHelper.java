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

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.InterMineException;
import org.intermine.metadata.ClassDescriptor;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.query.MainHelper;
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

    /**
     * Create a bag for the given profile and bag name from a PathQuery.  The PathQuery must
     * select only the id field from the type the bag is to be created from.  The name will be
     * made unique with "_n" if it already exists in the profile.
     * @param os the production ObjectStore
     * @param bagQueryRunner the bag query runner
     * @param profile bag will be created in this profile
     * @param bagName name of new bag
     * @param bagDescription a description for the new bag
     * @param bagType the class of object in the bag
     * @param pathQuery the query to create the bag from
     * @return the new bag, already saved
     * @throws ObjectStoreException if persistence problem
     */
    public static InterMineBag createBagFromPathQuery(PathQuery pathQuery, String bagName,
            String bagDescription, String bagType, Profile profile, ObjectStore os,
            BagQueryRunner bagQueryRunner) throws ObjectStoreException {
        if (pathQuery.getView().size() != 1) {
            throw new RuntimeException("Can only create bags from a PathQuery that selects just "
                    + "id");
        }
        if (!pathQuery.getView().get(0).getLastElement().equals("id")) {
            throw new RuntimeException("Can only create bags from a PathQuery that selects just "
                    + "id");
        }
        ObjectStoreWriterInterMineImpl osw = new ObjectStoreWriterInterMineImpl(os);
        Query q = MainHelper.makeQuery(pathQuery, null, null, bagQueryRunner, null, false);

        InterMineBag bag = new InterMineBag(bagName, bagType, bagDescription, new Date(), os,
                profile.getUserId(), profile.getProfileManager().getProfileObjectStoreWriter());
        osw.addToBagFromQuery(bag.getOsb(), q);
        profile.saveBag(bag.getName(), bag);
        return bag;
    }
}
