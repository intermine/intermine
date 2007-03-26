package org.intermine.objectstore.query;

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

import org.intermine.model.testmodel.*;

public class QueryCollectionReferenceTest extends TestCase
{
    private QueryClass qc;

    public QueryCollectionReferenceTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qc = new QueryClass(Department.class);
    }

    public void testMissingField() {
        try {
            new QueryCollectionReference(qc, "secretarys");
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testEmptyField() {
        try {
            new QueryCollectionReference(qc, "");
            fail("An IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testNullField() throws Exception {
        try {
            new QueryCollectionReference(qc, (String)null);
            fail("A NullPointerException should have been thrown");
        } catch (NullPointerException e) {
        }
    }

    public void testInvalidField() {
        try {
            new QueryCollectionReference(qc, "name");
            fail("An IllegalArgumentException should have been thrown");
        } catch (Exception e) {
        }
    }

    public void testValidField() {
        try {
            new QueryCollectionReference(qc, "employees");
        } catch (Exception e) {
            fail("An exception should not be thrown for a valid field");
        }
    }
}
