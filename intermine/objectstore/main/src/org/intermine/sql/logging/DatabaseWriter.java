package org.intermine.sql.logging;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.Writer;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.intermine.util.StringUtil;

/**
 * Writes tab-separated Strings to a database table
 *
 * @author Andrew Varley
 */
public class DatabaseWriter extends Writer
{
    protected Connection con;
    protected String table;
    protected StringBuffer sb = new StringBuffer();
    protected int fields = 0;

    /**
     * Construct an empty DatabaseWriter for testing purposes
     *
     */
    public DatabaseWriter() {
    }

   /**
     * Construct a DatabaseWriter
     *
     * @param con a database Connection
     * @param table the table to write to
     * @throws NullPointerException if either con or table are null
     */
    public DatabaseWriter(Connection con, String table) {
        if (con == null) {
            throw new NullPointerException("Connection cannot be null");
        }
        if (table == null) {
            throw new NullPointerException("Database table cannot be null");
        }
        this.con = con;
        this.table = table;
    }

    /**
     * Write a portion of an array to the database
     *
     * @param cbuff the array of characters
     * @param off the start point
     * @param len the number of characters to write
     * @throws IOException if an error occurs when writing to the underlying database
     */
    public void write(char[] cbuff, int off, int len) throws IOException {
        if (cbuff == null) {
            return;
        }
        sb.append(cbuff, off, len);

        List rows = new ArrayList();
        int index = -1;

        while ((index = sb.indexOf(System.getProperty("line.separator"))) != -1) {
            rows.add(sb.substring(0, index));
            if (index < sb.length()) {
                sb = new StringBuffer(sb.substring(index + 1));
            }
        }
        writeRows(rows);
    }

    /**
     * Flush completed rows to the database
     */
    public void flush() {
    }

    /**
     * Close this Writer
     */
    public void close() {
    }


    /**
     * Creates an SQL String suitable for initialising a PreparedStatement
     *
     * @param table the table to insert in
     * @param row an example row
     * @return the SQL String
     * @throws NullPointerException if any arguments are null
     */
    protected String createSQLStatement(String table, String row) {
        if ((table == null) || (row == null)) {
            throw new NullPointerException("Arguments to createSQLStatement must not be null");
        }

        fields = StringUtil.countOccurances("\t", row) + 1;

        StringBuffer sql = new StringBuffer();

        sql.append("INSERT INTO ")
            .append(table)
            .append(" VALUES(");
        for (int i = 0; i < fields; i++) {
            sql.append("?");
            if (i != (fields - 1)) {
                sql.append(", ");
            }
        }
        sql.append(")");

        return sql.toString();
    }

    /**
     * Actually do the writing of the rows to the database
     *
     * @param rows a List of rows to write
     * @throws IOException if an error occurs within the database
     */
    private void writeRows(List rows) throws IOException {
        boolean autoCommit = false;;
        PreparedStatement pstmt;

        if (rows.size() == 0) {
            return;
        }

        try {

            String row = (String) rows.get(0);

            autoCommit = con.getAutoCommit();
            pstmt = con.prepareStatement(createSQLStatement(table, (String) rows.get(0)));

            Iterator i = rows.iterator();
            while (i.hasNext()) {
                row = (String) i.next();
                StringTokenizer st = new StringTokenizer(row, "\t", false);
                int j = 1;
                while (st.hasMoreTokens()) {
                    pstmt.setString(j++, st.nextToken().trim());
                }
                if (j <= fields) {
                    throw new IOException("Too few fields");
                }

                pstmt.addBatch();
            }

            int [] updateCounts = pstmt.executeBatch();
            con.commit();
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        } finally {
            try {
                con.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                // Do nothing
            }
        }
    }


}
