package org.flymine.util;

/**
 * Generic utility functions.
 *
 * @author Matthew Wakeling
 */
public class Util
{
    /**
     * Compare two objects, using their .equals method, but comparing null to null as equal.
     *
     * @param a one Object
     * @param b another Object
     * @return true if they are equal or both null
     */
    public static boolean equals(Object a, Object b) {
        if (a == null) {
            return (b == null);
        } else {
            return a.equals(b);
        }
    }
}
