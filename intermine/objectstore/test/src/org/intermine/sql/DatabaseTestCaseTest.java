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

import junit.framework.AssertionFailedError;

import java.sql.Types;

import com.mockobjects.sql.MockSingleRowResultSet;
import com.mockobjects.sql.MockMultiRowResultSet;
import com.mockobjects.sql.MockResultSetMetaData;

public class DatabaseTestCaseTest extends DatabaseTestCase
{
    public DatabaseTestCaseTest(String arg1) {
        super(arg1);
    }

    protected Database getDatabase() throws Exception {
        return DatabaseFactory.getDatabase("db.unittest");
    }

    private boolean failed;
    private MockSingleRowResultSet mrs1, mrs2, mrs3, mrs4;
    private MockMultiRowResultSet mrs11, mrs12, mrs13, mrs14;

    public void setUp() throws Exception {
        super.setUp();
        failed = false;
        MockResultSetMetaData mrsmd1 = new MockResultSetMetaData();
        MockResultSetMetaData mrsmd3 = new MockResultSetMetaData();
        MockResultSetMetaData mrsmd2 = new MockResultSetMetaData();

        mrsmd1.setupAddColumnNames(new String[] {"field1", "field2", "field3", "field4"});
        mrsmd1.setupAddColumnTypes(new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
        mrsmd1.setupGetColumnCount(4);
        mrsmd2.setupAddColumnNames(new String[] {"field1", "field2", "field3", "field4"});
        mrsmd2.setupAddColumnTypes(new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
        mrsmd2.setupGetColumnCount(4);
        mrsmd3.setupAddColumnNames(new String[] {"field1", "field2", "field3", "field5"});
        mrsmd3.setupAddColumnTypes(new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
        mrsmd3.setupGetColumnCount(4);

        mrs1 = new MockSingleRowResultSet();
        mrs1.setupMetaData(mrsmd1);
        mrs1.addExpectedIndexedValues(new Object[] { "test1", "test2", "test3", "test4" });

        mrs2 = new MockSingleRowResultSet();
        mrs2.setupMetaData(mrsmd2);
        mrs2.addExpectedIndexedValues(new Object[] { "test1", "test2", "test3", "test4" });

        mrs3 = new MockSingleRowResultSet();
        mrs3.setupMetaData(mrsmd2);
        mrs3.addExpectedIndexedValues(new Object[] { "test1", "test2", "test3", "test5" });

        mrs4 = new MockSingleRowResultSet();
        mrs4.setupMetaData(mrsmd3);
        mrs4.addExpectedIndexedValues(new Object[] { "test1", "test2", "test3", "test4" });

        mrs11 = new MockMultiRowResultSet();
        mrs11.setupMetaData(mrsmd1);
        mrs11.setupRows(new Object[][] { new Object [] {"test1", "test2", "test3", "test4"},
                                         new Object [] {"test5", "test6", "test7", "test8"}});
        mrs12 = new MockMultiRowResultSet();
        mrs12.setupMetaData(mrsmd2);
        mrs12.setupRows(new Object[][] { new Object [] {"test1", "test2", "test3", "test4"},
                                         new Object [] {"test5", "test6", "test7", "test8"}});

        mrs13 = new MockMultiRowResultSet();
        mrs13.setupMetaData(mrsmd2);
        mrs13.setupRows(new Object[][] { new Object [] {"test1", "test2", "test3", "test4"},
                                         new Object [] {"test5", "TEST6", "test7", "test8"}});

        mrs14 = new MockMultiRowResultSet();
        mrs14.setupMetaData(mrsmd3);
        mrs14.setupRows(new Object[][] { new Object [] {"test1", "test2", "test3", "test4"},
                                        new Object [] {"test5", "test6", "test7", "test8"}});
    }

    public void testEqualsSingleRow() throws Exception {
        assertEquals(mrs1, mrs2);
    }

    public void testEqualsMultiRows() throws Exception {
        assertEquals(mrs11, mrs12);
    }

    public void testEqualsSingleRowDifferentData() throws Exception {
        try {
            assertEquals(mrs1, mrs3);
            failed = true;
        }
        catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("mrs1 and mrs3 should not be equal");
            }
        }
    }

    public void testEqualsSingleRowDifferentColumns() throws Exception {
        try {
            assertEquals(mrs1, mrs4);
            failed = true;
        }
        catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("mrs1 and mrs4 should not be equal");
            }
        }
    }

    public void testEqualsMultiRowDifferentData() throws Exception {
        try {
            assertEquals(mrs11, mrs13);
            failed=true;
        }
        catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("mrs11 and mrs13 should not be equal");
            }
        }
    }

    public void testEqualsMultiRowDifferentColumns() throws Exception {
        try {
            assertEquals(mrs11, mrs14);
            failed = true;
        }
        catch (AssertionFailedError e) {
        }
        finally {
            if (failed) {
                fail("mrs11 and mrs14 should not be equal");
            }
        }
    }

}
