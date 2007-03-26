package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Object representing a collection of objects which have been stored in the database. This bag
 * can be used in BagConstraints and QueryClassBag objects in queries.
 *
 * @author Matthew Wakeling
 */
public class ObjectStoreBag implements QuerySelectable
{
    private final int bagId;

    /**
     * Constructs a new ObjectStoreBag. This method should only be called from an ObjectStore
     * which can provide a suitable valid bagId. Once the bag has been created, elements can
     * be added to it through the ObjectStoreWriter.
     *
     * @param bagId the identifier of the bag.
     */
    public ObjectStoreBag(int bagId) {
        this.bagId = bagId;
    }

    /**
     * Returns the identifier of the bag. This number will probably only be of use to the internals
     * of an ObjectStore.
     *
     * @return an int
     */
    public int getBagId() {
        return bagId;
    }

    /**
     * @see QuerySelectable#getType
     */
    public Class getType() {
        return Integer.class;
    }

    /**
     * Override Object#equals. Note that this means that ObjectStoreBag objects for different
     * objectstores with the same ID will be counted as equals. Make sure you don't put
     * ObjectStoreBags from different objectstores in the same collection.
     *
     * @param o an Object
     * @return true if this equals o
     */
    public boolean equals(Object o) {
        if (o instanceof ObjectStoreBag) {
            return bagId == ((ObjectStoreBag) o).bagId;
        }
        return false;
    }

    /**
     * Override Object#hashCode. See note in equals.
     *
     * @return an int representing the contents
     */
    public int hashCode() {
        return bagId;
    }
}
