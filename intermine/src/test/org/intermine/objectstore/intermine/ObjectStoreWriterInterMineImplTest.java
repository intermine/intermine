package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.Iterator;
import java.sql.Connection;

import junit.framework.Test;

import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreWriterTestCase;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

import org.apache.log4j.Logger;

public class ObjectStoreWriterInterMineImplTest extends ObjectStoreWriterTestCase
{
    private static final Logger LOG = Logger.getLogger(ObjectStoreInterMineImpl.class);

    public static void oneTimeSetUp() throws Exception {
        writer = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        ObjectStoreWriterTestCase.oneTimeSetUp();
    }

    public static void oneTimeTearDown() throws Exception {
        ObjectStoreWriterTestCase.oneTimeTearDown();
        writer.close();
    }

    public ObjectStoreWriterInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreWriterInterMineImplTest.class);
    }

    /*
    public static void testLargeQuantitiesOfStuff() throws Exception {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 10000; i++) {
            sb.append("lkjhaskjfhlsdakf hsdkljf hasdlkf sakf daslhf dskhf ldskf dslkf sdlkf alskf");
        }
        try {
            {
                writer.beginTransaction();
                //Company cycle[] = new Company[10];
                int count = 0;
                for (int i = 0; i < 1000; i++) {
                    Company fred = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
                    //Employee fred = new Employee();
                    fred.setName(sb.toString() + i);
                    writer.store(fred);
                    count++;
                    //cycle[count % 10] = fred;
                    if ((count % 10) == 0) {
                        LOG.info("Writing Companies - done " + count);
                    }
                }
                //cycle = null;
                writer.commitTransaction();
                Connection con = ((ObjectStoreWriterInterMineImpl) writer).getConnection();
                con.createStatement().execute("analyse");
                ((ObjectStoreWriterInterMineImpl) writer).releaseConnection(con);
            }

            Query q = new Query();
            //QueryClass qc = new QueryClass(Employee.class);
            QueryClass qc = new QueryClass(Company.class);
            q.addFrom(qc);
            q.addToSelect(qc);
            q.setConstraint(new SimpleConstraint(new QueryField(qc, "name"), ConstraintOp.MATCHES, new QueryValue("lkjhask%")));

            {
                SingletonResults employees = new SingletonResults(q, writer.getObjectStore(), writer.getObjectStore().getSequence());
                employees.setNoExplain();
                employees.setBatchSize(20);
                writer.beginTransaction();
                int count = 0;
                Iterator empIter = employees.iterator();
                while (empIter.hasNext()) {
                    Company c = (Company) empIter.next();
                    //Employee e = (Employee) empIter.next();
                    //Company c = (Company) DynamicUtil.createObject(Collections.singleton(Company.class));
                    //c.setName(e.getName());
                    c.setVatNumber(count);
                    //e.setAge(count);
                    writer.store(c);
                    count++;
                    if ((count % 10) == 0) {
                        LOG.info("Altering Companies - done " + count);
                    }
                }
                writer.commitTransaction();
            }

            {
                SingletonResults employees = new SingletonResults(q, writer.getObjectStore(), writer.getObjectStore().getSequence());
                employees.setNoExplain();
                employees.setBatchSize(20);
                writer.beginTransaction();
                int count = 0;
                Iterator empIter = employees.iterator();
                while (empIter.hasNext()) {
                    writer.delete((InterMineObject) empIter.next());
                    count++;
                    if ((count % 10) == 0) {
                        LOG.info("Deleting Companies - done " + count);
                    }
                }

                //Query q2 = new Query();
                //QueryClass qc2 = new QueryClass(Company.class);
                //q2.addFrom(qc2);
                //q2.addToSelect(qc2);
                //q2.setConstraint(new SimpleConstraint(new QueryField(qc2, "name"), ConstraintOp.MATCHES, new QueryValue("Fred %")));
                //SingletonResults companies = new SingletonResults(q2, writer.getObjectStore(), writer.getObjectStore().getSequence());
                //companies.setNoExplain();
                //companies.setBatchSize(20000);
                //count = 0;
                //Iterator compIter = companies.iterator();
                //while (compIter.hasNext()) {
                //    Company c = (Company) compIter.next();
                //    writer.delete(c);
                //    count++;
                //    if ((count % 10000) == 0) {
                //        LOG.info("Deleting Employees - done " + count);
                //    }
                //}
                writer.commitTransaction();
            }
        } finally {
            //System.gc();
            //System.exit(0);
            if (writer.isInTransaction()) {
                writer.abortTransaction();
            }
            //Connection c = ((ObjectStoreWriterInterMineImpl) writer).getConnection();
            //c.createStatement().execute("delete from employee");
            //c.createStatement().execute("delete from employable");
            //c.createStatement().execute("delete from hasaddress");
            //c.createStatement().execute("delete from company");
            //c.createStatement().execute("delete from hassecretarys");
            //c.createStatement().execute("delete from intermineobject");
            //c.createStatement().execute("delete from randominterface");
            //c.createStatement().execute("delete from thing");
            //c.createStatement().execute("vacuum full");
            //((ObjectStoreWriterInterMineImpl) writer).releaseConnection(c);
        }
    }*/
}

