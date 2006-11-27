package org.intermine.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryObjectReference;

import org.intermine.metadata.Model;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;

import junit.framework.TestCase;

public class PathQueryCreatorTest extends TestCase {
    private Model model;
    private PathQueryCreator creator;

    public PathQueryCreatorTest (String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        model = Model.getInstanceByName("testmodel");
        creator = new PathQueryCreator();
    }

    public void testAttributeOnly() {
        Path path = new Path(model, "Department.name");

        Query q = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryField qf1 = new QueryField(qc1, "name");
        q.addFrom(qc1);
        q.addToSelect(qf1);

        assertEquals(q.toString(), creator.generate(new HashSet(Collections.singleton(path)), false).toString());
    }

    public void testReference() {
        Path path = new Path(model, "Department.company.name");

        Query q = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Company.class);
        QueryObjectReference ref1 = new QueryObjectReference(qc1, "company");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qc2);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        cs.addConstraint(cc1);
        q.setConstraint(cs);
        QueryField qf1 = new QueryField(qc2, "name");
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(qf1);

        assertEquals(q.toString(), creator.generate(new HashSet(Collections.singleton(path)), false).toString());
    }

    public void testMultiplePaths() {
        Path path1 = new Path(model, "Department.name");
        Path path2 = new Path(model, "Department.company.name");
        Path path3 = new Path(model, "Department.company.CEO.name");
        Set paths = new LinkedHashSet(new ArrayList(Arrays.asList(new Object[] {path1, path2, path3})));

        Query q = new Query();
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryClass qc1 = new QueryClass(Department.class);
        QueryField qf1 = new QueryField(qc1, "name");
        QueryClass qc2 = new QueryClass(Company.class);
        QueryObjectReference ref1 = new QueryObjectReference(qc1, "company");
        ContainsConstraint cc1 = new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qc2);
        cs.addConstraint(cc1);
        QueryField qf2 = new QueryField(qc2, "name");

        QueryClass qc3 = new QueryClass(CEO.class);
        QueryObjectReference ref2 = new QueryObjectReference(qc2, "CEO");
        ContainsConstraint cc2 = new ContainsConstraint(ref2, ConstraintOp.CONTAINS, qc3);
        QueryField qf3 = new QueryField(qc3, "name");
        cs.addConstraint(cc2);

        q.setConstraint(cs);
        q.addFrom(qc1);
        q.addToSelect(qf1);
        q.addFrom(qc2);
        q.addToSelect(qf2);
        q.addFrom(qc3);
        q.addToSelect(qf3);

        Query qq = creator.generate(paths, false);
        System.out.println(q);
        System.out.println(qq);

        assertEquals(q.toString(), creator.generate(paths, false).toString());
    }
}
