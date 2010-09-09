package org.intermine.web.logic.query;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathQuery;


public class ConstraintsTest extends TestCase
{
    public void testEq() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.eq("Employee.name", "monkey"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.name\" op=\"=\" value=\"monkey\"/></query>", q.toXml(1));
    }

    public void testNeq() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.neq("Employee.name", "monkey"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.name\" op=\"!=\" value=\"monkey\"/></query>", q.toXml(1));
    }

    public void testLike() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.like("Employee.name", "monkey"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.name\" op=\"LIKE\" value=\"monkey\"/></query>", q.toXml(1));
    }

    public void testLookup() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.lookup("Employee.name", "bob", null));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.name\" op=\"LOOKUP\" value=\"bob\"/></query>", q.toXml(1));
    }

    public void testIn() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.in("Employee", " "));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee\" op=\"IN\" value=\" \"/></query>", q.toXml(1));
    }

    public void testNotIn() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.notIn("Employee", "1"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee\" op=\"NOT IN\" value=\"1\"/></query>", q.toXml(1));
    }

    public void testIsNull() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.isNull("Employee.name"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.name\" op=\"IS NULL\"/></query>", q.toXml(1));
    }

    public void testIsNotNull() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.isNotNull("Employee.name"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.name\" op=\"IS NOT NULL\"/></query>", q.toXml(1));
    }

    public void testGreaterThan() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.greaterThan("Employee.length", "1"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.length\" op=\"&gt;\" value=\"1\"/></query>", q.toXml(1));
    }

    public void testGreaterThanEquals() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.greaterThanEqualTo("Employee.length", "-1"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.length\" op=\"&gt;=\" value=\"-1\"/></query>", q.toXml(1));
    }

    public void testLessThan() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.lessThan("Employee.length", "1"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.length\" op=\"&lt;\" value=\"1\"/></query>", q.toXml(1));
    }

    public void testLessThanEquals() {
        PathQuery q = new PathQuery(Model.getInstanceByName("testmodel"));
        q.addConstraint(Constraints.lessThanEqualTo("Employee.length", "-1"));
        assertEquals("<query name=\"query\" model=\"testmodel\" view=\"\"><constraint path=\"Employee.length\" op=\"&lt;=\" value=\"-1\"/></query>", q.toXml(1));
    }

    public void testInList() {
     // TODO test this
    }

    public void testNotInList() {
     // TODO test this
    }
}
