package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2013 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.query.ObjectStoreBagCombination;
import org.intermine.objectstore.query.Query;

/**
 * Perform logical operations on bags - combine bags to create new InterMineBags
 * @author Richard Smith
 */
public final class BagOperations
{
    private static final Logger LOG = Logger.getLogger(BagOperations.class);

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
    public static int union(
            Model model, Collection<InterMineBag> bags, String newBagName,
            Profile profile, Map<String, List<FieldDescriptor>> classKeys)
        throws BagOperationException, MetaDataException {
        BagOperation operation = new UnionOperation(model, bags, profile);
        return performBagOperation(operation, newBagName, classKeys);
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
    public static int intersect(Model model, Collection<InterMineBag> bags, String newBagName,
            Profile profile, Map<String, List<FieldDescriptor>> classKeys)
        throws BagOperationException, MetaDataException {
        BagOperation operation = new IntersectionOperation(model, bags, profile);
        return performBagOperation(operation, newBagName, classKeys);
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
    public static int subtract(Model model, Collection<InterMineBag> bags, String newBagName,
        Profile profile, Map<String, List<FieldDescriptor>> classKeys)
        throws BagOperationException, MetaDataException {
        BagOperation operation = new SymmetricDifferenceOperation(model, bags, profile);
        return performBagOperation(operation, newBagName, classKeys);
    }

    public static int asymmetricSubtract(
        Model model,
        Collection<InterMineBag> include,
        Collection<InterMineBag> exclude,
        String newBagName,
        Profile profile, Map<String, List<FieldDescriptor>> classKeys)
        throws BagOperationException, MetaDataException {

        BagOperation leftUnion = new UnionOperation(model, include, profile);
        BagOperation rightUnion = new UnionOperation(model, include, profile);
        if (classKeys != null) {
            leftUnion.setClassKeys(classKeys);
            rightUnion.setClassKeys(classKeys);
        }

        InterMineBag left = null;

        try {
            left = leftUnion.operate();

            BagOperation mainOp= new SubtractionOperation(model, left, exclude, profile);
            return performBagOperation(mainOp, newBagName, classKeys);
        } finally {
            removeTemporaryBag(profile, left);
        }
    }

    private static void removeTemporaryBag(Profile p, InterMineBag bag) {
        if (bag != null) {
            try {
                p.deleteBag(bag.getName());
            } catch (ObjectStoreException e) {
                LOG.warn("Error deleting temporary bag", e);
            }
        }
    }

    private static int performBagOperation(
        BagOperation operation, String newBagName, Map<String, List<FieldDescriptor>> classKeys)
        throws BagOperationException, MetaDataException {
        if (StringUtils.isNotBlank(newBagName)) operation.setNewBagName(newBagName);
        if (classKeys != null) operation.setClassKeys(classKeys);
        return performBagOperation(operation);
    }

    private static int performBagOperation(BagOperation operation)
        throws BagOperationException, MetaDataException {
        InterMineBag newBag = operation.operate();
        try {
            return newBag.size();
        } catch (ObjectStoreException e) {
            // Really shouldn't happen.
            throw new BagOperationException("Could not read size");
        }
    }

}
