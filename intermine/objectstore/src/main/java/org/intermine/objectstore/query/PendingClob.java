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

import org.intermine.objectstore.ObjectStore;

/**
 * Subclass of ClobAccess that contains data to be written to the database instead of access to data
 * already in the database.
 *
 * @author Matthew Wakeling
 */
public class PendingClob extends ClobAccess
{
    String text;

    /**
     * Construct a PendingClob from a String.
     *
     * @param text the String
     */
    public PendingClob(String text) {
        super();
        this.text = text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clob getClob() {
        throw new IllegalStateException("Clob is pending write");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOffset() {
        throw new IllegalStateException("Clob is pending write");
    }

    /**
     * Return a character from the specified index.
     *
     * @param index the position from which to return a character
     * @return a character
     * @throws IndexOutOfBoundsException if the index argument is negative or not less than length()
     */
    @Override
    public char charAt(int index) {
        return text.charAt(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int length() {
        return text.length();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PendingClob subSequence(int start, int end) {
        return new PendingClob(text.subSequence(start, end).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectStore getObjectStore() {
        throw new IllegalStateException("Clob is pending write");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDbDescription() {
        throw new IllegalStateException("Clob is pending write");
    }
}
