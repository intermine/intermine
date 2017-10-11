package org.intermine.bio.chado;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mockobjects.sql.MockMultiRowResultSet;

/**
 * Test data for ChadoCVFactoryTest
 * @author Kim Rutherford
 */
public class TestChadoCVFactory extends ChadoCVFactory
{
    public TestChadoCVFactory() {
        super(null);
    }

    @Override
    protected ResultSet getCVTermRelationshipResultSet(Connection connection, String cvName)
        throws SQLException {
        String[] columnNames = new String[] {
            "subject_id", "object_id",  "rel_type.name"
        };
        Object[][] resObjects = new Object[][] {
            {
                1002, 1001, "isa"
            },
            {
                1003, 1002, "isa"
            },
            {
                1004, 1003, "isa"
            },
            {
                1005, 1001, "isa"
            },
            {
                1005, 1003, "isa"
            },
            {
                1005, 1000, "isa"
            },
            {
                1005, 1006, "isa"
            }
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }

    @Override
    protected ResultSet getCVTermResultSet(Connection connection, String cvName)
        throws SQLException {
        String[] columnNames = new String[] {
            "cvterm_id", "cvterm_name"
        };
        Object[][] resObjects = new Object[][] {
            {
                1000, "root1"
            },
            {
                1001, "root2"
            },
            {
                1002, "child1"
            },
            {
                1003, "child2"
            },
            {
                1004, "child3"
            },
            {
                1005, "child4"
            },
            {
                1006, "root3"
            },
        };

        MockMultiRowResultSet res = new MockMultiRowResultSet();
        res.setupRows(resObjects);
        res.setupColumnNames(columnNames);
        return res;
    }
}
