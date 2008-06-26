package org.intermine.web.logic.query;

import junit.framework.TestCase;


public class ConstraintsTest extends TestCase
{
    public void testEq() {

        // integer
        String xml = "<query name=\"\" model=\"testmodel\" view=\"Employee.name Employee.department.name Employee.department.company.name\" sortOrder=\"Employee.name Employee.department.name Employee.department.company.name\">";
        xml = xml + "<node path=\"Employee\" type=\"Employee\">";
        xml = xml + "</node>";
        xml = xml + "<node path=\"Employee.age\" type=\"int\">";
        xml = xml + "  <constraint op=\"=\" value=\"10\" description=\"\" identifier=\"\" code=\"A\" extraValue=\"\">";
        xml = xml + "  </constraint>";
        xml = xml + "</node>";
        xml = xml + "</query>";


        Constraint c = Constraints.eq("Employee.age", new Integer(10));

        assertEquals(xml, "");

        // string?

    }

    public void testNeq() {
        Constraints.neq("", "");
    }

    public void testLike() {

    }

    public void testContains() {

    }

    public void testLookup() {

    }

    public void testBetween() {

    }

    public void testInString() {

    }

    public void testInList() {

    }


    public void testNotInList() {

    }


    public void testNotInString() {

    }


    public void testIsNull() {

    }


    public void testIsNotNull() {

    }


    public void testGreaterThan() {

    }
    public void testGreaterThanEqualTo() {

    }

    public void testLessThan() {

    }

    public void testlessThanEqualTo() {

    }
}
