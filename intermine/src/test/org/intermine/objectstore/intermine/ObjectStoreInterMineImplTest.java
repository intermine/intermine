package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreAbstractImplTestCase;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;

public class ObjectStoreInterMineImplTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        os = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
    }

    public ObjectStoreInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreInterMineImplTest.class);
    }

    public void testLargeOffset() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        Query q2 = QueryCloner.cloneQuery(q);
        SingletonResults r = new SingletonResults(q, os, os.getSequence());
        r.setBatchSize(2);
        InterMineObject o = (InterMineObject) r.get(5);
        SqlGenerator.registerOffset(q2, 6, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId());
        SingletonResults r2 = new SingletonResults(q2, os, os.getSequence());
        r2.setBatchSize(2);

        Query q3 = QueryCloner.cloneQuery(q);
        SqlGenerator.registerOffset(q3, 5, ((ObjectStoreInterMineImpl) os).getSchema(), ((ObjectStoreInterMineImpl) os).db, o.getId());
        SingletonResults r3 = new SingletonResults(q3, os, os.getSequence());
        r3.setBatchSize(2);

        assertEquals(r, r2);
        assertTrue(!r.equals(r3));
    }

    public void testPrecompute() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Department.class);
        QueryClass qc2 = new QueryClass(Employee.class);
        q.addFrom(qc1);
        q.addFrom(qc2);
        q.addToSelect(qc1);
        q.addToSelect(qc2);
        QueryField f1 = new QueryField(qc1, "name");
        QueryField f2 = new QueryField(qc2, "name");
        q.addToSelect(f1);
        q.addToSelect(f2);
        q.setConstraint(new ContainsConstraint(new QueryCollectionReference(qc1, "employees"), ConstraintOp.CONTAINS, qc2));
        q.setDistinct(false);
        Set indexes = new LinkedHashSet();
        indexes.add(qc1);
        indexes.add(f1);
        indexes.add(f2);
        String tableName = ((ObjectStoreInterMineImpl) os).precompute(q, indexes);
        Connection con = null;
        Map indexMap = new HashMap();
        try {
            con = ((ObjectStoreInterMineImpl) os).getConnection();
            Statement s = con.createStatement();
            ResultSet r = s.executeQuery("SELECT * FROM pg_indexes WHERE tablename = '" + tableName + "'");
            while (r.next()) {
                indexMap.put(r.getString("indexname"), r.getString("indexdef"));
            }
        } finally {
            if (con != null) {
                ((ObjectStoreInterMineImpl) os).releaseConnection(con);
            }
        }
        Map expectedIndexMap = new HashMap();
        expectedIndexMap.put("index" + tableName + "_field_a1_id__a3___a4_", "CREATE INDEX index" + tableName + "_field_a1_id__a3___a4_ ON " + tableName + " USING btree (a1_id, a3_, a4_)");
        expectedIndexMap.put("index" + tableName + "_field_a3_", "CREATE INDEX index" + tableName + "_field_a3_ ON " + tableName + " USING btree (a3_)");
        expectedIndexMap.put("index" + tableName + "_field_a4_", "CREATE INDEX index" + tableName + "_field_a4_ ON " + tableName + " USING btree (a4_)");
        assertEquals(expectedIndexMap, indexMap);
    }
}

