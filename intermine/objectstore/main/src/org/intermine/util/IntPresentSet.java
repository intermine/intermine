package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

/**
 * This is a set of ints. This class provides methods to insert an int, check for the presence of a
 * given int, and remove an int from the set. This class is designed to use as little RAM as
 * possible, and assumes that the ints are reasonably closely-spaced. In the case where the ints
 * are consecutive, this class will use not much more than an eighth of a byte per mapping.
 *
 * @author Matthew Wakeling
 */
public class IntPresentSet
{
    private static final int WORD_SIZE = 32; // Number of bits in a word
    private static final int WORD_MASK = WORD_SIZE - 1;
    private static final int PAGE_SIZE = 0x2000; // Number of words per page
    private static final int PAGE_MASK = PAGE_SIZE - 1;

    private Map pages = new HashMap();
    private int size = 0;

    /**
     * Constructor for this class. Creates an empty map.
     */
    public IntPresentSet() {
    }

    /**
     * Adds the given int to the set.
     *
     * @param i the int to add to the set
     * @param newBit true to add to the set, false to remove from the set
     */
    public void set(int i, boolean newBit) {
        int bitNo = i & WORD_MASK;
        i /= WORD_SIZE;
        int wordNo = i & PAGE_MASK;
        Integer pageNo = new Integer(i / PAGE_SIZE);
        int[] page = (int[]) pages.get(pageNo);
        if (page == null) {
            page = new int[PAGE_SIZE + 1];
            for (int o = 0; o <= PAGE_SIZE; o++) {
                page[o] = 0;
            }
            pages.put(pageNo, page);
        }
        int bitMask = 1 << bitNo;
        int word = page[wordNo];
        boolean oldBit = ((word & bitMask) != 0);
        if (oldBit != newBit) {
            if (newBit) {
                word = word | bitMask;
                size++;
                page[PAGE_SIZE]++;
            } else {
                word = word & (~bitMask);
                size--;
                page[PAGE_SIZE]--;
            }
            page[wordNo] = word;
            if (page[PAGE_SIZE] == 0) {
                pages.remove(pageNo);
            }
        }
    }

    /**
     * Returns whether the given int is present in this set.
     *
     * @param i any int
     * @return true or false
     */
    public boolean contains(int i) {
        int bitNo = i & WORD_MASK;
        i /= WORD_SIZE;
        int wordNo = i & PAGE_MASK;
        Integer pageNo = new Integer(i / PAGE_SIZE);
        int[] page = (int[]) pages.get(pageNo);
        if (page == null) {
            return false;
        }
        int bitMask = 1 << bitNo;
        int word = page[wordNo];
        return ((word & bitMask) != 0);
    }

    /**
     * Adds an int to the set.
     *
     * @param i an Integer
     */
    public void add(Integer i) {
        if (i == null) {
            throw new NullPointerException("i is null");
        }
        int iFrom = i.intValue();
        set(iFrom, true);
    }

    /**
     * Returns whether the given Integer is present in this set.
     *
     * @param i any Integer
     * @return true or false
     */
    public boolean contains(Integer i) {
        if (i == null) {
            throw new NullPointerException("i is null");
        }
        int iFrom = i.intValue();
        return contains(iFrom);
    }

    /**
     * Returns the number of ints present.
     *
     * @return the size
     */
    public int size() {
        return size;
    }

    /**
     * Removes all ints from the object.
     */
    public void clear() {
        pages.clear();
        size = 0;
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        StringBuffer retval = new StringBuffer("[");
        boolean needComma = false;
        TreeSet sortedKeys = new TreeSet(pages.keySet());
        Iterator keyIter = sortedKeys.iterator();
        while (keyIter.hasNext()) {
            Integer pageNo = (Integer) keyIter.next();
            int pageNoInt = pageNo.intValue();
            int[] page = (int[]) pages.get(pageNo);
            for (int wordNo = 0; wordNo < PAGE_SIZE; wordNo++) {
                int word = page[wordNo];
                if (word != 0) {
                    int bitMask = 1;
                    for (int bitNo = 0; bitNo < WORD_SIZE; bitNo++) {
                        if ((word & bitMask) != 0) {
                            if (needComma) {
                                retval.append(", ");
                            }
                            needComma = true;
                            retval.append(Integer.toString((pageNoInt * PAGE_SIZE + wordNo)
                                        * WORD_SIZE + bitNo));
                        }
                        bitMask <<= 1;
                    }
                }
            }
        }
        retval.append("]");
        return retval.toString();
    }
}

