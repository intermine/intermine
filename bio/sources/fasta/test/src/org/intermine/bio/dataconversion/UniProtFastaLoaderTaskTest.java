package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;

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

import org.intermine.model.bio.DataSet;
import org.intermine.model.bio.SequenceFeature;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Sequence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
public class UniProtFastaLoaderTaskTest extends TestCase
{
    private ObjectStoreWriter osw;
    private static final Logger LOG = Logger.getLogger(UniProtFastaLoaderTaskTest.class);
    private String dataSetTitle = "uniprot fasta test title";
    private final String dataSourceName = "test-source";

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
    }


    public void testFastaLoad() throws Exception {
        UniProtFastaLoaderTask flt = new UniProtFastaLoaderTask();
        flt.setFastaTaxonId("7227");
        flt.setIgnoreDuplicates(true);
        flt.setSequenceType("protein");
        flt.setClassName("org.intermine.model.bio.Protein");
        flt.setIntegrationWriterAlias("integration.bio-test");
        flt.setSourceName("fasta-test");
        flt.setDataSetTitle(dataSetTitle);
        flt.setDataSourceName(dataSourceName);
        flt.setClassAttribute("primaryAccession");

        File[] files = new File[1];
        files[0] = File.createTempFile("UniProtFastaLoaderTaskTest", "tmp");
        FileWriter fw = new FileWriter(files[0]);
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("uniprot.fasta");
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = br.readLine()) != null) {
            fw.write(line + "\n");
        }

        fw.close();
        files[0].deleteOnExit();
        flt.setFileArray(files);
        flt.execute();

        //Check the results to see if we have some data...
        ObjectStore os = osw.getObjectStore();

        Query q = new Query();
        QueryClass queryClass = new QueryClass(Protein.class);
        QueryClass seqQueryClass = new QueryClass(Sequence.class);
        q.addToSelect(queryClass);
        q.addToSelect(seqQueryClass);
        q.addFrom(queryClass);
        q.addFrom(seqQueryClass);

        QueryObjectReference qor = new QueryObjectReference(queryClass, "sequence");
        ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS, seqQueryClass);

        q.setConstraint(cc);

        Results r = os.execute(q);

        assertEquals(1, r.size());

        Protein protein = (Protein) ((List) r.get(0)).get(0);

        assertEquals("Q9V8R9-2", protein.getPrimaryAccession());

        "7227".equals(protein.getOrganism().getTaxonId());

        DataSet dataSet = protein.getDataSets().iterator().next();
        assertEquals(dataSetTitle, dataSet.getName());
        assertEquals(dataSourceName, dataSet.getDataSource().getName());

        /*
        >sp|Q9V8R9-2|41_DROME Isoform 2 of Protein 4.1 homolog OS=Drosophila melanogaster GN=cora
        */
        assertEquals("MPAEIKPSAPAEPETPTKSKPKSSSSSHGKPALARVTLLDGSLLDVSIDRKAIGRDVINS"
                     + "ICAGLNLIEKDYFGLTYETPTDPRTWLDLEKPVSKFFRTDTWPLTFAVKFYPPEPSQLKE"
                     + "DITRYHLCLQVRNDILEGRLPCTFVTHALLGSYLVQSEMGDYDAEEMPTRAYLKDFKIAP"
                     + "NQTAELEDKVMDLHKTHKGQSPAEAELHYLENAKKLAMYGVDLHPAKDSEGVDIMLGVCA"
                     + "SGLLVYRDKLRINRFAWPKILKISYKRHHFYIKIRPGEFEQYESTIGFKLANHRAAKKLW"
                     + "KSCVEHHTFFRLMTPEPVSKSKMFPVFGSTYRYKGRTQAESTNTPVDRTPPKFNRTLSGA"
                     + "RLTSRSMDALALAEKEKVARKSSTLDHRGDRNADGDAHSRSPIKNKKEKSSTGTASASSQ"
                     + "SSLEGDYETNLEIEAIEAEPPVQDADKEAKLREKKQKEKEEKERKEREKRELEEKKKAEK"
                     + "AAKAALAAGAAAGAAVNGNDELNDSNKSDKSSGRRVDPNDPRFAGARTTVTHTMTLTGEI"
                     + "DPVTGRIKSEYGDIDPNTGDIDPATAVTDPVTGKLILNYAQIDPSHFGKQAQVQTTTETV"
                     + "PITRQQFFDGVKHISKGALRRDSEGSSDDDMTAQYGADQVNEILIGSPAGQAGGKLGKPV"
                     + "STPTVVKTTTKQVLTKNIDGVTHNVEEEVRNLGTGEVTYSTQEHKADATPTDLSGAYVTA"
                     + "TAVTTRTATTHEDLGKNAKTEQLEEKTVATTRTHDPNKQQQRVVTQEVKTTATVTSGDQK"
                     + "SPLFTTSATTGPHVESTRVVLGEDTPGFSGHGEIISTQTGGGGGGI", protein.getSequence().getResidues().toString());
    }

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
