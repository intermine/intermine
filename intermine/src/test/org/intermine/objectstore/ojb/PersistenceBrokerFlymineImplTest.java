package org.flymine.objectstore.ojb;

import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.lang.reflect.Field;


import junit.framework.TestCase;

import org.apache.ojb.broker.*;
import org.apache.ojb.broker.metadata.*;

import org.flymine.sql.Database;
import org.flymine.sql.DatabaseFactory;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.query.*;
import org.flymine.objectstore.ojb.FlymineSqlSelectStatement;
import org.flymine.objectstore.ojb.PersistenceBrokerFlyMineImpl;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.proxy.LazyCollection;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.model.testmodel.*;
import org.flymine.util.RelationType;

public class PersistenceBrokerFlymineImplTest extends TestCase
{
    PersistenceBrokerFlyMineImpl broker;
    DescriptorRepository dr;

    public PersistenceBrokerFlymineImplTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        ObjectStoreOjbImpl os = (ObjectStoreOjbImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        broker = (PersistenceBrokerFlyMineImpl) os.getPersistenceBroker();
        dr = broker.getDescriptorRepository();
    }


    // check that query to retrieve collection is correctly formed
    public void testGetCollectionQuery() throws Exception {
        broker.clearCache();
        // need an object, its ClassDescriptor and CollectionDescriptor for one of its collections
        Department dept = getDeptExampleObject();
        ClassDescriptor cldDept = dr.getDescriptorFor(Department.class);
        CollectionDescriptor codEmployees = cldDept.getCollectionDescriptorByName("employees");

        Query q = broker.getCollectionQuery(dept, cldDept, codEmployees);
        FlymineSqlSelectStatement stmt = new FlymineSqlSelectStatement(q, dr);

        FieldDescriptor fld = cldDept.getFieldDescriptorByName("id");
        Integer id = (Integer) fld.getPersistentField().get(dept);

        assertEquals(stmt.getStatement(), "SELECT DISTINCT a1_.CLASS AS a1_CLASS, a1_.ID AS a1_ID, a1_.addressId AS a1_addressId, a1_.age AS a1_age, a1_.companyId AS a1_companyId, a1_.departmentId AS a1_departmentId, a1_.departmentThatRejectedMeId AS a1_departmentThatRejectedMeId, a1_.fullTime AS a1_fullTime, a1_.name AS a1_name, a1_.salary AS a1_salary, a1_.title AS a1_title FROM Employee AS a1_, Department AS a2_ WHERE ((a2_.ID = " + id.intValue() + ") AND (a2_.ID = a1_.departmentId)) ORDER BY a1_.ID");

    }

    // check that query to retrieve collection is correctly formed for many to many relationship
    public void testGetCollectionQueryMtoN() throws Exception {
        broker.clearCache();
        // Many Contractors per Company, check query still formed correctly
        Company comp = getCompExampleObject();
        ClassDescriptor cldComp = dr.getDescriptorFor(Company.class);
        CollectionDescriptor codContractors = cldComp.getCollectionDescriptorByName("contractors");

        Query q = broker.getCollectionQuery(comp, cldComp, codContractors);
        FlymineSqlSelectStatement stmt = new FlymineSqlSelectStatement(q, dr);

        FieldDescriptor fld = cldComp.getFieldDescriptorByName("id");
        Integer id = (Integer) fld.getPersistentField().get(comp);

        assertEquals(stmt.getStatement(), "SELECT DISTINCT a1_.ID AS a1_ID, a1_.businessAddressId AS a1_businessAddressId, a1_.name AS a1_name, a1_.personalAddressId AS a1_personalAddressId FROM Contractor AS a1_, Company AS a2_, CompanyContractor AS ind_a2_a1_CompanyContractor_ WHERE ((a2_.ID = " + id.intValue() + ") AND (a2_.ID = ind_a2_a1_CompanyContractor_.companyId AND a1_.ID = ind_a2_a1_CompanyContractor_.contractorId)) ORDER BY a1_.ID");

    }


    // test that field of materialized object is set to a LazyCollection
    public void testLazyCollectionField() throws Exception {
        broker.clearCache();
        Department dept = getDeptExampleObject();
        ClassDescriptor cldDept = dr.getDescriptorFor(Department.class);
        CollectionDescriptor codEmployees = cldDept.getCollectionDescriptorByName("employees");

        // override anything in mapping file
        codEmployees.setLazy(true);

        broker.retrieveCollection(dept, cldDept, codEmployees, true);

        Collection col = dept.getEmployees();
        if (!(col instanceof LazyCollection))
            fail("Expected Department.employees to be a LazyCollection");
    }


