package org.flymine.objectstore;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.xml.sax.InputSource;

import org.flymine.model.testmodel.*;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.ClassConstraint;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.QueryReference;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.util.XmlBinding;

public abstract class SetupDataTestCase extends ObjectStoreQueriesTestCase
{
    protected static ObjectStoreWriter writer;
    protected static Map data = new LinkedHashMap();

    public SetupDataTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        ObjectStoreQueriesTestCase.oneTimeSetUp();
        writer = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        setUpData();
        storeData();
        // These queries are here because they require objects with IDs
        queries.put("WhereClassObject", whereClassObject());
        queries.put("SelectClassObjectSubquery", selectClassObjectSubquery());
        queries.put("BagConstraint2", bagConstraint2());
    }

    public static void oneTimeTearDown() throws Exception {
        ObjectStoreQueriesTestCase.oneTimeTearDown();
        removeDataFromStore();
    }

    /**
     * Set up any data needed
     *
     * @throws Exception if an error occurs
     */
    public static void storeData() throws Exception {
        System.out.println("Storing data");
        if (writer == null) {
            throw new NullPointerException("writer must be set before trying to store data");
        }
        long start = new Date().getTime();
        try {
            writer.beginTransaction();
            Iterator iter = data.keySet().iterator();
            while (iter.hasNext()) {
                writer.store(data.get(iter.next()));
            }
            writer.commitTransaction();
        } catch (Exception e) {
            writer.abortTransaction();
            throw new Exception(e);
        }

        Database db = DatabaseFactory.getDatabase("db.unittest");
        java.sql.Connection con = db.getConnection();
        java.sql.Statement s = con.createStatement();
        con.setAutoCommit(true);
        s.execute("vacuum analyze");
        con.close();
        System.out.println("Took " + (new Date().getTime() - start) + " ms to set up data and VACUUM ANALYZE");
    }

    public static void removeDataFromStore() throws Exception {
        System.out.println("Removing data");
        if (writer == null) {
            throw new NullPointerException("writer must be set before trying to store data");
        }
        try {
            writer.beginTransaction();
            Iterator iter = data.keySet().iterator();
            while (iter.hasNext()) {
                writer.delete(data.get(iter.next()));
            }
            writer.commitTransaction();
        } catch (Exception e) {
            writer.abortTransaction();
            throw new Exception(e);
        }
    }

    public static void setUpData() throws Exception {
        XmlBinding binding = new XmlBinding("castor_xml_testmodel.xml");
        map((List) binding.unmarshal(new InputSource(SetupDataTestCase.class.getClassLoader().getResourceAsStream("test/testmodel_data.xml"))));
    }

    private static void map(Collection c) throws Exception {
        Iterator iter = c.iterator();
        while(iter.hasNext()) {
            Object o = iter.next();
            Method name = null;
            try {
                name = o.getClass().getMethod("getName", new Class[] {});
            } catch (Exception e) {}
            if(name!=null) {
                data.put((String)name.invoke(o, new Object[] {}), o);
            } else {
                data.put(new Integer(o.hashCode()), o);
            }
        }
    }

    /*
      select company,
      from Company
      where c1 = <company object>
    */
    public static Query whereClassObject() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        Object obj = data.get("CompanyA");
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, obj);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.setConstraint(cc1);
        return q1;
    }

    /*
      select company,
      from Company, Department
      where c1 = <company object>
      and Company.departments = Department
      and Department CONTAINS (select department
                               from Department
                               where department = <department object>)
    */
    public static Query selectClassObjectSubquery() throws Exception {
        QueryClass qc1 = new QueryClass(Company.class);
        QueryClass qc2 = new QueryClass(Department.class);
        Object obj1 = data.get("CompanyA");
        ConstraintSet cs1 = new ConstraintSet(ConstraintSet.AND);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addFrom(qc2);
        q1.addToSelect(qc1);
        ClassConstraint cc1 = new ClassConstraint(qc1, ConstraintOp.EQUALS, obj1);
        cs1.addConstraint(cc1);
        QueryReference qr1 = new QueryCollectionReference(qc1, "departments");
        ContainsConstraint con1 = new ContainsConstraint(qr1, ContainsConstraint.CONTAINS, qc2);
        cs1.addConstraint(con1);

        Query subquery = new Query();
        QueryClass qc3 = new QueryClass(Department.class);
        Object obj2 = data.get("DepartmentA1");
        ClassConstraint cc2 = new ClassConstraint(qc3, ConstraintOp.EQUALS, obj2);
        subquery.addFrom(qc3);
        subquery.addToSelect(qc3);
        subquery.setConstraint(cc2);
        SubqueryConstraint sc1 = new SubqueryConstraint(subquery, SubqueryConstraint.CONTAINS, qc2);
        cs1.addConstraint(sc1);
        q1.setConstraint(cs1);
        return q1;
    }

    /*
      select Company
      from Company
      where Company in ("hello", "goodbye")
    */
    public static Query bagConstraint2() throws Exception {
        QueryClass c1 = new QueryClass(Company.class);
        Query q1 = new Query();
        q1.alias(c1, "Company");
        q1.addFrom(c1);
        q1.addToSelect(c1);
        HashSet set = new HashSet();
        set.add("hello");
        set.add("goodbye");
        set.add("CompanyA");
        set.add(data.get("CompanyA"));
        set.add(new Integer(5));
        q1.setConstraint(new BagConstraint(c1, set));
        return q1;
    }
}
