package org.intermine.objectstore;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SingletonResults;

public abstract class StoreDataTestCase extends SetupDataTestCase
{
    protected static ObjectStoreWriter storeDataWriter;
    
    public StoreDataTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();
        try {
            if (storeDataWriter == null) {
                storeDataWriter = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
            }
            storeData();
            //System.exit(1);
        } catch (Exception e) {
            if (storeDataWriter != null) {
                storeDataWriter.close();
            }
            throw e;
        }
    }

    public static void oneTimeTearDown() throws Exception {
        ObjectStoreQueriesTestCase.oneTimeTearDown();
        removeDataFromStore();
        storeDataWriter.close();
        storeDataWriter = null;
    }

    public static void storeData() throws Exception {    	
    	//checkIsEmpty();
    	System.out.println("Storing data");
        if (storeDataWriter == null) {
            throw new NullPointerException("storeDataWriter must be set before trying to store data");
        }
        long start = new Date().getTime();
        try {
            //Iterator iter = data.entrySet().iterator();
            //while (iter.hasNext()) {
            //    InterMineObject o = (InterMineObject) ((Map.Entry) iter.next())
            //        .getValue();
            //    o.setId(null);
            //}
            storeDataWriter.beginTransaction();
            Iterator iter = data.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                Object o = entry.getValue();
                storeDataWriter.store(o);
            }
        } catch (Exception e) {
            storeDataWriter.abortTransaction();
            throw new Exception(e);
        } finally {
            storeDataWriter.commitTransaction();
        }

        System.out.println("Took " + (new Date().getTime() - start) + " ms to set up data");
    }

    public static void checkIsEmpty() throws Exception {
    	Query q = new Query();
    	QueryClass qc = new QueryClass(InterMineObject.class);
    	q.addToSelect(qc);
    	q.addFrom(qc);
    	ObjectStore os = storeDataWriter.getObjectStore();
    	Results res = os.execute(q);
    	Iterator resIter = res.iterator();
    	if (resIter.hasNext()) {
    		System.out.println("WARNING - database was not empty before storing data");
    	}
    }
    
    public static void removeDataFromStore() throws Exception {
        System.out.println("Removing data");
        long start = new Date().getTime();
        if (storeDataWriter == null) {
            throw new NullPointerException("storeDataWriter must be set before trying to remove data");
        }
        try {
            storeDataWriter.beginTransaction();
            Iterator iter = data.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                InterMineObject o = (InterMineObject) entry.getValue();
                storeDataWriter.delete(o);
            }
            Query q = new Query();
            QueryClass qc = new QueryClass(InterMineObject.class);
            q.addFrom(qc);
            q.addToSelect(qc);
            SingletonResults dataToRemove = storeDataWriter.getObjectStore().executeSingleton(q);
            iter = dataToRemove.iterator();
            while (iter.hasNext()) {
                InterMineObject toDelete = (InterMineObject) iter.next();
                storeDataWriter.delete(toDelete);
            }
            storeDataWriter.commitTransaction();
        } catch (RuntimeException e) {
            storeDataWriter.abortTransaction();
            storeDataWriter.beginTransaction();
            Iterator iter = data.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                if (entry.getValue() instanceof InterMineObject) {
                    InterMineObject o = (InterMineObject) entry.getValue();
                    storeDataWriter.delete(o);
                }
            }
            storeDataWriter.commitTransaction();
        } catch (Exception e) {
            storeDataWriter.abortTransaction();
            throw e;
        }
        System.out.println("Took " + (new Date().getTime() - start) + " ms to remove data");
    }
}
