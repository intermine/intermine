package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.ResultsRow;

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
        


         
//          r = new Object[][] { { data.get("DepartmentA1"), data.get("EmployeeA1") } };
//          results.put("Contains11", toList(r));
         
//          r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1") } };
//          results.put("Contains1N", toList(r));
         
//          r = new Object[][] { { data.get("ContractorA"), data.get("CompanyA") },
//                               { data.get("ContractorA"), data.get("CompanyB") } };
//          results.put("ContainsMN", toList(r));        
         
//          r = new Object[][] { { data.get("CompanyA"), new Integer(1) },
//                               { data.get("CompanyB"), new Integer(2) } };
//          results.put("SimpleGroupBy", toList(r));

//          r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1"), data.get("EmployeeA1"), ((Employee)data.get("EmployeeA1")).getAddress() } };
//          results.put("MultiJoin", toList(r));

//          r = new Object[][] { { data.get("CompanyA"), data.get("DepartmentA1"), new Integer(3476), data.get("DepartmentA1") },
//                               { data.get("CompanyA"), data.get("DepartmentB1"), new Integer(3476), data.get("DepartmentB1") },
//                               { data.get("CompanyA"), data.get("DepartmentB2"), new Integer(3476), data.get("DepartmentB2") },
//                               { data.get("CompanyB"), data.get("DepartmentA1"), new Integer(3476), data.get("DepartmentA1") },
//                               { data.get("CompanyB"), data.get("DepartmentB1"), new Integer(3476), data.get("DepartmentB1") },
//                               { data.get("CompanyB"), data.get("DepartmentB2"), new Integer(3476), data.get("DepartmentB2") } };
//          results.put("SelectComplex", toList(r));
         
//          r = new Object[][] { { data.get("EmployeeA1") },
//                               { data.get("EmployeeA2") },
//                               { data.get("EmployeeA3") },
//                               { data.get("EmployeeB1") },
//                               { data.get("EmployeeB2") },
//                               { data.get("EmployeeB3") } };
//          results.put("SelectClassAndSubClasses", toList(r));

//          r = new Object[][] { { data.get("EmployeeA1") },
//                               { data.get("EmployeeA2") },
//                               { data.get("EmployeeA3") },
//                               { data.get("EmployeeB1") },
//                               { data.get("EmployeeB2") },
//                               { data.get("EmployeeB3") },
//                               { data.get("ContractorA") },
//                               { data.get("ContractorB") } };
//          results.put("SelectInterfaceAndSubClasses", toList(r));
    }

    public void executeTest(String type) throws Exception {
        assertEquals(type + " has failed",results.get(type), os.execute((Query)queries.get(type), 0, 10));
    }

    private List toList(Object[][] o) {
        List rows = new ArrayList();
        for(int i=0;i<o.length;i++) {
            rows.add(new ResultsRow(Arrays.asList((Object[])o[i])));
        }
        return rows;
    }
}