    // test that entire collection of employees has been materialized
    public void testNotLazyCollectionField() throws Exception {
        broker.clearCache();
        Department dept = getDeptExampleObject();
        ClassDescriptor cldDept = dr.getDescriptorFor(Department.class);
        CollectionDescriptor codEmployees = cldDept.getCollectionDescriptorByName("employees");

        // override anything in mapping file
        codEmployees.setLazy(false);

        broker.retrieveCollection(dept, cldDept, codEmployees, true);

        Collection col = dept.getEmployees();
        if (col instanceof LazyCollection)
            fail("Expected Department.employees to be materialized but was a LazyCollection");

    }

    public void testLazyReferenceField() throws Exception {
        broker.clearCache();
        Department dept = getDeptExampleObject();
        ClassDescriptor cldDept = dr.getDescriptorFor(Department.class);
        ClassDescriptor cldCompany = dr.getDescriptorFor(Company.class);
        ObjectReferenceDescriptor ordCompany = cldDept.getObjectReferenceDescriptorByName("company");

        // override anything in mapping file
        ordCompany.setLazy(true);

        Object obj = broker.getReferencedObject(dept, ordCompany, cldDept);

        if (!(obj instanceof LazyReference))
            fail("Expected Department.company to be a LazyReference");
    }

    public void testNotLazyReferenceField() throws Exception {
        broker.clearCache();
        Department dept = getDeptExampleObject();
        ClassDescriptor cldDept = dr.getDescriptorFor(Department.class);
        ClassDescriptor cldCompany = dr.getDescriptorFor(Company.class);
        ObjectReferenceDescriptor ordCompany = cldDept.getObjectReferenceDescriptorByName("company");

        // override anything in mapping file
        ordCompany.setLazy(false);

        Object obj = broker.getReferencedObject(dept, ordCompany, cldDept);

        if (obj instanceof LazyReference)
            fail("Expected Department.company to be a materialized object but was a LazyReference");
    }


    public void testDescribeRelationOneToOne() throws Exception {
        Field f = Company.class.getDeclaredField("CEO");
        assertEquals(RelationType.ONE_TO_ONE, broker.describeRelation(f));
    }

    public void testDescribeRelationOneToN() throws Exception {
        Field f = Company.class.getDeclaredField("departments");
        assertEquals(RelationType.ONE_TO_N, broker.describeRelation(f));
    }

    public void testDescribeRelationNToOne() throws Exception {
        Field f = Department.class.getDeclaredField("company");
        assertEquals(RelationType.N_TO_ONE, broker.describeRelation(f));
    }

    public void testDescribeRelationMToN() throws Exception {
        Field f = Company.class.getDeclaredField("contractors");
        assertEquals(RelationType.M_TO_N, broker.describeRelation(f));
    }

    public void testDescribeRelationNamedMToN() throws Exception {
        Field f = Company.class.getDeclaredField("oldContracts");
        assertEquals(RelationType.M_TO_N, broker.describeRelation(f));
    }

    public void testDescribeRelationNamedOneToN() throws Exception {
        Field f = Department.class.getDeclaredField("rejectedEmployees");
        assertEquals(RelationType.ONE_TO_N, broker.describeRelation(f));
    }

    public void testDescribeRelationNamedNToOne() throws Exception {
        Field f = Employee.class.getDeclaredField("departmentThatRejectedMe");
        assertEquals(RelationType.N_TO_ONE, broker.describeRelation(f));
    }

    public void testDescribeRelationNoRelation() throws Exception {
        Field f = Department.class.getDeclaredField("name");
        assertEquals(RelationType.NOT_RELATION, broker.describeRelation(f));
    }


    // set up a Department object with an id
    private Department getDeptExampleObject() throws Exception {
        Department dept = new Department();
        Class deptClass = dept.getClass();
        Field f = deptClass.getDeclaredField("id");
        f.setAccessible(true);
        f.set(dept, new Integer(1234));
        dept.setName("DepartmentA1");
        f = deptClass.getDeclaredField("companyId");
        f.setAccessible(true);
        f.set(dept, new Integer(101));

        return dept;
    }


    // set up a Company object with an id
    private Company getCompExampleObject() throws Exception {
        Company comp = new Company();
        Class compClass = comp.getClass();
        Field f = compClass.getDeclaredField("id");
        f.setAccessible(true);
        f.set(comp, new Integer(1234));
        comp.setName("CompanyA1");

        return comp;
    }



}
