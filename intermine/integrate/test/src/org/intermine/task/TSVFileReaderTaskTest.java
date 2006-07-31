package org.intermine.task;

import java.util.Iterator;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.xml.full.ItemFactory;

import java.io.File;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.tools.ant.types.FileSet;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * TSVFileReaderTaskTest class
 *
 * @author Kim Rutherford
 */

public class TSVFileReaderTaskTest extends TestCase
{
    private Model model;
    private ItemFactory itemFactory;

    private static final Logger LOG = Logger.getLogger(TSVFileReaderTaskTest.class);

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
        itemFactory = new ItemFactory(model);
    }

    public void testFastaLoad() throws Exception {

        TSVFileReaderTask tsvTask = new TSVFileReaderTask();
        tsvTask.setIgnoreDuplicates(true);
        tsvTask.setIntegrationWriterAlias("integration.unittestsingle");
        tsvTask.setSourceName("tsv");

        File file = new File("resources/TSVFileReaderTaskTest.tsv");
        FileSet fileSet = new FileSet();

        fileSet.setFile(file);
        
        tsvTask.addFileSet(fileSet);

        InputStream confInputStream = 
            getClass().getClassLoader().getResourceAsStream("TSVFileReaderTaskTest.properties");
        DelimitedFileConfiguration dfc = new DelimitedFileConfiguration(model, confInputStream);
        
        tsvTask.executeInternal(dfc, file);

        //Check the results to see if we have some data...
        ObjectStore os = tsvTask.getDirectDataLoader().getIntegrationWriter().getObjectStore();

        Query q = new Query();
        QueryClass empQueryClass = new QueryClass(Employee.class);

        q.addToSelect(empQueryClass);
        q.addFrom(empQueryClass);

        Results r = os.execute(q);

        assertEquals(3, r.size());
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
        SingletonResults res
                = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore().getSequence());
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

