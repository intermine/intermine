package org.flymine.objectstore;

import java.io.Reader;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.xml.Unmarshaller;

import org.flymine.model.testmodel.*;
import org.flymine.objectstore.query.ClassConstraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.util.TypeUtil;

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
        queries.put("WhereClassObject", whereClassObject());
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
        URL mapFile = ObjectStoreQueriesTestCase.class.getClassLoader().getResource("castor_xml_testmodel.xml");
        Mapping map = new Mapping();
        map.loadMapping(mapFile);

        URL testdataUrl = ObjectStoreQueriesTestCase.class.getClassLoader()
            .getResource("test/testmodel_data.xml");

        Reader reader = new FileReader(testdataUrl.getFile());
        Unmarshaller unmarshaller = new Unmarshaller(map);
        List result = (List)unmarshaller.unmarshal(reader);
        map(TypeUtil.flatten(result));
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
        ClassConstraint cc1 = new ClassConstraint(qc1, ClassConstraint.EQUALS, obj);
        Query q1 = new Query();
        q1.addFrom(qc1);
        q1.addToSelect(qc1);
        q1.setConstraint(cc1);
        return q1;
    }

}
