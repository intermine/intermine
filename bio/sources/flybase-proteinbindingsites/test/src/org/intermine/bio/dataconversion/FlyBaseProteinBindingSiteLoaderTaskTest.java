package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryObjectReference;
import org.intermine.objectstore.query.Results;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.xml.full.ItemFactory;

import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.RegulatoryRegion;

import java.io.File;

import junit.framework.TestCase;

/**
 * Tests for the RedFlyGFF3RecordHandler class.
 *
 * @author Kim Rutherford
 */
public class FlyBaseProteinBindingSiteLoaderTaskTest extends TestCase
{
    private ObjectStoreWriter osw;
    private Model model;
    private ItemFactory itemFactory;

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
        itemFactory = new ItemFactory(model);
    }

    public void testLoad() throws Exception {
        FlyBaseProteinBindingSiteLoaderTask task = new FlyBaseProteinBindingSiteLoaderTask();
        task.setTaxonId(new Integer(7227));
        File[] files = new File[1];
        files[0] = new File("/home/kmr/svn/dev/bio/sources/flybase-proteinbindingsites/test/resources/flybase_protein_test.xml");

        task.setFileArray(files);
        task.setIntegrationWriterAlias("integration.bio-test");
        task.execute();

        ObjectStore os = osw.getObjectStore();

        Query q = new Query();
        QueryClass geneQueryClass = new QueryClass(Gene.class);
        QueryClass rrQueryClass = new QueryClass(RegulatoryRegion.class);
        q.addToSelect(geneQueryClass);
        q.addToSelect(rrQueryClass);
        q.addFrom(geneQueryClass);
        q.addFrom(rrQueryClass);

        QueryObjectReference qor = new QueryObjectReference(rrQueryClass, "gene");
        ContainsConstraint cc = new ContainsConstraint(qor, ConstraintOp.CONTAINS, geneQueryClass);

        q.setConstraint(cc);

        Results r = os.execute(q);

        assertEquals(2, r.size());
    }
}
