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
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.FileWriter;
import java.io.File;

import org.flymine.model.FlyMineBusinessObject;
import org.flymine.model.testmodel.*;
import org.flymine.objectstore.flymine.ObjectStoreWriterFlyMineImpl;
import org.flymine.objectstore.query.ConstraintOp;
import org.flymine.objectstore.query.BagConstraint;
import org.flymine.objectstore.query.ClassConstraint;
import org.flymine.objectstore.query.ContainsConstraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.SubqueryConstraint;
import org.flymine.objectstore.query.QueryCollectionReference;
import org.flymine.objectstore.query.QueryObjectReference;
import org.flymine.objectstore.query.QueryReference;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.SingletonResults;
import org.flymine.sql.DatabaseFactory;
import org.flymine.sql.Database;
import org.flymine.util.DynamicUtil;
import org.flymine.util.XmlBinding;
import org.flymine.util.TypeUtil;
import org.flymine.metadata.Model;

import org.apache.log4j.Logger;

public abstract class StoreDataTestCase extends SetupDataTestCase
{
    protected static final Logger LOG = Logger.getLogger(StoreDataTestCase.class);
    protected static ObjectStoreWriter writer;
    
    public StoreDataTestCase(String arg) {
        super(arg);
    }

    public static void oneTimeSetUp() throws Exception {
        SetupDataTestCase.oneTimeSetUp();
        try {
            if (writer == null) {
                writer = (ObjectStoreWriterFlyMineImpl) ObjectStoreWriterFactory
                    .getObjectStoreWriter("osw.unittest");
            }
            storeData();
        } catch (Exception e) {
            if (writer != null) {
                writer.close();
            }
            throw e;
        }
    }

    public static void oneTimeTearDown() throws Exception {
        ObjectStoreQueriesTestCase.oneTimeTearDown();
        removeDataFromStore();
        writer.close();
        writer = null;
    }

    public static void storeData() throws Exception {
        System.out.println("Storing data");
        if (writer == null) {
            throw new NullPointerException("writer must be set before trying to store data");
        }
        long start = new Date().getTime();
        try {
            Iterator iter = data.entrySet().iterator();
            while (iter.hasNext()) {
                FlyMineBusinessObject o = (FlyMineBusinessObject) ((Map.Entry) iter.next())
                    .getValue();
                o.setId(null);
            }
            writer.beginTransaction();
            iter = data.entrySet().iterator();
            while (iter.hasNext()) {
                FlyMineBusinessObject o = (FlyMineBusinessObject) ((Map.Entry) iter.next())
                    .getValue();
                writer.store(o);
            }
            writer.commitTransaction();
        } catch (Exception e) {
            writer.abortTransaction();
            throw new Exception(e);
        }

        //Database db = DatabaseFactory.getDatabase("db.unittest");
        //java.sql.Connection con = db.getConnection();
        java.sql.Connection con = ((ObjectStoreWriterFlyMineImpl) writer).getConnection();
        java.sql.Statement s = con.createStatement();
        //con.setAutoCommit(true);
        s.execute("vacuum analyze");
        ((ObjectStoreWriterFlyMineImpl) writer).releaseConnection(con);
        //con.close();
        System.out.println("Took " + (new Date().getTime() - start) + " ms to set up data and VACUUM ANALYZE");
    }

    public static void removeDataFromStore() throws Exception {
        System.out.println("Removing data");
        long start = new Date().getTime();
        if (writer == null) {
            throw new NullPointerException("writer must be set before trying to remove data");
        }
        try {
            writer.beginTransaction();
            Query q = new Query();
            QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
            q.addFrom(qc);
            q.addToSelect(qc);
            Set dataToRemove = new SingletonResults(q, writer.getObjectStore(),
                    writer.getObjectStore().getSequence());
            Iterator iter = dataToRemove.iterator();
            while (iter.hasNext()) {
                FlyMineBusinessObject toDelete = (FlyMineBusinessObject) iter.next();
                writer.delete(toDelete);
            }
            writer.commitTransaction();
        } catch (Exception e) {
            writer.abortTransaction();
            throw e;
        }
        System.out.println("Took " + (new Date().getTime() - start) + " ms to remove data");
    }

}
