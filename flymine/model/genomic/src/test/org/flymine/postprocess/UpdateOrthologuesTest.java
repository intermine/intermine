package org.flymine.postprocess;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.custommonkey.xmlunit.XMLTestCase;
import org.flymine.model.genomic.Gene;
import org.flymine.model.genomic.Orthologue;
import org.flymine.model.genomic.Paralogue;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Relation;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.sql.Database;
import org.intermine.util.DatabaseUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Tests for the UpdateOrthologues class.
 */
public class UpdateOrthologuesTest extends XMLTestCase {
    private static final Integer OBJ_PROTEIN_ID = new Integer(100000001);
    private static final Integer SUB_PROTEIN_ID = new Integer(100000002);
    private static final String PKG = "org.flymine.model.genomic";

    private ObjectStoreWriter osw;
    private Model model;

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.genomic-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
    }

    public void tearDown() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addFrom(qc);
        q.addToSelect(qc);
        ObjectStore os = osw.getObjectStore();
        SingletonResults res = new SingletonResults(q, os, os.getSequence());
        Iterator resIter = res.iterator();
        osw.beginTransaction();
        while (resIter.hasNext()) {
            InterMineObject o = (InterMineObject) resIter.next();
            osw.delete(o);
        }
        osw.commitTransaction();

        Set tables = new HashSet();
        tables.addAll(DatabaseUtil.getIndirectionTableNames(model.getClassDescriptorByName(PKG + ".Gene")));
        tables.addAll(DatabaseUtil.getIndirectionTableNames(model.getClassDescriptorByName(PKG + ".Protein")));

        Database db = ((ObjectStoreInterMineImpl) osw.getObjectStore()).getDatabase();
        Connection conn = db.getConnection();
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(true);
            Statement s = conn.createStatement();
            Iterator tableIter = tables.iterator();
            while (tableIter.hasNext()) {
                String sql = "DELETE FROM " + (String) tableIter.next();
                //System.out.println("deleting indirection table: " + sql);
                s.execute(sql);
            }
            conn.setAutoCommit(autoCommit);
        } finally {
            conn.setAutoCommit(autoCommit);
            conn.close();
        }

        osw.close();
    }

    public void testOrthologueSingleGenes() throws Exception {
        Gene objGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        objGene1.setId(new Integer(101));
        Gene subGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        subGene1.setId(new Integer(102));

        setUpData(Orthologue.class, Collections.singleton(objGene1),
                Collections.singleton(subGene1));

        UpdateOrthologues pp = new UpdateOrthologues(osw);
        pp.process();

        // find orthologue in database and check object and subject reference
        Set results = getFromDb(Orthologue.class);
        assertTrue("expected only one result", results.size() == 1);
        Orthologue resOrth = (Orthologue) results.iterator().next();
        resOrth.setId(null);
        assertEquals(objGene1, resOrth.getObject());
        assertEquals(subGene1, resOrth.getSubject());
    }

    public void testParalogueSingleGenes() throws Exception {
        osw.getObjectStore().flushObjectById();
        Gene objGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        objGene1.setId(new Integer(101));
        Gene subGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        subGene1.setId(new Integer(102));

        setUpData(Paralogue.class, Collections.singleton(objGene1),
                Collections.singleton(subGene1));

        UpdateOrthologues pp = new UpdateOrthologues(osw);
        pp.process();

        // find paralogue in database and check object and subject reference
        osw.getObjectStore().flushObjectById();

        Set results = getFromDb(Paralogue.class);
        assertTrue("expected only one result", results.size() == 1);
        Paralogue resPara = (Paralogue) results.iterator().next();
        resPara.setId(null);
        assertEquals(objGene1, resPara.getObject());
        assertEquals(subGene1, resPara.getSubject());
    }

    public void testOrthologueMultipleGenes() throws Exception {
        Gene objGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        objGene1.setId(new Integer(101));
        Gene objGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        objGene2.setId(new Integer(102));
        Gene subGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        subGene1.setId(new Integer(103));
        Gene subGene2 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        subGene2.setId(new Integer(104));

        setUpData(Orthologue.class, new HashSet(Arrays.asList(new Object[] {objGene1, objGene2})),
                  new HashSet(Arrays.asList(new Object[] {subGene1, subGene2})));

        UpdateOrthologues pp = new UpdateOrthologues(osw);
        pp.process();

        OrthologueHolder expOrth1 = new OrthologueHolder((Orthologue) getExpectedData(Orthologue.class, objGene1, subGene1));
        OrthologueHolder expOrth2 = new OrthologueHolder((Orthologue) getExpectedData(Orthologue.class, objGene1, subGene2));
        OrthologueHolder expOrth3 = new OrthologueHolder((Orthologue) getExpectedData(Orthologue.class, objGene2, subGene1));
        OrthologueHolder expOrth4 = new OrthologueHolder((Orthologue) getExpectedData(Orthologue.class, objGene2, subGene2));
        Set expectedHolders = new HashSet(Arrays.asList(new Object[] {expOrth1, expOrth2, expOrth3, expOrth4}));

        // find orthologue in database and check object and subject reference
        Set actual = getFromDb(Orthologue.class);

        assertTrue("expected four Orthologues in database", actual.size() == 4);


        // test OrthologueHolder .equals method
        OrthologueHolder tmpOrth = new OrthologueHolder((Orthologue) getExpectedData(Orthologue.class, objGene1, subGene1));
        assertFalse(expOrth1.equals(expOrth2));
        assertTrue(expOrth1.equals(tmpOrth));

        Set actualHolders = new HashSet();
        Iterator i = actual.iterator();
        while (i.hasNext()) {
            actualHolders.add(new OrthologueHolder((Orthologue) i.next()));
        }
        assertTrue(actualHolders.size() == 4);
        assertTrue(expectedHolders.size() == 4);
        System.out.println("actual: " + actualHolders);
        System.out.println("expected: " + expectedHolders);


        // problems encountered comparing HashSets of OrthologueHolders (or incorrect
        // code herein) made the following debacle necessary
        i = actualHolders.iterator();
        while (i.hasNext()) {
            OrthologueHolder actualHolder = (OrthologueHolder) i.next();
            if (actualHolder.equals(expOrth1)) {
                System.out.println("found expOrth1");
                expectedHolders.remove(expOrth1);
                i.remove();
            }
            if (actualHolder.equals(expOrth2)) {
                System.out.println("found expOrth2");
                expectedHolders.remove(expOrth2);
                i.remove();
            }
            if (actualHolder.equals(expOrth3)) {
                System.out.println("found expOrth3");
                expectedHolders.remove(expOrth3);
                i.remove();
            }
            if (actualHolder.equals(expOrth4)) {
                System.out.println("found expOrth4");
                expectedHolders.remove(expOrth4);
                i.remove();
            }

        }
        assertTrue(actualHolders.isEmpty());
        assertTrue(expectedHolders.isEmpty());


        //assertTrue(expectedHolders.equals(actualHolders));


    }


    private class OrthologueHolder
    {
        public Orthologue orth;

        public OrthologueHolder(Orthologue orth) {
            this.orth = orth;
        }

        public boolean equals(OrthologueHolder holder) {
            if (holder.orth.getObject().getId().equals(this.orth.getObject().getId())
                && holder.orth.getSubject().getId().equals(this.orth.getSubject().getId())) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            //return orth.getObject().getId().hashCode() + (3 * orth.getSubject().getId().hashCode());
            return 0;
        }

        public String toString() {
            return "object: " + orth.getObject().getId() + " subject: " + orth.getSubject().getId() + " hashCode: " + hashCode();
        }
    }


    // return true if expected and actaul ortholuges have the same gene as object and subject
    private boolean compareOrthologues(Orthologue expected, Orthologue actual) {
        if (expected.getObject().equals(actual.getObject())
            && expected.getSubject().equals(actual.getSubject())) {
            return true;
        }
        return false;
    }



    private Relation getExpectedData(Class relClass, Gene objGene, Gene subGene) {
        Relation rel = (Relation) DynamicUtil.createObject(Collections.singleton(relClass));
        rel.setEvidence(new HashSet());
        rel.setId(new Integer(1));
        Protein objProtein = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        objProtein.setId(OBJ_PROTEIN_ID);
        Protein subProtein = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        subProtein.setId(SUB_PROTEIN_ID);

        String clsName = TypeUtil.unqualifiedName(relClass.getName());
        if (clsName.equals("Orthologue")) {
            ((Orthologue) rel).setObjectProtein(objProtein);
            ((Orthologue) rel).setSubjectProtein(subProtein);
        } else {
            ((Paralogue) rel).setObjectProtein(objProtein);
            ((Paralogue) rel).setSubjectProtein(subProtein);
        }
        rel.setObject(objGene);
        rel.setSubject(subGene);
        return rel;
    }


    // create an [Ortho|Para]logue with object and subject Proteins that have objGenes and subGenes
    // in their respective genes collections
    private void setUpData(Class relClass, Set objGenes, Set subGenes) throws Exception {
        Relation rel = (Relation) DynamicUtil.createObject(Collections.singleton(relClass));
        Protein objProtein = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        objProtein.setId(OBJ_PROTEIN_ID);
        System.out.println("setUpData: objGenes = " + objGenes);
        objProtein.setGenes(objGenes);
        Protein subProtein = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        subProtein.setId(SUB_PROTEIN_ID);
        System.out.println("setUpData: subGenes = " + subGenes);
        subProtein.setGenes(subGenes);


        String clsName = TypeUtil.unqualifiedName(relClass.getName());
        if (clsName.equals("Orthologue")) {
            ((Orthologue) rel).setObjectProtein(objProtein);
            ((Orthologue) rel).setSubjectProtein(subProtein);
        } else {
            ((Paralogue) rel).setObjectProtein(objProtein);
            ((Paralogue) rel).setSubjectProtein(subProtein);
        }

        List toStore = new ArrayList(Arrays.asList(new Object[] {rel, objProtein, subProtein}));
        toStore.addAll(objGenes);
        toStore.addAll(subGenes);

        osw.beginTransaction();
        Iterator i = toStore.iterator();
        while (i.hasNext()) {
            osw.store((InterMineObject) i.next());
        }
        osw.commitTransaction();
    }

    // return set of all [Ortho|Para]logues in db
    private Set getFromDb(Class relClass) throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(relClass);
        q.addToSelect(qc);
        q.addFrom(qc);
        SingletonResults res = new SingletonResults(q, osw.getObjectStore(), osw.getObjectStore()
                                                    .getSequence());
        Set results = new HashSet();
        Iterator resIter = res.iterator();
        while(resIter.hasNext()) {
            results.add(resIter.next());
        }
        ObjectStore os = osw.getObjectStore();
        os.flushObjectById();
        return results;
    }


    // work out if two InterMineObjects are equal regardless of their ids by testing equivalence
    // of attributes and ids of reference and collection objects
//     private boolean assertEqualsNoId(InterMineObject a, InterMineObject b) throws IllegalAccessException {
//         Map infos = new HashMap();
//         Iterator clsIter = DynamicUtil.decomposeClass(obj.getClass()).iterator();
//         while (clsIter.hasNext()) {
//             fieldInfos.putAll(TypeUtil.getFieldInfos((Class) clsIter.next()));
//         }

//         Iterator fieldIter = fieldInfos.keySet().iterator();
//         while (fieldIter.hasNext()) {
//             String fieldName = (String) fieldIter.next();
//             if (!fieldName.equals("id")) {

//                 TypeUtil.setFieldValue(newObj, fieldName,
//                                        TypeUtil.getFieldProxy(obj, fieldName));
//             }
//         }

//     }

}
