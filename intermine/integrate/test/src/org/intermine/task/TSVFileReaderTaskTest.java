package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.xml.full.ItemFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.tools.ant.types.FileSet;

/**
 * Code for testing the TSVFileReaderTask class.
 *
 * @author Kim Rutherford
 */

public class TSVFileReaderTaskTest extends TestCase
{
    private Model model;
    private static final Logger LOG = Logger.getLogger(TSVFileReaderTaskTest.class);

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testLoad() throws Exception {

        TSVFileReaderTask tsvTask = new TSVFileReaderTask();
        tsvTask.setIgnoreDuplicates(true);
        tsvTask.setIntegrationWriterAlias("integration.unittestsingle");
        tsvTask.setSourceName("testsource");

        File tempFile = File.createTempFile("TSVFileReaderTaskTest", "tmp");
        FileWriter fw = new FileWriter(tempFile);
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("TSVFileReaderTaskTest.tsv");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        
        String line = null;
        while ((line = br.readLine()) != null) {
            fw.write(line + "\n");
        }
        
        fw.close();
        
        FileSet fileSet = new FileSet();

        fileSet.setFile(tempFile);
        
        tsvTask.addFileSet(fileSet);

        InputStream confInputStream = 
            getClass().getClassLoader().getResourceAsStream("TSVFileReaderTaskTest.properties");
        DelimitedFileConfiguration dfc = new DelimitedFileConfiguration(model, confInputStream);
        
        tsvTask.executeInternal(dfc, tempFile);

        //Check the results to see if we have some data...
        ObjectStore os = tsvTask.getDirectDataLoader().getIntegrationWriter().getObjectStore();

        Query q = new Query();
        QueryClass empQueryClass = new QueryClass(Employee.class);
        QueryField qf0 = new QueryField(empQueryClass, "age");
        QueryField qf1 = new QueryField(empQueryClass, "name");
        QueryField qf2 = new QueryField(empQueryClass, "fullTime");

        q.addToSelect(qf0);
        q.addToSelect(qf1);
        q.addToSelect(qf2);
        q.addFrom(empQueryClass);
        
        q.addToOrderBy(qf1);
        
        Results r = os.execute(q);

        assertEquals(3, r.size());

        List expectedRow0 = Arrays.asList(new Object[] {new Integer(10), "EmployeeA1", Boolean.FALSE});
        assertEquals(expectedRow0, r.get(0));

        List expectedRow1 = Arrays.asList(new Object[] {new Integer(20), "EmployeeA2", Boolean.TRUE});
        assertEquals(expectedRow1, r.get(1));

        List expectedRow2 = Arrays.asList(new Object[] {new Integer(0), "EmployeeA3", Boolean.FALSE});
        assertEquals(expectedRow2, r.get(2));
    }

    public void tearDown() throws Exception {
        LOG.info("in tear down");
        ObjectStoreWriter osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");

        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        LOG.info("created results");
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            LOG.info("deleting: " + o.getId());
            osw.delete(o);
        }
        osw.commitTransaction();
        LOG.info("committed transaction");
        osw.close();
        LOG.info("closed objectstore");
    }
    
}

