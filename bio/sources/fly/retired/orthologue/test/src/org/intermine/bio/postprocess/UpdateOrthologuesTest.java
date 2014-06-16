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
import org.intermine.model.bio.Gene;
import org.intermine.model.bio.Homologue;
import org.intermine.model.bio.Protein;
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
import org.intermine.metadata.TypeUtil;

/**
 * Tests for the UpdateOrthologues class.
 */
public class UpdateOrthologuesTest extends XMLTestCase {
    private static final Integer OBJ_PROTEIN_ID = new Integer(100000001);
    private static final Integer SUB_PROTEIN_ID = new Integer(100000002);
    private static final String PKG = "org.intermine.model.bio";

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

        setUpData(Homologue.class, objGene1, subGene1, false);

        UpdateOrthologues pp = new UpdateOrthologues(osw);
        pp.postProcess();

        // find orthologue in database and check object and subject reference
        Set results = getFromDb(Homologue.class);
        assertTrue("expected only one result", results.size() == 1);
        Homologue resOrth = (Homologue) results.iterator().next();
        resOrth.setId(null);
        assertEquals(objGene1, resOrth.getGene());
        assertEquals(subGene1, resOrth.getHomologue());
    }

    public void testParalogueSingleGenes() throws Exception {
        osw.getObjectStore().flushObjectById();
        Gene objGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        objGene1.setId(new Integer(101));
        Gene subGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        subGene1.setId(new Integer(102));

        setUpData(Homologue.class, objGene1, subGene1, false);

        UpdateOrthologues pp = new UpdateOrthologues(osw);
        pp.postProcess();

        // find paralogue in database and check object and subject reference
        osw.getObjectStore().flushObjectById();

        Set results = getFromDb(Homologue.class);
        assertTrue("expected only one result", results.size() == 1);
        Homologue resPara = (Homologue) results.iterator().next();
        resPara.setId(null);
        assertEquals(objGene1, resPara.getGene());
        assertEquals(subGene1, resPara.getHomologue());
    }


    // in some cases the original orthologue will have one end as a Gene and the
    // other as a Protein (currently Genes for worm data)
    public void testOrthologueGeneProtein() throws Exception {
        Gene objGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        objGene1.setId(new Integer(101));
        Gene subGene1 = (Gene) DynamicUtil.createObject(Collections.singleton(Gene.class));
        subGene1.setId(new Integer(102));

        setUpData(Homologue.class, objGene1, subGene1, true);

        UpdateOrthologues pp = new UpdateOrthologues(osw);
        pp.postProcess();

        // find orthologue in database and check object and subject reference
        Set results = getFromDb(Homologue.class);
        assertTrue("expected only one result", results.size() == 1);
        Homologue resOrth = (Homologue) results.iterator().next();
        resOrth.setId(null);
        assertEquals(objGene1, resOrth.getGene());
        assertEquals(subGene1, resOrth.getHomologue());
    }

    // create an [Ortho|Para]logue with object and subject Proteins that have objGenes and subGenes
    // in their respective genes collections
    private void setUpData(Class relClass, Gene objGene, Gene subGene, boolean startsOnSubGene) throws Exception {
        InterMineObject io = (InterMineObject) DynamicUtil.simpleCreateObject(relClass);
        Protein objProtein = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        objProtein.setId(OBJ_PROTEIN_ID);
        objProtein.addGenes(objGene);
        System.out.println("setUpData: objGenes= " + objGene);

        Protein subProtein = (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        subProtein.setId(SUB_PROTEIN_ID);
        System.out.println("setUpData: subGene = " + subGene);
        subProtein.addGenes(subGene);

        String clsName = TypeUtil.unqualifiedName(relClass.getName());
        if ("Homologue".equals(clsName)) {
            if (startsOnSubGene) {
                // in this case one end of the orthologue already references a Gene
                ((Homologue) io).setProtein(objProtein);
                ((Homologue) io).setHomologue(subGene);
            } else {
                ((Homologue) io).setProtein(objProtein);
                ((Homologue) io).setHomologueProtein(subProtein);
            }
        } else {
            ((Homologue) io).setProtein(objProtein);
            ((Homologue) io).setHomologueProtein(subProtein);
        }

        List toStore = new ArrayList(Arrays.asList(new Object[] {io, objProtein, subProtein}));
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
}
