package org.intermine.objectstore.query;

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

/**
 * Common interface for BagConstraint and MultipleInBagConstraint defining the getBag() method.
 *
 * @author Matthew Wakeling
 */
public interface ConstraintWithBag
{
    /**
     * Returns the bag that the constraint is using.
     *
     * @return a Collection
     */
    Collection<?> getBag();
}
