package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;

import org.flymine.objectstore.ojb.QueryPackage;
import org.flymine.model.testmodel.Department;
import org.flymine.model.testmodel.Employee;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;

public class QueryPackageTest extends TestCase
{
    private QueryPackage query;
    private ClassDescriptor cld1, cld2;
    private DescriptorRepository repository;
    private Query fq;

    public QueryPackageTest(String arg) {
        super(arg);
    }

    public void setUp() {
        fq = new Query();
        fq.addFrom(new QueryClass(Department.class));
        fq.addFrom(new QueryClass(Employee.class));
        assertNotNull("Problem creating Query instance", fq);

        // need a DescriptorRepository to construct ClassDescriptors
        repository = new DescriptorRepository();
        cld1 = new ClassDescriptor(repository);
        cld2 = new ClassDescriptor(repository);
        cld1.setClassOfObject(Department.class);
        cld2.setClassOfObject(Employee.class);
    }


    public void testNullConstructor() throws Exception {
        try {
            ClassDescriptor clds[] = {cld1};
            query = new QueryPackage(null, clds);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
        try {
            query = new QueryPackage(fq, null);
            fail("Expected: NullPointerException");
        } catch (NullPointerException e) {
        }
    }

    public void testWrongClassDescriptors() throws Exception {
        try {
            // fq has two classes, ClassDescriptor[] only contains one ClassDescriptor
            query = new QueryPackage(fq, new ClassDescriptor[] {cld1});
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testCorrectClassDescriptors() throws Exception {
        try {
            query = new QueryPackage(fq, new ClassDescriptor[] {cld1, cld2});
        } catch (IllegalArgumentException e) {
            fail("No IllegalArgumentException should have been thrown" + e.getMessage());
        }
    }

}
