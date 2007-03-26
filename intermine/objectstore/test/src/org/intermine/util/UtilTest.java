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

import junit.framework.TestCase;

public class UtilTest extends TestCase
{
    public UtilTest(String arg) {
        super(arg);
    }

    public void testEquals() {
        assertTrue(org.intermine.util.Util.equals(null, null));
        assertTrue(org.intermine.util.Util.equals(new Integer(10), new Integer(10)));
        assertTrue(org.intermine.util.Util.equals(new Integer(20), new Integer(10)) == false);
        assertTrue(org.intermine.util.Util.equals(null, new Integer(10)) == false);
        assertTrue(org.intermine.util.Util.equals(new Integer(10), null) == false);
    }

    public void testHashCode() {
        assertTrue(org.intermine.util.Util.hashCode(null) == 0);
        assertTrue(org.intermine.util.Util.hashCode(new Integer(10)) != 0);
    }

}
