package org.intermine.bio.postprocess;

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
import org.flymine.model.genomic.Translation;
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
import org.intermine.sql.DatabaseUtil;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Tests for the UpdateOrthologues class.
 */
public class UpdateOrthologuesTest extends XMLTestCase {
    private static final Integer OBJ_TRANSLATION_ID = new Integer(100000001);
    private static final Integer SUB_TRANSLATION_ID = new Integer(100000002);
    private static final String PKG = "org.flymine.model.genomic";

    private ObjectStoreWriter osw;
    private Model model;

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
        model = Model.getInstanceByName("genomic");
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

        Set tables = new HashSet();
        tables.addAll(DatabaseUtil.getIndirectionTableNames(model.getClassDescriptorByName(PKG + ".Gene")));
        tables.addAll(DatabaseUtil.getIndirectionTableNames(model.getClassDescriptorByName(PKG + ".Translation")));

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

        setUpData(Orthologue.class, objGene1, subGene1, false);

        UpdateOrthologues pp = new UpdateOrthologues(osw);
        pp.postProcess();

        // find orthologue in database and check object and subject reference
        Set results = getFromDb(Orthologue.class);
        assertTrue("expected only one result", results.size() == 1);
        Orthologue resOrth = (Orthologue) results.iterator().next();
        resOrth.setId(null);
        assertEquals(objGene1, resOrth.getGene());
        assertEquals(subGene1, resOrth.getOrthologue());
    }

    public void testParalogueSingleGenes() throws Exception {
        osw.getObjectStore().flushObjectById();
        Gene objGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        objGene1.setId(new Integer(101));
        Gene subGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        subGene1.setId(new Integer(102));

        setUpData(Paralogue.class, objGene1, subGene1, false);

        UpdateOrthologues pp = new UpdateOrthologues(osw);
        pp.postProcess();

        // find paralogue in database and check object and subject reference
        osw.getObjectStore().flushObjectById();

        Set results = getFromDb(Paralogue.class);
        assertTrue("expected only one result", results.size() == 1);
        Paralogue resPara = (Paralogue) results.iterator().next();
        resPara.setId(null);
        assertEquals(objGene1, resPara.getGene());
        assertEquals(subGene1, resPara.getParalogue());
    }


    // in some cases the original orthologue will have one end as a Gene and the
    // other as a Translation (currently Genes for worm data)
    public void testOrthologueGeneTranslation() throws Exception {
        Gene objGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        objGene1.setId(new Integer(101));
        Gene subGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        subGene1.setId(new Integer(102));

        setUpData(Orthologue.class, objGene1, subGene1, true);

        UpdateOrthologues pp = new UpdateOrthologues(osw);
        pp.postProcess();

        // find orthologue in database and check object and subject reference
        Set results = getFromDb(Orthologue.class);
        assertTrue("expected only one result", results.size() == 1);
        Orthologue resOrth = (Orthologue) results.iterator().next();
        resOrth.setId(null);
        assertEquals(objGene1, resOrth.getGene());
        assertEquals(subGene1, resOrth.getOrthologue());
    }

    // create an [Ortho|Para]logue with object and subject Translations that have objGenes and subGenes
    // in their respective genes collections
    private void setUpData(Class relClass, Gene objGene, Gene subGene, boolean startsOnSubGene) throws Exception {
        InterMineObject io = (InterMineObject) DynamicUtil.createObject(Collections.singleton(relClass));
        Translation objTranslation = (Translation) DynamicUtil.createObject(Collections.singleton(Translation.class));
        objTranslation.setId(OBJ_TRANSLATION_ID);
        System.out.println("setUpData: objGenes= " + objGene);
        objTranslation.setGene(objGene);

        Translation subTranslation = (Translation) DynamicUtil.createObject(Collections.singleton(Translation.class));
        subTranslation.setId(SUB_TRANSLATION_ID);
        System.out.println("setUpData: subGene = " + subGene);
        subTranslation.setGene(subGene);

        String clsName = TypeUtil.unqualifiedName(relClass.getName());
        if (clsName.equals("Orthologue")) {
            if (startsOnSubGene) {
                // in this case one end of the orthologue already references a Gene
                ((Orthologue) io).setTranslation(objTranslation);
                ((Orthologue) io).setOrthologue(subGene);
            } else {
                ((Orthologue) io).setTranslation(objTranslation);
                ((Orthologue) io).setOrthologueTranslation(subTranslation);
            }
        } else {
            ((Paralogue) io).setTranslation(objTranslation);
            ((Paralogue) io).setParalogueTranslation(subTranslation);
        }

        List toStore = new ArrayList(Arrays.asList(new Object[] {io, objTranslation, subTranslation}));
        toStore.add(objGene);
        toStore.add(subGene);

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
        SingletonResults res = osw.getObjectStore().executeSingleton(q);
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
