package org.intermine.web.results;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.iql.IqlQuery;

import org.intermine.metadata.Model;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;

import junit.framework.TestCase;

public class PagedResultsTest extends TestCase
{
    public PagedResultsTest(String arg) {
        super(arg);
    }

    private ObjectStoreDummyImpl os;
    private IqlQuery fq;
    private Model model;

    public void setUp() throws Exception {
        os = new ObjectStoreDummyImpl();
        os.setResultsSize(15);
        fq = new IqlQuery("select c1, c2, d1, d2 from Company as c1, Company as c2, Department as d1, Department as d2", "org.intermine.model.testmodel");
        model = Model.getInstanceByName("testmodel");
    }

 /*   
    private PagedResults getEmptyResults() throws Exception {
        os.setResultsSize(0);
        Results results = os.execute(fq.toQuery());
        try {
            results.setBatchSize(20);
            results.get(0);
        } catch (IndexOutOfBoundsException e) {
        }
        return new PagedResults(results, model);
    }

    private PagedResults getExactResults() throws Exception {
        Results results = os.execute(fq.toQuery());
        // Make sure we definitely know the end
        results.setBatchSize(20);
        results.get(0);
        return new PagedResults(results, model);
    }

    private PagedResults getEstimateTooHighResults() throws Exception {
        os.setEstimatedResultsSize(25);
        Results results = os.execute(fq.toQuery());
        results.setBatchSize(1);
        return new PagedResults(results, model);
    }

    private PagedResults getEstimateTooLowResults() throws Exception {
        os.setEstimatedResultsSize(10);
        Results results = os.execute(fq.toQuery());
        results.setBatchSize(1);
        return new PagedResults(results, model);
    }

    public void testConstructor() throws Exception {
        PagedResults dr = getExactResults();
        assertEquals(4, dr.getColumns().size());
    }

    public void testSizeExact() throws Exception {
        PagedResults dr = getExactResults();
        dr.setPageSize(10);
        assertEquals(15, dr.getSize());
    }

    public void testSizeHigh() throws Exception {
        PagedResults dr = getEstimateTooHighResults();
        dr.setPageSize(10);
        assertEquals(25, dr.getSize());
    }

    public void testGetIndexForColumn() throws Exception {
        Map queries = readQueries();
        Map headers = new HashMap();
        Map expected = new HashMap();
        Map results = new HashMap();

        Employee e1 = new Employee();
        e1.setName("employee1");
        e1.setId(new Integer(101));

        Employee e2 = new Employee();
        e2.setName("employee2");
        e2.setId(new Integer(102));

        Department d1 = new Department();
        d1.setName("DepartmentA1");
        d1.setId(new Integer(201));
       
        Company c1 = (Company) DynamicUtil.instantiateObject("org.intermine.model.testmodel.Company", null);
        c1.setName("Company1");
        c1.setId(new Integer(301));

        ObjectStore os = new ObjectStoreDummyImpl();
        results.put("employee", toList(new Object[][] { { e1 } }));
        expected.put("employee", Arrays.asList(new ResultElement[] {new ResultElement(e1,e1.getId(), "Employee", false)}));
        headers.put("employee", toList(new Object[] {"Employee"}));
        
        results.put("employeeName", toList(new Object[][] { { e1 } }));
        expected.put("employeeName", Arrays.asList(new Object[] {new ResultElement(e1,e1.getId(), "Employee", false)}));
        headers.put("employeeName", toList(new Object[] {"Employee"}));

        results.put("employeeAndName", toList(new Object[][] { { e1 } }));
        expected.put("employeeAndName", Arrays.asList(new Object[] {new ResultElement(e1,e1.getId(),"Employee", false),
            new ResultElement(e1,e1.getId(),"Employee", false)}));
        headers.put("employeeAndName", toList(new Object[] {"Employee", "Employee"}));
 
        results.put("employeeDepartment", toList(new Object[][] { { e1, d1 } }));
        expected.put("employeeDepartment", Arrays.asList(new Object[] {new ResultElement(e1,e1.getId(), "Employee", false), 
                new ResultElement(d1,d1.getId(),"Department", false)}));
        headers.put("employeeDepartment", toList(new Object[] {"Employee", "Employee.department"}));

        results.put("employeeDepartmentReference", toList(new Object[][] { { e1, d1 } }));
        expected.put("employeeDepartmentReference", Arrays.asList(new Object[] {new ResultElement(e1,e1.getId(), "Employee", false), 
                new ResultElement(d1, d1.getId(), "Department", false)}));
        headers.put("employeeDepartmentReference", toList(new Object[] {"Employee", "Employee.department"}));
        
        results.put("employeeDepartmentCompany", toList(new Object[][] { { e1, d1, c1 } }));
        expected.put("employeeDepartmentCompany", Arrays.asList(new Object[] {new ResultElement(e1,e1.getId(),"Employee", false),
                new ResultElement(d1,d1.getId(),"Department", false),
                new ResultElement(c1,c1.getId(),"Company", false)}));
        headers.put("employeeDepartmentCompany", toList(new Object[] {"Employee", "Employee.department", "Employee.department.company"}));
        
        results.put("employeeCompany", toList(new Object[][] { { e1, c1 } }));
        expected.put("employeeCompany", Arrays.asList(new Object[] {new ResultElement(e1,e1.getId(), "Employee", false),
                new ResultElement(c1,c1.getId(),"Company", false)}));
        headers.put("employeeCompany", toList(new Object[] {"Employee", "Employee.department.company"}));
        
        results.put("employeeDepartmentEmployees", toList(new Object[][] { { e1, d1, e2 } } ));
        expected.put("employeeDepartmentEmployees", Arrays.asList(new Object[] {new ResultElement(e1,e1.getId(), "Employee", false),
                new ResultElement(d1,d1.getId(),"Department", false),
                new ResultElement(e2,e2.getId(),"Employee", false)}));
        headers.put("employeeDepartmentEmployees", toList(new Object[] {"Employee", "Employee.department", "Employee.department.employees"}));
        
        // check all queries, fail if no expected values set
        Iterator queryIter = queries.entrySet().iterator();
        while (queryIter.hasNext()) {
        	Map.Entry entry = (Map.Entry) queryIter.next();
        	String queryName = (String) entry.getKey();
            if (!expected.containsKey(queryName)) {
                fail("no expected column indexes set up for query: " + queryName);
            }

            PathQuery pq = (PathQuery) entry.getValue();
            Map pathToQueryNode = new HashMap();
            Query q = MainHelper.makeQuery(pq, new HashMap(), pathToQueryNode);
            Results r = new DummyResults(os, q, (List) results.get(queryName));
//            PagedResults pr = new PagedResults(pq.getView(), r, model, pathToQueryNode, null);
            WebResults webResults = new WebResults((List) headers.get(queryName),r, model, pathToQueryNode, null);
            PagedResults pr = new PagedResults(webResults);
            assertEquals("Failed with query: " + queryName + ". ", (List) expected.get(queryName), (List) pr.getRows().get(0));
         }
    }

    private class DummyResults extends Results {
        List rows;

        public DummyResults(ObjectStore os, Query query, List rows) {
            super(query, os, os.getSequence());
            this.rows = rows;	
        }

        public Object get(int index) {
            return rows.get(index);
        }
    }

    private List toList(Object array[][]) {
        List rows = new ArrayList();
        for(int i=0;i<array.length;i++) {
            rows.add(new ResultsRow(Arrays.asList((Object[])array[i])));
        }
        return rows;
    }
    
    private List toList(Object array[]) {
        List rows = new ArrayList();
        for(int i=0;i<array.length;i++) {
            rows.add(array[i]);
        }
        return rows;
    }
    
    private Map readQueries() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("MainHelperTest.xml");
        return PathQueryBinding.unmarshal(new InputStreamReader(is));
    }
    */
    
//     public void testSizeLow() throws Exception {
//         PagedResults dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         // Calling size() affects the estimate as it tries to fetch
//         // more rows.  I think the best thing to do here is to check
//         // that the size is greater than 10 and less than 15 to prove
//         // that the size is not stuck at the estimate
//         assertTrue(dr.getSize() > 10);
//         assertTrue(dr.getSize() <= 15);
//     }

//     public void testSizeEmpty() throws Exception {
//         PagedResults dr = getEmptyResults();
//         dr.setPageSize(10);
//         assertEquals(0, dr.getSize());
//     }

//     public void testEndExact() throws Exception {
//         // At the beginning
//         PagedResults dr = getExactResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(0);
//         assertEquals(9, dr.getEndIndex());
//         // Abutting the end
//         dr = getExactResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(5);
//         assertEquals(14, dr.getEndIndex());
//         // Overlapping the end
//         dr = getExactResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(10);
//         assertEquals(14, dr.getEndIndex());
//     }

//     public void testEndHigh() throws Exception {
//         // At the beginning
//         PagedResults dr = getEstimateTooHighResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(0);
//         assertEquals(9, dr.getEndIndex());
//         // Abutting the end
//         dr = getEstimateTooHighResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(5);
//         assertEquals(14, dr.getEndIndex());
//         // Overlapping the end
//         dr = getEstimateTooHighResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(10);
//         assertEquals(14, dr.getEndIndex());
//     }

//     public void testEndLow() throws Exception {
//         // At the beginning
//         PagedResults dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(0);
//         assertEquals(9, dr.getEndIndex());
//         // Abutting the end
//         dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(5);
//         assertEquals(14, dr.getEndIndex());
//         // Overlapping the end
//         dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         dr.setStartIndex(10);
//         assertEquals(14, dr.getEndIndex());
//     }

