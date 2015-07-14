package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import junit.framework.TestCase;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Consequence;
import org.intermine.model.bio.Deletion;
import org.intermine.model.bio.Insertion;
import org.intermine.model.bio.SNV;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.SingletonResults;

public class VcfLoaderTaskTest extends TestCase
{
    private ObjectStoreWriter osw;

    Model model = Model.getInstanceByName("genomic");

    String taxonId = "9606";
    String dataSetName = "dbSNP data set";
    String dataSourceName = "Ensembl";
    String fileName = "Homo_sapiens.vcf";

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();

    }

    @Override
    public void tearDown() throws Exception {
        if (osw.isInTransaction()) {
            osw.abortTransaction();
        }
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }

        qc = new QueryClass(Consequence.class);
        osw.delete(qc, null);

        osw.commitTransaction();
        osw.close();
    }

    public void testExecute() throws Exception {
        VcfLoaderTask converter = new VcfLoaderTask();

        File file = File.createTempFile("vcf-test", "tmp");
        FileWriter fw = new FileWriter(file);
        InputStream is =
            getClass().getClassLoader().getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = br.readLine()) != null) {
            fw.write(line + "\n");
        }

        fw.close();
        file.deleteOnExit();
        converter.setSourceName("vcf-test");
        converter.setVcfTaxonId(taxonId);
        converter.setVcfDataSetName(dataSetName);
        converter.setVcfDataSourceName(dataSourceName);
        converter.setIntegrationWriterAlias("integration.bio-test");

        File[] files = new File[1];
        files[0] = file;
        converter.setFileArray(files);
        converter.execute();


        //Check the results to see if we have some data...
        ObjectStore os = osw.getObjectStore();

        Query q = new Query();
        QueryClass qc = new QueryClass(SNV.class);
        q.addToSelect(qc);
        q.addFrom(qc);

//        QueryCollectionReference qor = new QueryCollectionReference(QCSNP, "consequences");
//        ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS, QCConsequence);

//        q.setConstraint(cc);

        Results r = os.execute(q);

        assertEquals("there should be two SNVs", 2, r.size());

        // --------------------------------------------------------- //

        q = new Query();
        qc = new QueryClass(Deletion.class);
        q.addToSelect(qc);
        q.addFrom(qc);

        r = os.execute(q);
        assertEquals("there should be one deletion", 1, r.size());

        // --------------------------------------------------------- //

        q = new Query();
        qc = new QueryClass(Insertion.class);
        q.addToSelect(qc);
        q.addFrom(qc);

        r = os.execute(q);
        assertEquals("there should be one Insertion", 1, r.size());

    }
}
