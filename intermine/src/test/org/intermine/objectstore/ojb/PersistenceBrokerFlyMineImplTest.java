package org.flymine.objectstore.ojb;

import java.util.Collection;

import junit.framework.TestCase;

import org.apache.ojb.broker.singlevm.DelegatingPersistenceBroker;
import org.apache.ojb.broker.metadata.*;

import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.proxy.LazyCollection;
import org.flymine.objectstore.proxy.LazyReference;
import org.flymine.model.testmodel.*;

import org.flymine.util.TypeUtil;

public class PersistenceBrokerFlyMineImplTest extends TestCase
{
    PersistenceBrokerFlyMineImpl broker;
    DescriptorRepository dr;

    public PersistenceBrokerFlyMineImplTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        ObjectStoreOjbImpl os = (ObjectStoreOjbImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        PersistenceBrokerFlyMine pb = (PersistenceBrokerFlyMine) os.getPersistenceBroker();
        if (pb instanceof PersistenceBrokerFlyMineImpl) {
            broker = (PersistenceBrokerFlyMineImpl) pb;
        } else {
            broker = (PersistenceBrokerFlyMineImpl) ((DelegatingPersistenceBroker) pb).getDelegate();
        }
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
        FlyMineSqlSelectStatement stmt = new FlyMineSqlSelectStatement(q, dr);

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
        FlyMineSqlSelectStatement stmt = new FlyMineSqlSelectStatement(q, dr);

        FieldDescriptor fld = cldComp.getFieldDescriptorByName("id");
        Integer id = (Integer) fld.getPersistentField().get(comp);

        assertEquals(stmt.getStatement(), "SELECT DISTINCT a1_.ID AS a1_ID, a1_.businessAddressId AS a1_businessAddressId, a1_.name AS a1_name, a1_.personalAddressId AS a1_personalAddressId FROM Contractor AS a1_, Company AS a2_, CompanysContractors AS ind_a2_a1_CompanysContractors_ WHERE ((a2_.ID = " + id.intValue() + ") AND (a2_.ID = ind_a2_a1_CompanysContractors_.companysId AND a1_.ID = ind_a2_a1_CompanysContractors_.contractorsId)) ORDER BY a1_.ID");
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
        ordCompany.getForeignKeyFieldDescriptors(cldDept)[0].getPersistentField().set(dept, new Integer(0));

        // override anything in mapping file
        ordCompany.setLazy(true);

        Object obj = broker.getReferencedObject(dept, ordCompany, cldDept);

        if (!(obj instanceof LazyReference))
            fail("Expected Department.company to be a LazyReference but was " + obj);
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

    // set up a Department object with an id
    private Department getDeptExampleObject() throws Exception {
        Department dept = new Department();
        TypeUtil.setFieldValue(dept, "id", new Integer(1));
        return dept;
    }

    // set up a Company object with an id
    private Company getCompExampleObject() throws Exception {
        Company comp = new Company();
        TypeUtil.setFieldValue(comp, "id", new Integer(2));
        return comp;
    }
}
