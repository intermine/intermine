package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2017 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.model.InterMineObject;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.SOTerm;
import org.intermine.model.bio.Transcript;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.metadata.ConstraintOp;
import org.intermine.objectstore.query.ConstraintSet;
import org.intermine.objectstore.query.ContainsConstraint;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryCollectionReference;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the PopulateLocatedFeatures class.
 */
public class PopulateChildFeaturesTest extends TestCase {

    private ObjectStoreWriter osw;
    private Gene storedGene = null;
    private Transcript storedTranscript1 = null;
    private Transcript storedTranscript2 = null;
    private SOTerm storedGeneTerm = null;
    private SOTerm storedTranscriptTerm = null;
    
    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        createData();
    }

    protected Map<String, SOTerm> populateSOTermMap(ObjectStore os) {
    	Map<String, SOTerm> soTerms = new HashMap<String, SOTerm>();
    	soTerms.put("gene", storedGeneTerm);
    	soTerms.put("transcript", storedTranscriptTerm);
        return soTerms;
    }
    
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
        osw.commitTransaction();
        osw.close();
    }

    public void testPopulatingCollections() throws Exception {
        osw.flushObjectById();
    	PopulateChildFeatures plf = new PopulateChildFeatures(osw);
    	plf.populateCollection();
    	
        Query q = new Query();
        QueryClass qcGene = new QueryClass(Gene.class);
        q.addFrom(qcGene);
        q.addToSelect(qcGene);
        
        QueryClass qcTranscript = new QueryClass(Transcript.class);
        q.addFrom(qcTranscript);
        q.addToSelect(qcTranscript);

        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        
        QueryCollectionReference ref1 = new QueryCollectionReference(qcGene, "childFeatures");
        cs.addConstraint(new ContainsConstraint(ref1, ConstraintOp.CONTAINS, qcTranscript));
        
        q.setConstraint(cs);
        
        ObjectStore os = osw.getObjectStore();
        Results res = os.execute(q);
        HashSet<Integer> actualCollectionIds = new HashSet();
        Iterator<Object> resIter = res.iterator();
        while (resIter.hasNext()) {
        	ResultsRow<InterMineObject> row = (ResultsRow<InterMineObject>) resIter.next();
        	Gene resGene = (Gene) row.get(0);
        	Transcript resTranscript = (Transcript) row.get(1);
        	actualCollectionIds.add(resTranscript.getId());
        }
        HashSet<Integer> expectedCollectionIds = new HashSet(Arrays.asList(new Integer[] {storedTranscript1.getId(), storedTranscript2.getId()}));
        assertEquals(expectedCollectionIds, actualCollectionIds);
    }

    private void createData() throws Exception {
        osw.flushObjectById();
    	
    	Set toStore = new HashSet();

    	storedGeneTerm = (SOTerm) DynamicUtil.createObject(Collections.singleton(SOTerm.class));
    	storedGeneTerm.setName("gene");
    	
    	toStore.add(storedGeneTerm);
    	
    	storedTranscriptTerm = (SOTerm) DynamicUtil.createObject(Collections.singleton(SOTerm.class));
    	storedTranscriptTerm.setName("transcript");
    	storedTranscriptTerm.addParents(storedGeneTerm);
    	
        toStore.add(storedTranscriptTerm);
    	
        storedGene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        storedGene.setPrimaryIdentifier("gene1");
        storedGene.setLength(new Integer(10000));
        storedGene.setId(new Integer(1002));
        storedGene.setSequenceOntologyTerm(storedGeneTerm);
        
        toStore.add(storedGene);

        storedTranscript1 = (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript1.setLength(new Integer(1050));
        storedTranscript1.setId(new Integer(11));
        storedTranscript1.setPrimaryIdentifier("transcript1");
        storedTranscript1.setGene(storedGene);
        storedTranscript1.setSequenceOntologyTerm(storedTranscriptTerm);
        toStore.add(storedTranscript1);
       
        storedTranscript2 = (Transcript) DynamicUtil.createObject(Collections.singleton(Transcript.class));
        storedTranscript2.setLength(new Integer(1051));
        storedTranscript2.setId(new Integer(12));
        storedTranscript2.setPrimaryIdentifier("transcript2");
        storedTranscript2.setGene(storedGene);
        storedTranscript2.setSequenceOntologyTerm(storedTranscriptTerm);
        toStore.add(storedTranscript2);
        
        Iterator iter = toStore.iterator();
        while (iter.hasNext()) {
            InterMineObject o = (InterMineObject) iter.next();
            osw.store(o);
        }
    }
}