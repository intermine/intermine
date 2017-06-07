package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;

/**
 * Simple read/write performance measurement for the ObjectStore, can be used to compare
 * performance between machines and to monitor performance before and after changes. The code
 * simply writes and reads thousands of objects to a single table and reports time taken.
 * @author Richard Smith
 *
 */
public class ReadWritePerformanceTester {
    protected static final Logger LOG = Logger.getLogger(ReadWritePerformanceTester.class);

    public static void testPerformance(ObjectStore os)
        throws ObjectStoreException {

        // make sure we have verbose query logging on
        ((ObjectStoreInterMineImpl) os).setVerboseQueryLog(true);

        int batchSize = 10000;

        ObjectStoreWriterInterMineImpl osw = new ObjectStoreWriterInterMineImpl(os);
        long storeTime = 0;
        int batches = 3;
        for (int i = 0; i < batches; i++) {
            storeTime += storeEmployees(osw, batchSize);
        }
        double timePerThousand = (double) storeTime / ((double)(batches * batchSize)) * 1000;
        System.out.println("Total store time: " + storeTime + "ms. Average time per thousand: "
                + new DecimalFormat("#0.000").format(timePerThousand) + "ms.");
        LOG.info("Total store time: " + storeTime + "ms");

        // flush the cache so we have to materialise all objects
        os.flushObjectById();

        System.out.println("\nReading all employee objects with empty object cache");
        readEmployees(os, batchSize);
    }


    private static long readEmployees(ObjectStore os, int batchSize) throws ObjectStoreException{
        long startTime = System.currentTimeMillis();
        Query q = new Query();
        QueryClass qcEmployee = new QueryClass(Employee.class);
        q.addFrom(qcEmployee);
        q.addToSelect(qcEmployee);
        Results res = os.execute(q, batchSize, false, false, false);
        Iterator<?> resIter = res.iterator();
        int rowCount = 0;
        long splitTime = System.currentTimeMillis();
        while (resIter.hasNext()) {
            ResultsRow<?> rr = (ResultsRow<?>) resIter.next();
            rowCount++;
            if (rowCount % batchSize == 0) {

                System.out.println("Read  " + rowCount + " employee objects, took: "
                        + (System.currentTimeMillis() - splitTime) + "ms.");
                splitTime = System.currentTimeMillis();
            }
        }
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("totalTime: " + totalTime + " rowCount: " + rowCount);
        double timePerThousand = ((double) totalTime / (double) rowCount) * 1000;
        System.out.println("Finished reading " + rowCount + " employee objects, took: " + totalTime
                + "ms. Average time per thousand: "
                + new DecimalFormat("#0.000").format(timePerThousand) + "ms.");
        return totalTime;
    }

    private static long storeEmployees(ObjectStoreWriter osw, int numberToCreate) throws ObjectStoreException {
        long startTime = System.currentTimeMillis();
        List<Employee> employees = createEmployees(numberToCreate);
        for (Employee e: employees) {
            osw.store(e);
        }
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Stored " + numberToCreate + " employee objects, took: " + totalTime
                + "ms");
        return totalTime;
    }


    private static List<Employee> createEmployees(int numberToCreate) {
        List<Employee> employees = new ArrayList<Employee>();

        for (int i = 0; i < numberToCreate; i++) {
            Employee e = new Employee();
            e.setName("Employee" + i);
            e.setAge(i * 10);
            e.setFullTime(i % 2 == 0);
            e.setEnd("Some longer text which doesn't really say much about Employee" + i
                    + " but is just taking up space.");

            employees.add(e);
        }

        return employees;
    }
}
