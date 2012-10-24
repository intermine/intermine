package org.intermine.util;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

public class PasswordHasherTest extends TestCase
{
    public PasswordHasherTest(String arg1) {
        super(arg1);
    }

    public void test1() throws Exception {
        String hashed = PasswordHasher.hashPassword("flibble");
        assertTrue(PasswordHasher.checkPassword("flibble", hashed));
        assertFalse(PasswordHasher.checkPassword("flibble", "hhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh"));
        assertTrue(PasswordHasher.checkPassword("flibble", "flibble"));
        assertFalse(PasswordHasher.checkPassword("flibble", hashed + " "));
        assertFalse(PasswordHasher.checkPassword("flibble", "flobble"));
    }
}
