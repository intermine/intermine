package org.flymine.objectstore.ojb;

import junit.framework.*;
import java.util.*;
import org.flymine.objectstore.*;
import org.flymine.objectstore.query.*;
import org.flymine.sql.*;
import org.apache.ojb.broker.*;
import org.apache.ojb.broker.metadata.*;

import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Company;

public class SqlGeneratorFlymineImplTest extends TestCase
{
    private DescriptorRepository dr;
    private SqlGeneratorFlymineImpl gen;

    public SqlGeneratorFlymineImplTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.unittest");
        ObjectStoreOjbImpl os = ObjectStoreOjbImpl.getInstance(db);
        PersistenceBroker broker = os.getPersistenceBroker();
        dr = broker.getDescriptorRepository();
        gen = (SqlGeneratorFlymineImpl) broker.serviceSqlGenerator();
    }

    public void testSubquery() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        q1.addToSelect(v1);
        Query q2 = new Query();
        q2.addFrom(q1);
        QueryField f1 = new QueryField(q1, c1, "name");
        QueryField f2 = new QueryField(q1, v1);
        q2.addToSelect(f1);
        q2.addToSelect(f2);
        assertEquals("SELECT DISTINCT a1_.a1_name AS a2_, a1_.a2_ AS a3_ FROM (SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, 5 AS a2_ FROM Company AS a1_) AS a1_ ORDER BY a1_.a1_name, a1_.a2_ LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q2, dr, 0, 10000));
    }

    public void testWhereSimpleEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        assertEquals("SELECT DISTINCT a1_.vatNumber AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber = 5 ORDER BY a1_.vatNumber LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereSimpleNotEquals() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.NOT_EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        assertEquals("SELECT DISTINCT a1_.vatNumber AS a2_ FROM Company AS a1_ WHERE a1_.vatNumber != 5 ORDER BY a1_.vatNumber LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereSimpleLike() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name LIKE 'flibble' ORDER BY a1_.name LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereSimpleEqualString() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryField f1 = new QueryField(c1, "name");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(sc1);
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE a1_.name = 'flibble' ORDER BY a1_.name LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereAndSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryValue v2 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (a1_.name LIKE 'flibble' AND a1_.vatNumber > 5) ORDER BY a1_.name LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereOrSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryValue v2 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.OR);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE (a1_.name LIKE 'flibble' OR a1_.vatNumber > 5) ORDER BY a1_.name LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereNotSet() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryValue v1 = new QueryValue("flibble");
        QueryValue v2 = new QueryValue(new Integer(5));
        QueryField f1 = new QueryField(c1, "name");
        QueryField f2 = new QueryField(c1, "vatNumber");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.MATCHES, v1);
        SimpleConstraint sc2 = new SimpleConstraint(f2, SimpleConstraint.GREATER_THAN, v2);
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        cs1.addConstraint(sc1);
        cs1.addConstraint(sc2);
        cs1.setNegated(true);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        q1.setConstraint(cs1);
        assertEquals("SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_ WHERE ( NOT (a1_.name LIKE 'flibble' AND a1_.vatNumber > 5)) ORDER BY a1_.name LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereSubqueryField() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        QueryField f1 = new QueryField(c1, "name");
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(f1);
        QueryClass c2 = new QueryClass(Department.class);
        QueryField f2 = new QueryField(c2, "name");
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, f2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        assertEquals("SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name FROM Department AS a1_ WHERE a1_.name IN (SELECT DISTINCT a1_.name AS a2_ FROM Company AS a1_) ORDER BY a1_.ID LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q2, dr, 0, 10000));
    }

    public void testWhereSubqueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryClass c2 = new QueryClass(Department.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, c2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        assertEquals("SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name FROM Department AS a1_ WHERE a1_.ID IN (SELECT DISTINCT a1_.ID AS a1_ID FROM Company AS a1_) ORDER BY a1_.ID LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q2, dr, 0, 10000));
    }

    public void testWhereNotSubqueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryClass c2 = new QueryClass(Department.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.DOES_NOT_CONTAIN, c2);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        assertEquals("SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name FROM Department AS a1_ WHERE a1_.ID NOT IN (SELECT DISTINCT a1_.ID AS a1_ID FROM Company AS a1_) ORDER BY a1_.ID LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q2, dr, 0, 10000));
    }

    public void testWhereNegSubqueryClass() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryClass c2 = new QueryClass(Department.class);
        SubqueryConstraint sqc1 = new SubqueryConstraint(q1, SubqueryConstraint.CONTAINS, c2);
        sqc1.setNegated(true);
        Query q2 = new Query();
        q2.addFrom(c2);
        q2.addToSelect(c2);
        q2.setConstraint(sqc1);
        assertEquals("SELECT DISTINCT a1_.ID AS a1_ID, a1_.companyId AS a1_companyId, a1_.managerId AS a1_managerId, a1_.name AS a1_name FROM Department AS a1_ WHERE a1_.ID NOT IN (SELECT DISTINCT a1_.ID AS a1_ID FROM Company AS a1_) ORDER BY a1_.ID LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q2, dr, 0, 10000));
    }

    public void testWhereClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        assertEquals("SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Company AS a1_, Company AS a2_ WHERE (a1_.ID = a2_.ID) ORDER BY a1_.ID, a2_.ID LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereNotClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.NOT_EQUALS, qc2);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        assertEquals("SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Company AS a1_, Company AS a2_ WHERE ( NOT (a1_.ID = a2_.ID)) ORDER BY a1_.ID, a2_.ID LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereNegClassClass() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Company.class);
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, qc2);
        cc1.setNegated(true);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        q1.addToSelect(qc2);
        q1.setConstraint(cc1);
        assertEquals("SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber, a2_.ID AS a2_ID, a2_.addressId AS a2_addressId, a2_.name AS a2_name, a2_.vatNumber AS a2_vatNumber FROM Company AS a1_, Company AS a2_ WHERE ( NOT (a1_.ID = a2_.ID)) ORDER BY a1_.ID, a2_.ID LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }

    public void testWhereClassObject() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        Company obj = new Company();
        ClassDescriptor cld = dr.getDescriptorFor(Company.class);
        FieldDescriptor fld = cld.getFieldDescriptorByName("id");
        fld.getPersistentField().set(obj, new Integer(2345));
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, obj);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.setConstraint(cc1);
        assertEquals("SELECT DISTINCT a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.name AS a1_name, a1_.vatNumber AS a1_vatNumber FROM Company AS a1_ WHERE (a1_.ID = 2345) ORDER BY a1_.ID LIMIT 10000 OFFSET 0", gen.getPreparedSelectStatement(q1, dr, 0, 10000));
    }


}
