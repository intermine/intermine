package org.intermine.sql.logging;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;
import org.intermine.sql.DatabaseFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseWriterSQLStatementsTest extends TestCase
{
    DatabaseWriter writer;

    public DatabaseWriterSQLStatementsTest(String arg1) {
        super(arg1);
    }

    public void testSQLStatement() throws Exception {
        writer = new DatabaseWriter();

        assertEquals("INSERT INTO table VALUES(?)", writer.createSQLStatement("table", "value1"));
        assertEquals("INSERT INTO table VALUES(?, ?)", writer.createSQLStatement("table", "value1\tvalue2"));
        assertEquals("INSERT INTO table VALUES(?, ?, ?)", writer.createSQLStatement("table", "value1\t\tvalue3"));
    }

    public void testSQLStatementWithNullTable() throws Exception {
        writer = new DatabaseWriter();
        try {
            writer.createSQLStatement(null, "value1");
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testSQLStatementWithNullRow() throws Exception {
        writer = new DatabaseWriter();
        try {
            writer.createSQLStatement("table", null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }

    public void testWriteNull() throws Exception {
        writer = new DatabaseWriter();
        try {
            writer.write((String) null);
            fail("Expected: NullPointerException");
        }
        catch (NullPointerException e) {
        }
    }
}
