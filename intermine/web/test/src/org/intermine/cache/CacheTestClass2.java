package org.intermine.cache;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Serializable;

/**
 * A class to use when testing InterMineCache.
 *
 * @author Kim Rutherford
 */

public class CacheTestClass2 implements Serializable
{
    Integer intArg;
    String stringArg;

    public CacheTestClass2 (Integer intArg, String stringArg) {
        this.intArg = intArg;
        this.stringArg = stringArg;
    }

    public boolean equals(Object other) {
        if (other instanceof CacheTestClass2) {
            return intArg.equals(((CacheTestClass2) other).intArg) 
                && stringArg.equals(((CacheTestClass2) other).stringArg);
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return stringArg.hashCode() ^ intArg.intValue();
    }
}
