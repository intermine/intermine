package org.intermine.web.logic.query;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.TestUtil;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;


public class ConstraintsTest extends TestCase
{
    String e; // expected constraint
    Constraint c;
    public void testEq() {
        e = "<constraint op=\"=\" value=\"10\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.eq(new Integer(10));
        assertEquals(e, c.toXML());

        e = "<constraint op=\"=\" value=\"monkey\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.eq("monkey");
        assertEquals(e, c.toXML());
    }

    public void testNeq() {
        e = "<constraint op=\"!=\" value=\"42\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.neq(new Integer(42));
        assertEquals(e, c.toXML());

        e = "<constraint op=\"!=\" value=\"pants\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.neq("pants");
        assertEquals(e, c.toXML());
    }

    public void testLike() {
        e = "<constraint op=\"CONTAINS\" value=\"value\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.like("value");
        assertEquals(e, c.toXML());

        e = "<constraint op=\"CONTAINS\" value=\"*wars\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.like("*wars");
        assertEquals(e, c.toXML());
    }

    public void testContains() {
        e = "<constraint op=\"CONTAINS\" value=\"abcdefghijklmnopqrstuvwxyz!£$%^&*()\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.contains("abcdefghijklmnopqrstuvwxyz!£$%^&*()");
        assertEquals(e, c.toXML());

        e = "<constraint op=\"CONTAINS\" value=\" \" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.contains(" ");
        assertEquals(e, c.toXML());
    }

    public void testLookup() {
        e = "<constraint op=\"LOOKUP\" value=\"bob\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.lookup("bob");
        assertEquals(e, c.toXML());

        e = "<constraint op=\"LOOKUP\" value=\"\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.lookup("");
        assertEquals(e, c.toXML());
    }

    public void testBetween() {
        e = "<constraint op=\"BETWEEN\" value=\"1\" description=\"\" identifier=\"\" code=\"\" extraValue=\"2\"></constraint>";
        c = Constraints.between(1, 2);
        assertEquals(e, c.toXML());
    }

    public void testIn() {
        e = "<constraint op=\"IN\" value=\" \" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.in(" ");
        assertEquals(e, c.toXML());

        e = "<constraint op=\"IN\" value=\"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.in("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        assertEquals(e, c.toXML());
    }

    public void testInList() {
     // TODO test this
    }

    public void testNotIn() {
        e = "<constraint op=\"NOT IN\" value=\"1\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.notIn("1");
        assertEquals(e, c.toXML());

        e = "<constraint op=\"NOT IN\" value=\"\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.notIn("");
        assertEquals(e, c.toXML());
    }

    public void testNotInList() {
     // TODO test this
    }

    public void testIsNull() {
        e = "<constraint op=\"IS NULL\" value=\"null\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.isNull();
        assertEquals(e, c.toXML());
    }

    public void testIsNotNull() {
        e = "<constraint op=\"IS NOT NULL\" value=\"null\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.isNotNull();
        assertEquals(e, c.toXML());
    }

    public void testGreaterThan() {
        e = "<constraint op=\">\" value=\"0\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.greaterThan(0);
        assertEquals(e, c.toXML());
    }

    public void testGreaterThanEqualTo() {
        e = "<constraint op=\">=\" value=\"-1\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.greaterThanEqualTo(-1);
        assertEquals(e, c.toXML());
    }

    public void testLessThan() {
        e = "<constraint op=\"<\" value=\"1000000000\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.lessThan(1000000000);
        assertEquals(e, c.toXML());
    }

    public void testlessThanEqualTo() {
        e = "<constraint op=\"<=\" value=\"1.0\" description=\"\" identifier=\"\" code=\"\" extraValue=\"\"></constraint>";
        c = Constraints.lessThanEqualTo(1.0);
        assertEquals(e, c.toXML());
    }
}
