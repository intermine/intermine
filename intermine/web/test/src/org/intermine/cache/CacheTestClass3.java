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

public class CacheTestClass3 implements Serializable
{
    Integer intArg;
    String stringArg;
    Float floatArg;

    public CacheTestClass3 (Integer intArg, String stringArg, Float floatArg) {
        this.intArg = intArg;
        this.stringArg = stringArg;
        this.floatArg = floatArg;
    }

    public boolean equals(Object other) {
        if (other instanceof CacheTestClass3) {
            return intArg.equals(((CacheTestClass3) other).intArg) 
                && stringArg.equals(((CacheTestClass3) other).stringArg)
                && floatArg.equals(((CacheTestClass3) other).floatArg);
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return stringArg.hashCode() ^ intArg.intValue();
    }
}
