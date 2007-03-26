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

public class QueryObjectReferenceTest extends TestCase
{
    private QueryClass qc;

    public QueryObjectReferenceTest(String arg1) {
        super(arg1);
    }

    public void setUp() {
        qc = new QueryClass(Department.class);
    }

    public void testMissingField() {
        try {
            new QueryObjectReference(qc, "secretarys");
            fail("A IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testEmptyField() {
        try {
            new QueryObjectReference(qc, "");
            fail("A IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {
        }
    }
    
    public void testNullField() throws Exception {
        try {
            new QueryObjectReference(qc, (String)null);
            fail("A NoSuchFieldException should have been thrown");
        } catch (NullPointerException e) {
        }
    }

    public void testInvalidField() {
        try {
            new QueryObjectReference(qc, "employees");
            fail("An IllegalArgumentException should have been thrown");
        } catch (Exception e) {
        }
    }

    public void testInvalidField2() {
        try {
            new QueryObjectReference(qc, "name");
            fail("An IllegalArgumentException should have been thrown");
        } catch (Exception e) {
        }
    }

    public void testValidField() {
        try {
            new QueryObjectReference(qc, "company");
        } catch (Exception e) {
            fail("An exception should not be thrown for a valid field: " + e);
        }
    }
}
