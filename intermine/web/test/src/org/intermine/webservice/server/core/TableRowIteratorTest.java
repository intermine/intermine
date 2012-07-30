package org.intermine.webservice.server.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.StringValueTransformer;
import org.apache.log4j.Logger;
import org.intermine.api.query.MainHelper;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Address;
import org.intermine.model.testmodel.Bank;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Manager;
import org.intermine.model.testmodel.Secretary;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QuerySelectable;
import org.intermine.objectstore.query.Results;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathQuery;
import org.intermine.pathquery.PathQueryBinding;
import org.intermine.util.DynamicUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class TableRowIteratorTest
{
    private static ObjectStoreWriter osw;
    private static final Logger LOG = Logger.getLogger(TableRowIteratorTest.class);
    private static final int COMPANIES = 6;
    private static final int SECRETARIES = 3;
    private static final int DEPARTMENTS = 2;
    private static final int EMPLOYEES = 3;
    
    @BeforeClass
    public static void loadData() throws ObjectStoreException {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        int made = 0;
        try {
            osw.beginTransaction();
            Bank[] banks = new Bank[COMPANIES / 2];
            for (int bi = 0; bi < COMPANIES / 2; bi++) {
                Bank bank = new Bank();
                bank.setName("temp-bank-" + bi);
                osw.store(bank);
                banks[bi] = bank;
                made++;
            }
            for (int k = 0; k < COMPANIES; k++) {
                Company c = (Company) DynamicUtil.createObject(new HashSet(Arrays.asList(Company.class)));
                c.setName("temp-company" + k);
                c.setVatNumber((k + 1) * (k + 1));
                c.setBank(banks[k / 2]);
                osw.store(c);
                made++;
                for (int i = 0; i < SECRETARIES; i++) {
                    Secretary s = new Secretary();
                    s.setName("temp-secretary" + i);
                    osw.store(s);
                    c.addSecretarys(s);
                    osw.store(c);
                }
                for (int i = 0; i < DEPARTMENTS; i++) {
                    Department d = new Department();
                    d.setName(String.format("temp-department-%d-%d", k, i));
                    d.setCompany(c);
                    if ((k % 2 == 0) ? i % 2 == 0 : i % 2 != 0) {
                        Manager m = new Manager();
                        m.setName(String.format("temp-manager-%d-%d", k, i));
                        m.setDepartment(d);
                        d.setManager(m);
                        osw.store(m);
                        made++;
                    }
                    osw.store(d);
                    made++;
                    
                    for (int j = 0; j < EMPLOYEES; j++) {
                        Employee e = new Employee();
                        e.setName(String.format("temp-employee-%d-%d-%d", k, i, j));
                        e.setAge(made + 20);
                        e.setDepartment(d);
                        e.setFullTime(k == 0 || j % k == 0);
                        if (j % 2 == 0) {
                            Address adr = new Address();
                            adr.setAddress(made + " employee st");
                            osw.store(adr);
                            made++;
                            e.setAddress(adr);
                        }
                        osw.store(e);
                        made++;
                    }
                }
            }
            osw.commitTransaction();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            if (osw != null) {
                if (osw.isInTransaction()) {
                    osw.abortTransaction();
                }
                osw.close();
            }
        }
        puts("[START UP] Made %d employees\n", made);
    }

    private EitherVisitor<TableCell, SubTable, Map<String, Object>> jsonTransformer
        = new EitherVisitor<TableCell, SubTable, Map<String, Object>>() {

            @Override
            public Map<String, Object> visitLeft(TableCell a) {
                Map<String, Object> cell = new HashMap<String, Object>();
                cell.put("value", a.getField());
                cell.put("id", a.getId());
                cell.put("type", a.getType());
                cell.put("column", a.getPath().getNoConstraintsString());
                return cell;
            }

            @Override
            public Map<String, Object> visitRight(SubTable b) {
                Map<String, Object> st = new HashMap<String, Object>();
                st.put("collection", b.getJoinPath().getNoConstraintsString());
                st.put("columns", CollectionUtils.collect(b.getColumns(), StringValueTransformer.getInstance()));
                List<List<Map<String, Object>>> rows = new ArrayList<List<Map<String, Object>>>();
                st.put("rows", rows);
                for (List<Either<TableCell, SubTable>> items: b.getRows()){
                    List<Map<String, Object>> row = new ArrayList<Map<String, Object>>();
                    rows.add(row);
                    for (Either<TableCell, SubTable> item: items) {
                        row.add(item.accept(this));
                    }
                }
                return st;
            }
        
    };

    private  EitherVisitor<TableCell, SubTable, Void> printer = new IndentingPrinter(4);
    
    @Before
    public void setup() throws ObjectStoreException {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
    }
    
    @After
    public void teardown() throws ObjectStoreException {
        if (osw != null) {
            osw.close();
        }
    }
    
    @AfterClass
    public static void shutdown() {
        int deleted = 0;
        try {
            osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        } catch (Exception e) {
            System.err.println("Error connecting to DB");
            System.err.println(e);
        }
        if (osw != null) {
            try {
                osw.beginTransaction();
                PathQuery pq = new PathQuery(osw.getModel());
                pq.addView("Company.id");
                pq.addConstraint(Constraints.eq("Company.name", "temp*"));

                Query q = MainHelper.makeQuery(
                        pq, new HashMap(), new HashMap(), null, new HashMap());

                Results res = osw.execute(q, 50000, true, false, true);
                Set<Bank> banks = new HashSet<Bank>();
                for (Object row: res) {
                    Company c = (Company) ((List) row).get(0);
                    for (Secretary s: c.getSecretarys()) {
                        osw.delete(s);
                        deleted++;
                    }
                    if (c.getBank() != null) {
                        banks.add(c.getBank());
                    }
                    for (Department dep: c.getDepartments()) {
                        if (dep.getManager() != null) {
                            osw.delete(dep.getManager());
                            deleted++;
                        }
                        for (Employee e: dep.getEmployees()) {
                            if (e.getAddress() != null) {
                                osw.delete(e.getAddress());
                                deleted++;
                            }
                            osw.delete(e);
                            deleted++;
                        }
                        osw.delete(dep);
                        deleted++;
                    }
                    osw.delete(c);
                    deleted++;
                }
                for (Bank b: banks) {
                    osw.delete(b);
                    deleted++;
                }
                
                osw.commitTransaction();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }finally {
                try {
                    osw.close();
                } catch (Exception e) {
                    System.err.print(e);
                }
            }
        }
        puts("\n[CLEAN UP] Deleted %d things", deleted);
    }

    /* VISITORS WE WILL BE USING... */
    private static class IndentingPrinter extends EitherVisitor<TableCell, SubTable, Void> {
        
        int indent = 0;
        int depth = 0;
        String spacer = null;
        
        IndentingPrinter(int indent) {
            this.indent = indent;
            this.spacer = "";
        }
        
        private IndentingPrinter(int indent, int depth) {
            this.indent = indent;
            this.depth = depth;
            this.spacer = String.format("%" + ((depth == 0) ? "" : indent * depth) + "s", " ");
        }

        @Override
        public Void visitLeft(TableCell a) {
            puts(spacer + "%s: %s", a.getPath(), a.getField());
            return null;
        }

        @Override
        public Void visitRight(SubTable b) {
            puts(spacer + "%d %s", b.getRows().size(), b.getJoinPath());
            puts(spacer + "  " + b.getColumns());
            int c = 0;
            for (List<Either<TableCell, SubTable>> row: b.getRows()) {
                puts(spacer + "  %s %d",
                        b.getJoinPath().getLastClassDescriptor().getUnqualifiedName(), c++);
                for (Either<TableCell, SubTable> item: row) {
                    item.accept(new IndentingPrinter(indent, depth + 1));
                }
            }
            return null;
        }
    };
    
    private static final EitherVisitor<TableCell, SubTable, Integer> deepCounter = new EitherVisitor<TableCell, SubTable, Integer>() {

        @Override public Integer visitLeft(TableCell a) { return Integer.valueOf(1); }

        @Override public Integer visitRight(SubTable b) {
            int c = 0;
            for (List<Either<TableCell, SubTable>> row: b.getRows()) {
                for (Either<TableCell, SubTable> item: row) {
                    c += item.accept(this);
                }
            }
            return c;
        }
        
    };

    private static final EitherVisitor<TableCell, SubTable, String> toStringNoTables = new EitherVisitor<TableCell, SubTable, String>() {
        public String visitLeft(TableCell a) { return String.valueOf(a.getField()); }
        public String visitRight(SubTable b) { fail("No subtables expected"); return null; }
    };

    private static final EitherVisitor<TableCell, SubTable, Integer> counterNoTables = new EitherVisitor<TableCell, SubTable, Integer>() {
        public Integer visitLeft(TableCell a) { return Integer.valueOf(1);}
        public Integer visitRight(SubTable b) { fail("No subtables expected"); return null; }
    };

    
    /* AND NOW FOR THE TESTS... */

    /*
     * This test is to test the ability to deal with ungrouped OJGs. This involves changing the order
     * of the view slightly so that outer-joined groups are grouped.
     */
    @Test 
    public void ungroupedOJG() throws ObjectStoreException {
        /*
         *   4 cells * 3
         * + 4 cells * 1
         * --------------
         * = 16
         */
        runQuery("ungroupedOJG", 16, new Page(4, 3));
    }

    @Test
    public void getJSONRecords() throws ObjectStoreException, JSONException {
        PathQuery pq = getPQ("AllOuterJoinedCollections");
        TableRowIterator iter = getResults(pq, new Page(2, 3));

        while(iter.hasNext()) {
            List<Map<String, Object>> data = ListFunctions.map(iter.next(),
                new F<Either<TableCell, SubTable>, Map<String, Object>>() {
                    @Override public Map<String, Object> call(Either<TableCell, SubTable> a) {
                        return a.accept(jsonTransformer);
                    }
            });
            JSONArray ja = new JSONArray(data);
            puts(ja.toString(2));
        }
    }

    @Test
    public void allInnerJoined() throws ObjectStoreException {
        PathQuery pq = getPQ("allInnerJoined");
        TableRowIterator iter = getResults(pq, new Page(2, 3));

        // We expect:
        String[] values = new String[] {
          "temp-company0", "temp-department-0-0", "temp-employee-0-0-0", "26",
          "6 employee st", "temp-secretary2", "1"
        };

        // We get:
        List<Either<TableCell, SubTable>> row = iter.next();

        // Same size
        assertEquals(values.length, row.size());

        // Same content
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], row.get(i).accept(toStringNoTables));
        }

        // For all the rows in the set.
        int c = 0;
        while (iter.hasNext()) {
            for (Either<TableCell, SubTable> o: iter.next()) {
                c += o.accept(counterNoTables);
            }
        }
        assertEquals(c, 14);
    }

    @Test public void someOuterJoinedReferences() throws ObjectStoreException {
        runQuery("someOuterJoinedReferences", 24);
    }
    
    @Test public void nestedOuterJoinedReferences() throws ObjectStoreException {
        runQuery("nestedOuterJoinedReferences", 24);
    }
    
    @Test public void outerJoinRefWithInnerJoinOnIt() throws ObjectStoreException {
        /*
         *   3 cells * 3 (E.{name,age,fullTime})
         * + 4 cells * 1 (E.d.name, E.d.m.name, E.d.c.{name,vatNumber})
         * -----------------
         *   13
         */
        runQuery("outerJoinRefWithInnerJoinOnIt", 13, new Page(4, 3));
    }
    
    @Test public void allOuterJoinedCollections() throws ObjectStoreException {
        /* Per company:
         *  - 1 name
         *  - 2 departments
         *    - 1 name
         *    - 3.5 employees per department (every second has a manager)
         *      - 3 fields per employee.
         *  - 3 secretary names
         *  - 1 VAT Number
         * times 3 companies.
         */
        runQuery("AllOuterJoinedCollections", 84);
        
    }

    @Test public void refsFirst() throws ObjectStoreException {
        /* Per company:
        *  - 1 name
        *  - 2 departments
        *    - 1 name
        *    - 3.5 employees per department (every second has a manager)
        *      - 3 fields per employee.
        *  - 3 secretary names
        *  - 1 VAT Number
        * times 3 companies.
        */
        runQuery("refsFirst", 84);
    }
    
    @Test public void noTopLevel() throws ObjectStoreException {
        /* Per company:
         *  - 2 departments
         *    - 1 name
         *    - 3.5 employees per department (every second has a manager)
         *      - 3 fields per employee.
         *  - 3 secretary names
         * times 3 companies.
         */
        runQuery("noTopLevel", 78);
    }

    @Test public void noTopLevelReversed() throws ObjectStoreException {
        /* Per company:
         *  - 2 departments
         *    - 1 name
         *    - 3.5 employees per department (every second has a manager)
         *      - 3 fields per employee.
         *  - 3 secretary names
         * times 3 companies.
         */
        runQuery("noTopLevelReversed", 78);
    }
    
    /* Tests an outer join reference on an inner join collection. */
    @Test
    public void ticket2936() throws ObjectStoreException {
    	runQuery("ticket2936", 12);
    }
    
    /*
     * Not totally sure what the error was - likely something to do with the fact
     * that outerjoined subtables can themselves be totally null if their underlying
     * join object is null. I think.
     */
    @Test
    public void IrinaBug() throws ObjectStoreException {
        runQuery("irinaBug", 24, new Page(5, 3));
    }

    /* Tests an inner join references on an outer join reference. */
    @Test
    public void innerRefOnOuterRef() throws ObjectStoreException {
        /*
         *   4 cells * 3
         * + 3 cells * 1
         * --------------
         * = 15  
         */
    	runQuery("itWasWorking", 15, new Page(4, 3));
    }
    
    @Test
    public void NoSuchElement() throws ObjectStoreException {
        // NOTE THAT THE STRUCTURE OF THESE RESULTS IS MENTAL. FIX THIS.
        runQuery("noSuchElement", 21);
    }
    
    @Test
    public void GettingEffectiveView() throws ObjectStoreException {
        PathQuery pq = getPQ("ungroupedOJG");
        TableRowIterator iter = getResults(pq, new Page(2, 3));
        List<String> expected = Arrays.asList(
            "Employee.name", "Employee.department.name", "Employee.department.manager.name",
            "Employee.department.company.name", "Employee.department.manager.title", "Employee.fullTime",
            "Employee.address.address", "Employee.age");
        List<String> orig = pq.getView();
        List<String> rejiggered = ListFunctions.map(iter.getEffectiveView(), new F<Path, String>() {
            public String call(Path p) {
                return p.getNoConstraintsString();
            }
        });
        puts(orig);
        puts(rejiggered);
        // Have the same dimensions.
        assertEquals(orig.size(), rejiggered.size());
        // Have all the same elements.
        assertEquals(new HashSet<String>(orig), new HashSet<String>(rejiggered));
        // And they are in a suitable order.
        assertEquals(expected, rejiggered);
    }

    
    /* THE ACTUAL TEST RUNNING INFRASTRUCTURE */

    private static void puts(Object s) {
        System.out.println("[DEBUG] " + String.valueOf(s));
    }

    private static void puts(String fmt, Object... params) {
        puts(String.format(fmt, params));
    }

    private void runQuery(String name, int expected) throws ObjectStoreException {
        runQuery(name, expected, new Page(2, 3));
    }
    
    private PathQuery getPQ(String name) {
        return Queries.getPathQuery("TableRowIteratorTest." + name);
    }

    private TableRowIterator getResults(final PathQuery pq, final Page page)  throws ObjectStoreException {
        Map<String, QuerySelectable> p2qn = new HashMap<String, QuerySelectable>();
        Query q = MainHelper.makeQuery(pq, new HashMap(), p2qn, null, new HashMap());

        Results res = osw.execute(q, 1000, true, false, true);
        //puts("EXAMPLE ROW: " + res.get(0));
        return new TableRowIterator(pq, q, res, p2qn, page, null);
    }

    private void runQuery(String name, int expected, Page page) throws ObjectStoreException {
        puts("Running test: " + name);
        puts("=============================");
        PathQuery pq = getPQ(name);
        TableRowIterator iter = getResults(pq, page);
        int c = 0, r = page.getStart();
        for (List<Either<TableCell, SubTable>> row: iter) {
            puts("Row " + r++);
            for (Either<TableCell, SubTable> ro: row) {
                c += ro.accept(printer.and(deepCounter));
            }
            puts("-----");
        }
		
        assertEquals(expected, c);
    }
}