    // For the moment the end is -1 if the underlying results is empty
    // Anything using PagedResults should call getSize() first
//     public void testEndEmpty() throws Exception {
//         PagedResults dr = getEmptyResults();
//         dr.setPageSize(10);
//         assertEquals(-1, dr.getEndIndex());
//     }

//     public void testButtonsExact() throws Exception {
//         PagedResults dr = getExactResults();
//         dr.setPageSize(10);
//         // At the beginning
//         dr.setStartIndex(0);
//         assertTrue(dr.isFirstPage());
//         assertFalse(dr.isLastPage());
//         // Abutting the end
//         dr.setStartIndex(5);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//         // Overlapping the end
//         dr.setStartIndex(10);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//     }

//     public void testButtonsHigh() throws Exception {
//         PagedResults dr = getEstimateTooHighResults();
//         dr.setPageSize(10);
//         // At the beginning
//         dr.setStartIndex(0);
//         assertTrue(dr.isFirstPage());
//         assertFalse(dr.isLastPage());
//         // Abutting the end
//         dr.setStartIndex(5);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//         // Overlapping the end
//         dr.setStartIndex(10);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//     }

//     public void testButtonsLow() throws Exception {
//         PagedResults dr = getEstimateTooLowResults();
//         dr.setPageSize(10);
//         // At the beginning (this abuts the estimated end)
//         dr.setStartIndex(0);
//         assertTrue(dr.isFirstPage());
//         assertFalse(dr.isLastPage());
//         // Abutting the end
//         dr.setStartIndex(5);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());
//         // Overlapping the end
//         dr.setStartIndex(10);
//         assertFalse(dr.isFirstPage());
//         assertTrue(dr.isLastPage());

//     }

    /*
    public void testMoveColumnLeft1() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnLeft(0);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnLeft2() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnLeft(1);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnLeft3() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnLeft(2);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnLeft4() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);

        dr.moveColumnLeft(3);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnLeft5() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnLeft(4);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight1() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnRight(0);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight2() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnRight(1);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight3() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);

        dr.moveColumnRight(2);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight4() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnRight(3);
        assertEquals(columns, dr.getColumns());
    }

    public void testMoveColumnRight5() throws Exception {
        PagedResults dr = getExactResults();
        List columns = new LinkedList();
        Column col = new Column();
        col.setName("c1");
        columns.add(col);
        col = new Column();
        col.setName("c2");
        columns.add(col);
        col = new Column();
        col.setName("d1");
        columns.add(col);
        col = new Column();
        col.setName("d2");
        columns.add(col);

        dr.moveColumnRight(4);
        assertEquals(columns, dr.getColumns());
    }
    */
}
