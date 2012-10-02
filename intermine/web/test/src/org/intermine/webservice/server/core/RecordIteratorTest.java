package org.intermine.webservice.server.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.StringValueTransformer;
import org.apache.log4j.Logger;
import org.intermine.api.query.MainHelper;
import org.intermine.metadata.Model;
import org.intermine.model.testmodel.Address;
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
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RecordIteratorTest
{
    private static ObjectStoreWriter osw;
    private static final Logger LOG = Logger.getLogger(RecordIteratorTest.class);
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
            for (int k = 0; k < COMPANIES; k++) {
                Company c = (Company) DynamicUtil.createObject(new HashSet(Arrays.asList(Company.class)));
                c.setName("temp-company" + k);
                c.setVatNumber((k + 1) * (k + 1));
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
        System.out.printf("[START UP] Made %d employees\n", made);
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
                for (Object row: res) {
                    Company c = (Company) ((List) row).get(0);
                    for (Secretary s: c.getSecretarys()) {
                        osw.delete(s);
                        deleted++;
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
        System.out.printf("\n[CLEAN UP] Deleted %d things\n", deleted);
    }

    private PathQuery getPQ() {
        PathQuery pq = new PathQuery(Model.getInstanceByName("testmodel"));

        pq.addViews(
            "Company.name",
            "Company.departments.name",
            "Company.departments.employees.name",
            "Company.departments.employees.age",
            "Company.departments.employees.address.address",
            "Company.secretarys.name",
            "Company.vatNumber");
        return pq;
    }
    
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
            System.out.printf(spacer + "%s: %s\n", a.getPath().getEndFieldDescriptor().getName(), a.getField());
            return null;
        }

        @Override
        public Void visitRight(SubTable b) {
            System.out.printf(spacer + "%d %s\n", b.getRows().size(), b.getJoinPath());
            System.out.println(spacer + "  " + b.getColumns());
            int c = 0;
            for (List<Either<TableCell, SubTable>> row: b.getRows()) {
                System.out.printf(spacer + "  %s %d\n",
                        b.getJoinPath().getLastClassDescriptor().getUnqualifiedName(), c++);
                for (Either<TableCell, SubTable> item: row) {
                    item.accept(new IndentingPrinter(indent, depth + 1));
                }
            }
            return null;
        }
        
    };

    @Test
    public void getJSONRecords() throws ObjectStoreException, JSONException {
        PathQuery pq = getPQ();
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees.address", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.secretarys", OuterJoinStatus.OUTER);

        Map<String, QuerySelectable> p2qn = new HashMap<String, QuerySelectable>();
        Query q = MainHelper.makeQuery(pq, new HashMap(), p2qn, null, new HashMap());

        Results res = osw.execute(q, 1000, true, false, true);

        TableRowIterator iter = new TableRowIterator(pq, q, res, p2qn, new Page(2, 3), null);

        EitherVisitor<TableCell, SubTable, Void> printer = new IndentingPrinter(4);
        while(iter.hasNext()) {
            Collection data = CollectionUtils.collect(iter.next(),
                new Transformer() {
                    @Override
                    public Object transform(Object arg0) {
                        Either<TableCell, SubTable> item = (Either<TableCell, SubTable>) arg0;
                        return item.accept(jsonTransformer);
                    }
                });
            JSONArray ja = new JSONArray(data);
            System.out.println(ja.toString(2));
        }
    }
    
    @Test
    public void allInnerJoined() throws ObjectStoreException {
        PathQuery pq = getPQ();

        Map<String, QuerySelectable> p2qn = new HashMap<String, QuerySelectable>();
        Query q = MainHelper.makeQuery(pq, new HashMap(), p2qn, null, new HashMap());

        Results res = osw.execute(q, 1000, true, false, true);

        TableRowIterator iter = new TableRowIterator(pq, q, res, p2qn, new Page(2, 3), null);
        
        List<Either<TableCell, SubTable>> row = iter.next();
        String[] values = new String[] {
          "temp-company0", "temp-department-0-0", "temp-employee-0-0-0", "23",
          "3 employee st", "temp-secretary2", "1"
        };
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i],
                row.get(i).accept(new EitherVisitor<TableCell, SubTable, String>() {
                    public String visitLeft(TableCell a) { return String.valueOf(a.getField()); }
                    public String visitRight(SubTable b) { fail("No subtables expected"); return null; }
                })
            );
        }
        int c = 0;
        while (iter.hasNext()) {
            for (Either<TableCell, SubTable> o: iter.next()) {
                c += o.accept(new EitherVisitor<TableCell, SubTable, Integer>() {
                    public Integer visitLeft(TableCell a) {return 1;}
                    public Integer visitRight(SubTable b) { fail("No subtables expected"); return null; }
                });
            }
        }
        assertEquals(c, 14);
    }
    
    EitherVisitor<TableCell, SubTable, Integer> deepCounter = new EitherVisitor<TableCell, SubTable, Integer>() {

        @Override public Integer visitLeft(TableCell a) { return 1; }

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
    
    @Test public void allOuterJoinedReferences() throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        pq.addViews(
                "Employee.name",
                "Employee.department.name",
                "Employee.department.manager.name",
                "Employee.department.company.name",
                "Employee.department.company.vatNumber",
                "Employee.fullTime",
                "Employee.address.address");
        pq.setOuterJoinStatus("Employee.department", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Employee.department.manager", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Employee.department.company", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Employee.address", OuterJoinStatus.OUTER);
        
        Map<String, QuerySelectable> p2qn = new HashMap<String, QuerySelectable>();
        Query q = MainHelper.makeQuery(pq, new HashMap(), p2qn, null, new HashMap());

        Results res = osw.execute(q, 1000, true, false, true);
        TableRowIterator iter = new TableRowIterator(pq, q, res, p2qn, new Page(2, 10), null);
        int c = 0;
        while (iter.hasNext()) {
            List<Either<TableCell, SubTable>> row = iter.next();
            //System.out.println();
            for (Either<TableCell, SubTable> ro: row) {
                c += ro.accept(deepCounter); //printer.and(deepCounter));
            }
        }
        
         /* 
          * 7 fields in 10 rows
         */
        assertEquals(70, c);
    }
    
    @Test public void outerJoinRefWithInnerJoinOnIt() throws ObjectStoreException {
        PathQuery pq = new PathQuery(osw.getModel());
        
        pq.addViews(
                "Employee.name",
                "Employee.department.name",
                "Employee.department.manager.name",
                "Employee.department.company.name",
                "Employee.department.company.vatNumber",
                "Employee.fullTime",
                "Employee.address.address");
        pq.setOuterJoinStatus("Employee.department", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Employee.department.company", OuterJoinStatus.OUTER);

        Map<String, QuerySelectable> p2qn = new HashMap<String, QuerySelectable>();
        Query q = MainHelper.makeQuery(pq, new HashMap(), p2qn, null, new HashMap());

        Results res = osw.execute(q, 1000, true, false, true);
        
        TableRowIterator iter = new TableRowIterator(pq, q, res, p2qn, new Page(2, 6), null);
        int c = 0;
        while (iter.hasNext()) {
            List<Either<TableCell, SubTable>> row = iter.next();
            System.out.println();
            for (Either<TableCell, SubTable> ro: row) {
                c += ro.accept(printer.and(deepCounter));
            }
        }
        
         /* 
          * 3 fields always present in 6 rows
          * 4 fields contingently present if there is a manager (present in two rows)
          * 
         */
        assertEquals(26, c);
    }
    
    @Test public void allOuterJoinedCollections() throws ObjectStoreException {
        
        PathQuery pq = getPQ();
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees.address", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.secretarys", OuterJoinStatus.OUTER);

        Map<String, QuerySelectable> p2qn = new HashMap<String, QuerySelectable>();
        Query q = MainHelper.makeQuery(pq, new HashMap(), p2qn, null, new HashMap());

        Results res = osw.execute(q, 1000, true, false, true);

        TableRowIterator iter = new TableRowIterator(pq, q, res, p2qn, new Page(2, 3), null);
        int c = 0;
        for (List<Either<TableCell, SubTable>> row: iter) {
            for (Either<TableCell, SubTable> ro: row) {
                c += ro.accept(printer.and(deepCounter));
            }
        }
       
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
        assertEquals(84, c);
    }
    
    @Test public void refsFirst() throws ObjectStoreException {
        
        PathQuery pq = new PathQuery(Model.getInstanceByName("testmodel"));

        pq.addViews(
            "Company.departments.name",
            "Company.departments.employees.address.address",
            "Company.departments.employees.name",
            "Company.departments.employees.age",
            "Company.secretarys.name",
            "Company.name",
            "Company.vatNumber");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees.address", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.secretarys", OuterJoinStatus.OUTER);

        Map<String, QuerySelectable> p2qn = new HashMap<String, QuerySelectable>();
        Query q = MainHelper.makeQuery(pq, new HashMap(), p2qn, null, new HashMap());

        Results res = osw.execute(q, 1000, true, false, true);
        System.out.println(res);
        TableRowIterator iter = new TableRowIterator(pq, q ,res, p2qn, new Page(2, 3), null);
        int c = 0;
        for (List<Either<TableCell, SubTable>> row: iter) {
            for (Either<TableCell, SubTable> ro: row) {
                c += ro.accept(printer.and(deepCounter));
            }
        }
       
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
        assertEquals(84, c);
    }
    
    @Test public void noTopLevel() throws ObjectStoreException {
        
        PathQuery pq = new PathQuery(Model.getInstanceByName("testmodel"));

        pq.addViews(
            "Company.departments.name",
            "Company.departments.employees.address.address",
            "Company.departments.employees.name",
            "Company.departments.employees.age",
            "Company.secretarys.name");
        pq.setOuterJoinStatus("Company.departments", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.departments.employees.address", OuterJoinStatus.OUTER);
        pq.setOuterJoinStatus("Company.secretarys", OuterJoinStatus.OUTER);

        Map<String, QuerySelectable> p2qn = new HashMap<String, QuerySelectable>();
        Query q = MainHelper.makeQuery(pq, new HashMap(), p2qn, null, new HashMap());

        Results res = osw.execute(q, 1000, true, false, true);
        System.out.println(res);
        TableRowIterator iter = new TableRowIterator(pq, q, res, p2qn, new Page(2, 3), null);
        int c = 0;
        for (List<Either<TableCell, SubTable>> row: iter) {
            for (Either<TableCell, SubTable> ro: row) {
                c += ro.accept(printer.and(deepCounter));
            }
        }
       
         /* Per company:
         *  - 2 departments
         *    - 1 name
         *    - 3.5 employees per department (every second has a manager)
         *      - 3 fields per employee.
         *  - 3 secretary names
         * times 3 companies.
         */
        assertEquals(78, c);
    }
}
