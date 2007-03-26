package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCloner;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

public class ObjectStorePrecomputedTablesTest extends TestCase
{
    public ObjectStorePrecomputedTablesTest(String arg) throws Exception {
        super(arg);
    }

    public void testCreatePrecomputedTable() throws Exception {
        doTestCreatePrecomputedTable(7);
    }

    public void testCreatePrecomputedTableNegatives() throws Exception {
        doTestCreatePrecomputedTable(2207);
    }

    public void doTestCreatePrecomputedTable(int sequenceMillions) throws Exception {
        ObjectStoreWriterInterMineImpl writer = (ObjectStoreWriterInterMineImpl)
            ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) writer.getObjectStore();
        writer.sequenceBase = sequenceMillions * 1000000;
        writer.sequenceOffset = 0;
        List toRemove = new ArrayList();
        try {
            writer.beginTransaction();
            for (int i = 0; i < 200; i++) {
                Department d = new Department();
                d.setName("Flibble" + i);
                Employee e = new Employee();
                e.setName("Fred" + i);
                writer.store(d);
                writer.store(e);
                toRemove.add(d);
                toRemove.add(e);
            }
            writer.commitTransaction();
            Connection c = os.getConnection();
            c.createStatement().execute("ANALYSE");
            os.releaseConnection(c);
            Query q1 = new Query();
            QueryClass qc1 = new QueryClass(Department.class);
            QueryClass qc2 = new QueryClass(Employee.class);
            q1.addFrom(qc1);
            q1.addFrom(qc2);
            q1.addToSelect(qc1);
            q1.addToSelect(qc2);
            q1.setDistinct(false);
            long time1 = System.currentTimeMillis();
            Results r1 = os.execute(q1);
            r1.setBatchSize(1000);
            r1.setNoExplain();
            int counter = 0;
            Iterator rowIter = r1.iterator();
            while (rowIter.hasNext()) {
                ResultsRow row = (ResultsRow) rowIter.next();
                Department d = (Department) row.get(0);
                Employee e = (Employee) row.get(1);
                assertEquals("Row " + counter + ", Flibble", "Flibble" + (counter / 200), d.getName());
                assertEquals("Row " + counter + ", Fred", "Fred" + (counter % 200), e.getName());
                counter++;
            }
            assertEquals(40000, counter);
            Query q2 = QueryCloner.cloneQuery(q1);
            long time2 = System.currentTimeMillis();
            System.out.println("Access to results took " + (time2 - time1) + " ms");
            ((ObjectStoreInterMineImpl) os).precompute(q2, "test");
            long time3 = System.currentTimeMillis();
            System.out.println("Precomputing took " + (time3 - time2) + " ms");
            Results r2 = os.execute(q2);
            r2.setBatchSize(1000);
            r2.setNoExplain();
            counter = 0;
            rowIter = r2.iterator();
            while (rowIter.hasNext()) {
                ResultsRow row = (ResultsRow) rowIter.next();
                Department d = (Department) row.get(0);
                Employee e = (Employee) row.get(1);
                assertEquals("Row " + counter + ", Flibble", "Flibble" + (counter / 200), d.getName());
                assertEquals("Row " + counter + ", Fred", "Fred" + (counter % 200), e.getName());
                counter++;
            }
            assertEquals(40000, counter);
            long time4 = System.currentTimeMillis();
            System.out.println("Access to precomputed results took " + (time4 - time3) + " ms");
            //Thread.sleep(200000);
            Department newD = new Department();
            newD.setName("Flibble200");
            writer.store(newD);
            toRemove.add(newD);
            Query q3 = QueryCloner.cloneQuery(q1);
            Results r3 = os.execute(q3);
            r3.setBatchSize(1000);
            r3.setNoExplain();
            counter = 0;
            rowIter = r3.iterator();
            while (rowIter.hasNext()) {
                ResultsRow row = (ResultsRow) rowIter.next();
                Department d = (Department) row.get(0);
                Employee e = (Employee) row.get(1);
                assertEquals("Row " + counter + ", Flibble", "Flibble" + (counter / 200), d.getName());
                assertEquals("Row " + counter + ", Fred", "Fred" + (counter % 200), e.getName());
                counter++;
            }
            assertEquals(40200, counter);
            long time5 = System.currentTimeMillis();
            System.out.println("Access to modified results took " + (time5 - time4) + " ms");
        } finally {
            writer.beginTransaction();
            Iterator iter = toRemove.iterator();
            while (iter.hasNext()) {
                writer.delete((InterMineObject) iter.next());
            }
            writer.commitTransaction();
            writer.close();
        }
    }
}


