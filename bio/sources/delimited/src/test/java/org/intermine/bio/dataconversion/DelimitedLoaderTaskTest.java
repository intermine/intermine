package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SingletonResults;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;

import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Organism;
import org.intermine.model.bio.Protein;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.tools.ant.types.FileSet;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.* ;

public class DelimitedLoaderTaskTest {

    private ObjectStoreWriter osw;
    private static final Logger LOG = Logger.getLogger(DelimitedLoaderTaskTest.class);
    private Model model;

    @Before
    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
    }

    @Test
    public void testFileLoad() throws Exception {
        DelimitedLoaderTask delimitedLoaderTask = new DelimitedLoaderTask();
        delimitedLoaderTask.setIntegrationWriterAlias("integration.bio-test");
        delimitedLoaderTask.setDataSourceName("Delimited source name");
        delimitedLoaderTask.setDataSetTitle("Delimited data set");
        delimitedLoaderTask.setSourceName("delimited-test");
        delimitedLoaderTask.setIgnoreDuplicates(true);
        delimitedLoaderTask.setSourceType("delimited");
        delimitedLoaderTask.setSeparator("tab");
        String columns = "Gene.primaryIdentifier, Organism.taxonId, null," +
                "Protein.primaryIdentifier,Protein.primaryAccession";
        delimitedLoaderTask.setColumns(columns);

        File tempFile = File.createTempFile("tab_delimited_test.tsv", "tmp");
        FileWriter fw = new FileWriter(tempFile);
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("tab_delimited_test.tsv");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = br.readLine()) != null) {
            fw.write(line + "\n");
        }
        fw.close();

        FileSet fileSet = new FileSet();
        fileSet.setDir(new File("/tmp"));
        fileSet.setIncludes("tab_delimited_test.tsv*");
        delimitedLoaderTask.addFileSet(fileSet);

        delimitedLoaderTask.execute();

        //Check the results to see if we have some data...
        ObjectStore os = osw.getObjectStore();

        Query q = new Query();
        QueryClass geneQueryClass = new QueryClass(Gene.class);
        q.addToSelect(geneQueryClass);
        q.addFrom(geneQueryClass);
        Results r = os.execute(q);
        assertEquals(2, r.size());

        q = new Query();
        QueryClass organismQueryClass = new QueryClass(Organism.class);
        q.addToSelect(organismQueryClass);
        q.addFrom(organismQueryClass);
        r = os.execute(q);
        assertEquals(2, r.size());

        q = new Query();
        QueryClass proteinQueryClass = new QueryClass(Protein.class);
        q.addToSelect(proteinQueryClass);
        q.addFrom(proteinQueryClass);
        r = os.execute(q);
        assertEquals(3, r.size());

        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() throws Exception {
        LOG.info("in tear down");
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
