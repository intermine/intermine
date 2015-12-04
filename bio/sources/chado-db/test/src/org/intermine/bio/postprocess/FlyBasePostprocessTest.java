package org.intermine.bio.postprocess;


/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.custommonkey.xmlunit.XMLTestCase;
import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Intron;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

/**
 * @author Julie Sullivan
 *
 */
public class FlyBasePostprocessTest extends XMLTestCase{

   private ObjectStoreWriter osw;

   public void setUp() throws Exception {
       super.setUp();
       osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
       osw.getObjectStore().flushObjectById();
       setUpData();
   }

   public void tearDown() throws Exception {
       Query q = new Query();
       QueryClass qc = new QueryClass(InterMineObject.class);
       q.addFrom(qc);
       q.addToSelect(qc);
       ObjectStore os = osw.getObjectStore();
       SingletonResults res = os.executeSingleton(q);
       Iterator resIter = res.iterator();
       osw.beginTransaction();
       while (resIter.hasNext()) {
           InterMineObject o = (InterMineObject) resIter.next();
           osw.delete(o);
       }
       osw.commitTransaction();
       osw.close();
   }

   public void testPostProcess() throws Exception {
       FlyBasePostProcess fb = new FlyBasePostProcess(osw);
       fb.postProcess();

       Gene resGene = (Gene) getFromDb(Gene.class).iterator().next();

       // Gene should come back with a collection of introns
       assertEquals(2, resGene.getIntrons().size());
   }


   // Store a gene with two protein, each protein has a GO term
   private void setUpData() throws Exception {
       Gene gene = (Gene) DynamicUtil.createObject(Gene.class);
       Transcript transcript1 = (Transcript) DynamicUtil.createObject(Transcript.class);
       transcript1.setGene(gene);
       Transcript transcript2 = (Transcript) DynamicUtil.createObject(Transcript.class);
       transcript2.setGene(gene);

       Intron intron1 = (Intron) DynamicUtil.createObject(Intron.class);
       intron1.setTranscripts(Collections.singleton(transcript1));
       Intron intron2 = (Intron) DynamicUtil.createObject(Intron.class);
       intron2.setTranscripts(Collections.singleton(transcript2));

       List toStore = new ArrayList(Arrays.asList(new Object[] {gene, transcript1, transcript2, intron1, intron2}));

       osw.beginTransaction();
       Iterator i = toStore.iterator();
       while (i.hasNext()) {
           osw.store((InterMineObject) i.next());
       }
       osw.commitTransaction();
   }

   private Set<InterMineObject> getFromDb(Class relClass) throws Exception {
       Query q = new Query();
       QueryClass qc = new QueryClass(relClass);
       q.addToSelect(qc);
       q.addFrom(qc);
       SingletonResults res = osw.getObjectStore().executeSingleton(q);
       Set<InterMineObject> results = new HashSet<InterMineObject>();
       Iterator resIter = res.iterator();
       while(resIter.hasNext()) {
           results.add((InterMineObject) resIter.next());
       }
       ObjectStore os = osw.getObjectStore();
       os.flushObjectById();
       return results;
   }
}
