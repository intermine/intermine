package org.intermine.api.bag;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.bag.operations.BagOperation;
import org.intermine.api.bag.operations.BagOperationException;
import org.intermine.api.bag.operations.Intersection;
import org.intermine.api.bag.operations.RelativeComplement;
import org.intermine.api.bag.operations.SymmetricDifference;
import org.intermine.api.bag.operations.Union;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.types.ClassKeys;
import org.intermine.metadata.MetaDataException;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;

/**
 * Perform logical operations on bags - combine bags to create new InterMineBags
 * @author Richard Smith
 */
public final class BagOperations
{
//    private static final Logger LOG = Logger.getLogger(BagOperations.class);

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
     * Constant representing logical asymmetric substraction.
     */
    public static final String ASYMMETRIC_SUBTRACT = "ASYMMETRIC_SUBTRACT";

    /**
     * Create a bag that is the UNION of all the bags provided, if the union is the
     * empty set then don't create the new bag - if bags are of incompatible types or are all empty.
     * @param bags the bags to operate on
     * @param newBagName name of the new bag to create
     * @param profile the user that will own the new bag
     * @param model the data model
     * @param classKeys the class keys
     * @return the size of the new bag or 0 if no bag created
     * @throws BagOperationException if problems merging list
     * @throws MetaDataException if problems storing bag
     */
    public static int union(
            Model model, Collection<InterMineBag> bags, String newBagName,
            Profile profile, ClassKeys classKeys)
        throws BagOperationException, MetaDataException {
        BagOperation operation = new Union(model, profile, bags);
        return performBagOperation(operation, newBagName, classKeys);
    }

    /**
     * Create a bag that is the INTERSECTION of all the bags provided, if the intersection is the
     * empty set then don't create the new bag.
     * @param bags the bags to operate on
     * @param model the data model
     * @param classKeys the class keys
     * @param newBagName name of the new bag to create
     * @param profile the user that will own the new bag
     * @return the size of the new bag or 0 if no bag created
     * @throws BagOperationException if problems merging list
     * @throws MetaDataException if problems storing bag
     */
    public static int intersect(Model model, Collection<InterMineBag> bags, String newBagName,
            Profile profile, ClassKeys classKeys)
        throws BagOperationException, MetaDataException {
        BagOperation operation = new Intersection(model, profile, bags);
        return performBagOperation(operation, newBagName, classKeys);
    }

    /**
     * Create a bag that contains the union of the bags provided minus the intersection of those
     * bags.
     * @param bags the bags to operate on
     * @param newBagName name of the new bag to create
     * @param classKeys the class keys
     * @param model the data model
     * @param profile the user that will own the new bag
     * @return the size of the new bag or 0 if no bag created
     * @throws BagOperationException if problems merging list
     * @throws MetaDataException if problems storing bag
     */
    public static int subtract(Model model, Collection<InterMineBag> bags, String newBagName,
        Profile profile, ClassKeys classKeys)
        throws BagOperationException, MetaDataException {
        BagOperation operation = new SymmetricDifference(model, profile, bags);
        return performBagOperation(operation, newBagName, classKeys);
    }

    /**
     * @param include list of bags to include
     * @param exclude list of bags to exclude
     * @param model the data model
     * @param classKeys the class keys
     * @param newBagName name of the new bag to create
     * @param profile the user that will own the new bag
     * @return the size of the new bag or 0 if no bag created
     * @throws BagOperationException if problems merging list
     * @throws MetaDataException if problems storing bag
     */
    public static int asymmetricSubtract(Model model, Collection<InterMineBag> include,
        Collection<InterMineBag> exclude, String newBagName,
        Profile profile, ClassKeys classKeys)
        throws BagOperationException, MetaDataException {
        BagOperation op = new RelativeComplement(model, profile, include, exclude);
        return performBagOperation(op, newBagName, classKeys);
    }

    private static int performBagOperation(
        BagOperation operation, String newBagName, ClassKeys classKeys)
        throws BagOperationException {
        if (StringUtils.isNotBlank(newBagName)) {
            operation.setNewBagName(newBagName);
        }
        if (classKeys != null) {
            operation.setClassKeys(classKeys);
        }
        return performBagOperation(operation);
    }

    private static int performBagOperation(BagOperation operation)
        throws BagOperationException {
        InterMineBag newBag = operation.operate();
        try {
            return newBag.size();
        } catch (ObjectStoreException e) {
            // Really shouldn't happen.
            throw new BagOperationException("Could not read size");
        }
    }

}
