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

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * A CharSequence object representing a String constructed out of a sequence of other Strings.
 * This object does not copy any of the string data - rather, it stores pointers to the original
 * Strings it was constructed with.
 */
public class StringConstructor implements CharSequence
{
    private TreeMap<Integer, String> strings = new TreeMap();
    int length = 0;

    /**
     * Main constructor - creates an empty object.
     */
    public StringConstructor() {
    }

    public void append(String string) {
        strings.put(new Integer(length), string);
        length += string.length();
    }

    public int length() {
        return length;
    }

    public String toString() {
        StringBuffer retval = new StringBuffer();
        for (String string : strings.values()) {
            retval.append(string);
        }
        return retval.toString();
    }

    public char charAt(int index) {
        // If we were using Java 1.6, I could do floorEntry(). Instead, I have to do:
        String component = strings.get(new Integer(index));
        int componentIndex = index;
        if (component == null) {
            Integer key = strings.headMap(new Integer(index)).lastKey();
            if (key != null) {
                component = strings.get(key);
                componentIndex = key.intValue();
            }
        }
        return component.charAt(index - componentIndex);
    }

    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException("We probably don't need this method");
    }

    public Collection<String> getStrings() {
        return strings.values();
    }
}
