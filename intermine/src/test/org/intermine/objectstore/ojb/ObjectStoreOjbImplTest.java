package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.math.BigDecimal;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.QueryField;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.SimpleConstraint;

import org.flymine.model.testmodel.*;

public class ObjectStoreOjbImplTest extends QueryTestCase
{
    private ObjectStore os;

    public ObjectStoreOjbImplTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        os = ObjectStoreOjbImpl.getInstance(db);
        //Thread.sleep(10000);
    }

    public void setUpResults() throws Exception {
        Object[][] r;

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") } };
        results.put("SelectSimpleObject", toList(r));

        r = new Object[][] { { "CompanyA", new Integer(5) },
                             { "CompanyB", new Integer(5) } };
        results.put("SubQuery", toList(r));

        r = new Object[][] { { "CompanyA" } };
        results.put("WhereSimpleEquals", toList(r));
        
        r = new Object[][] { { "CompanyB" } };
        results.put("WhereSimpleNotEquals", toList(r));

        r = new Object[][] { { "CompanyA" },
                             { "CompanyB" } };
        results.put("WhereSimpleLike", toList(r));
        
        r = new Object[][] { { "CompanyA" } };
        results.put("WhereEqualsString", toList(r));
        
        r = new Object[][] { { "CompanyB" } };
        results.put("WhereAndSet", toList(r));
         
        r = new Object[][] { { "CompanyA" },
                             { "CompanyB" } };
        results.put("WhereOrSet", toList(r));
        
        r = new Object[][] { { "CompanyA" } };
        results.put("WhereNotSet", toList(r));
        
        r = new Object[][] { { data.get("DepartmentA1") }, 
                             { data.get("DepartmentB1") },
                             { data.get("DepartmentB2") } };
        results.put("WhereSubQueryField", toList(r));
        
        r = new Object[][] { { data.get("CompanyA") } };
        results.put("WhereSubQueryClass", toList(r));
        
        r = new Object[][] { { data.get("CompanyB") } };
        results.put("WhereNotSubQueryClass", toList(r));
        
        r = new Object[][] { { data.get("CompanyB") } };
        results.put("WhereNegSubQueryClass", toList(r));
        
        r = new Object[][] { { data.get("CompanyA"), data.get("CompanyA") }, 
                             { data.get("CompanyB"), data.get("CompanyB") } };
        results.put("WhereClassClass", toList(r));
        
        r = new Object[][] { { data.get("CompanyA"), data.get("CompanyB") },
                             { data.get("CompanyB"), data.get("CompanyA") } };
        results.put("WhereNotClassClass", toList(r));
        
        r = new Object[][] { { data.get("CompanyA"), data.get("CompanyB") },
                             { data.get("CompanyB"), data.get("CompanyA") } };
        results.put("WhereNegClassClass", toList(r));
        
        r = new Object[][] { { data.get("CompanyA") } };
        results.put("WhereClassObject", toList(r));
        
        r = new Object[][] { { data.get("DepartmentA1"), data.get("EmployeeA1") } };
        results.put("Contains11", toList(r));
         
        r = new Object[][] { { data.get("DepartmentA1"), data.get("EmployeeB1") },
                             { data.get("DepartmentA1"), data.get("EmployeeB3") } };
        results.put("ContainsNot11", toList(r));
        results.put("ContainsNeg11", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1") } };
        results.put("Contains1N", toList(r));
         
        r = new Object[][] { { data.get("ContractorA"), data.get("CompanyA") },
                             { data.get("ContractorA"), data.get("CompanyB") } };
        results.put("ContainsMN", toList(r));        
         
        r = new Object[][] { { data.get("CompanyA"), new Long(1) },
                             { data.get("CompanyB"), new Long(2) } };
        results.put("SimpleGroupBy", toList(r));

        r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1"), data.get("EmployeeA1"), ((Employee)data.get("EmployeeA1")).getAddress() } };
        results.put("MultiJoin", toList(r));

        r = new Object[][] { { new BigDecimal("3476.0000000000"), "DepartmentA1", data.get("DepartmentA1") },
                             { new BigDecimal("3476.0000000000"), "DepartmentB1", data.get("DepartmentB1") },
                             { new BigDecimal("3476.0000000000"), "DepartmentB2", data.get("DepartmentB2") } };
        results.put("SelectComplex", toList(r));
         
        r = new Object[][] { { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") } };
        results.put("SelectClassAndSubClasses", toList(r));

        r = new Object[][] { { data.get("ContractorA") },
                             { data.get("ContractorB") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB2") },
                             { data.get("EmployeeB3") },
                             { data.get("EmployeeA1") },
                             { data.get("EmployeeA2") },
                             { data.get("EmployeeA3") } };
        results.put("SelectInterfaceAndSubClasses", toList(r));

        r = new Object[][] { { data.get("CompanyA") },
                             { data.get("CompanyB") },
                             { data.get("DepartmentB1") },
                             { data.get("DepartmentB2") },
                             { data.get("DepartmentA1") } };
        results.put("SelectInterfaceAndSubClasses2", toList(r));

        r = new Object[][] { { data.get("ContractorA") },
                             { data.get("ContractorB") },
                             { data.get("EmployeeB1") },
                             { data.get("EmployeeB3") },
                             { data.get("EmployeeA1") } };
        results.put("SelectInterfaceAndSubClasses3", toList(r));
    }

    public void executeTest(String type) throws Exception {
        List thingy = os.execute((Query)queries.get(type), 0, 10);
        //System.out.println(type + ": " + thingy);
        //System.out.flush();
        //if ("SelectComplex".equals(type)) {
        //    System.out.println(type + ": " + ((ResultsRow)thingy.get(0)).get(0).getClass());
        //}
        assertEquals(type + " has failed", results.get(type), thingy);
    }

    private List toList(Object[][] o) {
        List rows = new ArrayList();
        for(int i=0;i<o.length;i++) {
            rows.add(new ResultsRow(Arrays.asList((Object[])o[i])));
        }
        return rows;
    }


    public void testCEOWhenSearchingForManager() throws Exception {
        QueryClass c1 = new QueryClass(Manager.class);
        Query q1 = new Query();
        q1.addFrom(c1);
        q1.addToSelect(c1);
        QueryField f1 = new QueryField(c1, "name");
        QueryValue v1 = new QueryValue("EmployeeB1");
        SimpleConstraint sc1 = new SimpleConstraint(f1, SimpleConstraint.EQUALS, v1);
        q1.setConstraint(sc1);
        List l1 = os.execute(q1, 0, 10);
        //System.out.println(l1.toString());
        //System.out.println(l1.get(0).getClass());
        //System.out.println(((ResultsRow) l1.get(0)).get(0).getClass());
        CEO ceo = (CEO) (((ResultsRow) l1.get(0)).get(0));
        //System.out.println(ceo.getSalary());
        assertEquals(45000, ceo.getSalary());
    }

}
