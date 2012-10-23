package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ObjectStoreBagCombination;
import org.intermine.objectstore.query.Query;

/**
 * Perform logical operations on bags - combine bags to create new InterMineBags
 * @author Richard Smith
 */
public final class BagOperations
{
    private BagOperations() {
        // don't
    }

    /**
     * Constant representing logical union.
     */
    public static final String UNION = "UNION";

    /**
     * Constant representing logical intersection.
     */
    public static final String INTERSECT = "INTERSECT";

    /**
     * Constant representing logical substraction.
     */
    public static final String SUBTRACT = "SUBTRACT";

    /**
     * Create a bag that is the UNION of all the bags provided, if the union is the
     * empty set then don't create the new bag - if bags are of incompatible types or are all empty.
     * @param bags the bags to operate on
     * @param newBagName name of the new bag to create
     * @param profile the user that will own the new bag
     * @return the size of the new bag or 0 if no bag created
     * @throws ObjectStoreException if problems storing bag
     */
    public static int union(Collection<InterMineBag> bags, String newBagName,
            Profile profile, Map<String, List<FieldDescriptor>> classKeys)
        throws ObjectStoreException {
        return performBagOperation(bags, newBagName, profile, ObjectStoreBagCombination.UNION,
                                  classKeys);
    }

    /**
     * Create a bag that is the INTERSECTION of all the bags provided, if the intersection is the
     * empty set then don't create the new bag.
     * @param bags the bags to operate on
     * @param newBagName name of the new bag to create
     * @param profile the user that will own the new bag
     * @return the size of the new bag or 0 if no bag created
     * @throws ObjectStoreException if problems storing bag
     */
    public static int intersect(Collection<InterMineBag> bags, String newBagName,
            Profile profile, Map<String, List<FieldDescriptor>> classKeys)
        throws ObjectStoreException {
        return performBagOperation(bags, newBagName, profile, ObjectStoreBagCombination.INTERSECT,
                                  classKeys);
    }

    /**
     * Create a bag that contains the union of the bags provided minus the intersection of those
     * bags.
     * @param bags the bags to operate on
     * @param newBagName name of the new bag to create
     * @param profile the user that will own the new bag
     * @return the size of the new bag or 0 if no bag created
     * @throws ObjectStoreException if problems storing bag
     */
    public static int subtract(Collection<InterMineBag> bags, String newBagName,
            Profile profile, Map<String, List<FieldDescriptor>> classKeys)
        throws ObjectStoreException {
        return performBagOperation(bags, newBagName, profile,
                ObjectStoreBagCombination.ALLBUTINTERSECT, classKeys);
    }

    private static int performBagOperation(Collection<InterMineBag> bags, String newBagName,
            Profile profile, int op, Map<String, List<FieldDescriptor>> classKeys)
        throws ObjectStoreException {
        String type = getCommonBagType(bags);
        if (type == null) {
            throw new IncompatibleTypesException("Given bags were of incompatible types.");
        }
        InterMineBag combined = null;
        try {
            combined = profile.createBag(newBagName, type, "", classKeys);
        } catch (UnknownBagTypeException e) {
            throw new RuntimeException(
                    "The type returned by getCommonBagType is not in the model", e);
        } catch (ClassKeysNotFoundException cke) {
            throw new RuntimeException("Bag has not class key set", cke);
        }
        ObjectStoreBagCombination osbc =
            new ObjectStoreBagCombination(op);
        for (InterMineBag bag : bags) {
            osbc.addBag(bag.getOsb());
        }
        Query q = new Query();
        q.addToSelect(osbc);

        combined.addToBagFromQuery(q);

        if (combined.size() == 0) {
            profile.deleteBag(combined.getName());
        }
        return combined.size();
    }


    /**
     * If all of the bags provided are of the same type return the type, otherwise return null.
     * This method does not take into account inheritance.
     * @param bags the bags to check
     * @return the common type or null if the bags are not all the same type
     */
    public static String getCommonBagType(Collection<InterMineBag> bags) {
        Set<String> types = new HashSet<String>();
        for (InterMineBag bag : bags) {
            types.add(bag.getType());
        }
        if (types.size() == 1) {
            return types.iterator().next();
        }
        return null;
    }
}
