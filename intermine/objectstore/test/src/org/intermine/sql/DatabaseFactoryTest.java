package org.intermine.sql;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.util.Properties;

public class DatabaseFactoryTest extends TestCase
{
    Properties props;

    public DatabaseFactoryTest(String arg1) {
        super(arg1);
    }


    public void testConfigure() throws Exception {

        Database db1 = DatabaseFactory.getDatabase("db.unittest");
        Database db2 = DatabaseFactory.getDatabase("db.unittest");

        // These should be exactly the same object
        assertTrue(db1 == db2);
    }

    public void testNullName() throws Exception {
        try {
            DatabaseFactory.getDatabase(null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

}
