package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;

import org.intermine.model.fulldata.Item;

/**
 * Provides an object that describes a constraint that is part of a path for the
 * ObjectStoreItemPathFollowingImpl to follow.
 *
 * @author Matthew Wakeling
 */
public interface ItemPrefetchConstraint
{
    /**
     * Returns a FieldNameAndValue object that describes this constraint with respect to a
     * particular Item.
     *
     * @param item the Item
     * @return a FieldNameAndValue object
     */
    public FieldNameAndValue getConstraint(Item item);

    /**
     * Returns a Set of FieldNameAndValue objects that describe this constraint with respect to a
     * particular target Item. Sometimes the result of this may seemingly contain infinite entries,
     * in which case only the ones which are deemed to be of use are returned.
     *
     * @param item the Item
     * @return a Set of FieldNameAndValue objects
     */
    public Set getConstraintFromTarget(Item item);


    /**
     * Perform a deep clone on this object.
     * @return a deep cloned ItemPrefetchConstraint
     */
    public ItemPrefetchConstraint deepClone();
}
