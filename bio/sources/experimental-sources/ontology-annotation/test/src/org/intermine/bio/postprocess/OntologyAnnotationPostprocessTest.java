package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import org.intermine.model.bio.GOAnnotation;
import org.intermine.model.bio.GOEvidence;
import org.intermine.model.bio.GOEvidenceCode;
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.OntologyTerm;
import org.intermine.model.bio.Protein;
import org.intermine.model.bio.Publication;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the GoPostprocess class.
 */
public class OntologyAnnotationPostprocessTest extends XMLTestCase {

    private ObjectStoreWriter osw;

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();

    }

    public void tearDown() throws Exception {
        deleteAlltheThings();
        osw.close();
    }

    public void deleteAlltheThings() throws ObjectStoreException {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = os.executeSingleton(q);
        Iterator<?> resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();
    }

    public void testPostProcess() throws Exception {
        setUpData();
        OntologyAnnotationPostprocess gp = new OntologyAnnotationPostprocess(osw);
//        gp.setOntologyPrefix("GO");
        gp.postProcess();

        Gene resGene = (Gene) getFromDb(Gene.class).iterator().next();

        // Gene should come back with a collection of GO terms
//        assertEquals(2, resGene.getGoAnnotation().size());

        assertEquals(2, resGene.getOntologyAnnotations().size());
    }

    public void testMerging() throws Exception {
        setUpDuplicateData();

        OntologyAnnotationPostprocess gp = new OntologyAnnotationPostprocess(osw);
        gp.postProcess();

        Gene resGene = (Gene) getFromDb(Gene.class).iterator().next();

        // one annotation instead of two
        assertEquals(1, resGene.getGoAnnotation().size());
        Set<GOAnnotation> goes = resGene.getGoAnnotation();
        for (GOAnnotation a : goes) {
            assertEquals(2, a.getEvidence().size());
        }
    }


    // Store a gene with two protein, each protein has a GO term
    private void setUpData() throws Exception {
        Gene gene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        Protein protein1 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        protein1.addGenes(gene);
        Protein protein2 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        protein2.addGenes(gene);

        List<Object> toStore = new ArrayList<Object>(Arrays.asList(new Object[] {gene, protein1, protein2}));

        toStore.addAll(setUpAnnotations(protein1));
        toStore.addAll(setUpAnnotations(protein2));

        osw.beginTransaction();
        Iterator<Object> i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        osw.commitTransaction();
    }

    private List<Object> setUpAnnotations(Protein protein) {
        GOAnnotation go = (GOAnnotation) DynamicUtil.createObject(Collections.singleton(GOAnnotation.class));
        go.setSubject(protein);
        OntologyTerm ontologyTerm = (OntologyTerm) DynamicUtil.createObject(Collections.singleton(OntologyTerm.class));
        go.setOntologyTerm(ontologyTerm);
        GOEvidence evidence = (GOEvidence) DynamicUtil.createObject(Collections.singleton(GOEvidence.class));
        GOEvidenceCode code = (GOEvidenceCode) DynamicUtil.createObject(Collections.singleton(GOEvidenceCode.class));
        evidence.setCode(code);
        Publication pub = (Publication) DynamicUtil.createObject(Collections.singleton(Publication.class));
        evidence.addPublications(pub);
        go.setEvidence(Collections.singleton(evidence));
        return new ArrayList<Object>(Arrays.asList(new Object[] {go, ontologyTerm, evidence, code, pub}));
    }


    // one gene, two proteins, ONE annotation, different evidence codes - they should merge
    private void setUpDuplicateData() throws Exception {
        Gene gene = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        Protein protein1 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        protein1.addGenes(gene);
        Protein protein2 = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        protein2.addGenes(gene);

        GOAnnotation go1 = (GOAnnotation) DynamicUtil.createObject(Collections.singleton(GOAnnotation.class));
        go1.setSubject(protein1);
        OntologyTerm ontologyTerm = (OntologyTerm) DynamicUtil.createObject(Collections.singleton(OntologyTerm.class));
        go1.setOntologyTerm(ontologyTerm);
        GOEvidence evidence1 = (GOEvidence) DynamicUtil.createObject(Collections.singleton(GOEvidence.class));
        GOEvidenceCode code1 = (GOEvidenceCode) DynamicUtil.createObject(Collections.singleton(GOEvidenceCode.class));
        evidence1.setCode(code1);
        Publication pub1 = (Publication) DynamicUtil.createObject(Collections.singleton(Publication.class));
        evidence1.addPublications(pub1);
        go1.setEvidence(Collections.singleton(evidence1));

        GOAnnotation go2 = (GOAnnotation) DynamicUtil.createObject(Collections.singleton(GOAnnotation.class));
        go2.setSubject(protein2);
        go2.setOntologyTerm(ontologyTerm);
        GOEvidence evidence2 = (GOEvidence) DynamicUtil.createObject(Collections.singleton(GOEvidence.class));
        GOEvidenceCode code2 = (GOEvidenceCode) DynamicUtil.createObject(Collections.singleton(GOEvidenceCode.class));
        evidence2.setCode(code2);
        Publication pub2 = (Publication) DynamicUtil.createObject(Collections.singleton(Publication.class));
        evidence2.addPublications(pub2);
        go2.setEvidence(Collections.singleton(evidence2));


        List<Object> toStore = new ArrayList<Object>(Arrays.asList(new Object[] {gene, protein1, protein2, go1, ontologyTerm, evidence1, code1, pub1, go2, ontologyTerm, evidence2, code2, pub2}));
        osw.beginTransaction();
        Iterator<Object> i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        osw.commitTransaction();
    }


    private Set<InterMineObject> getFromDb(Class<Gene> relClass) throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(relClass);
        q.addToSelect(qc);
        q.addFrom(qc);
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
        Set<InterMineObject> results = new HashSet<InterMineObject>();
        Iterator<?> resIter = res.iterator();
        while(resIter.hasNext()) {
            results.add((InterMineObject) resIter.next());
        }
        ObjectStore os = osw.getObjectStore();
        os.flushObjectById();
        return results;
    }
}
