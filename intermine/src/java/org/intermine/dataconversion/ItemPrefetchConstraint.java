package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.flymine.model.fulldata.Item;

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
}
