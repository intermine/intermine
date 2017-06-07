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

/**
 * Object representing a large String that has been stored in the database. This object can be used
 * to retrieve the String in bits or as a stream, and to alter the object.
 *
 * @author Matthew Wakeling
 */
public class Clob implements QuerySelectable
{
    /** Page size for clob data */
    public static final int CLOB_PAGE_SIZE = 7000;

    private final int clobId;

    /**
     * Constructs a new Clob. This method should only be called from an ObjectStore which can
     * provide a suitable valid clobId. Once the Clob has been created, content can be added to it
     * through the ObjectStoreWriter.
     *
     * @param clobId the identifier of the Clob
     */
    public Clob(int clobId) {
        this.clobId = clobId;
    }

    /**
     * Returns the identifier of the Clob. This number will probably only be of use to the internals
     * of an ObjectStore.
     *
     * @return an int
     */
    public int getClobId() {
        return clobId;
    }

    /**
     * {@inheritDoc}
     */
    public Class<String> getType() {
        return String.class;
    }

    /**
     * Override Object#equals. Note that this means that Clob objects for different objectstores
     * with the same ID will be counted as equals. Make sure you don't put Clobs from different
     * objectstores in the same collection.
     *
     * @param o an Object
     * @return true if this equals o
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Clob) {
            return clobId == ((Clob) o).clobId;
        }
        return false;
    }

    /**
     * Override Object#hashCode. See note in equals.
     *
     * @return an int representing the contents
     */
    @Override
    public int hashCode() {
        return clobId;
    }
}
