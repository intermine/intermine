package org.intermine.bio.postprocess;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.tools.ant.filters.StringInputStream;
import org.flymine.model.genomic.Protein;
import org.flymine.model.genomic.Synonym;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.util.DynamicUtil;

/**
 * Tests for the SynonymUpdater class.
 * @author Kim Rutherford
 */
public class SynonymUpdaterTest extends TestCase {

    private ObjectStoreWriter osw;


    public SynonymUpdaterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.bio-test");
        osw.getObjectStore().flushObjectById();
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

    public void testUpdateSynonyms() throws Exception {
        osw.beginTransaction();
        Protein storedProtein1 =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein1.setPrimaryIdentifier("Protein1");
        storedProtein1.setName("Protein name");
        osw.store(storedProtein1);
        Protein storedProtein2 =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein2.setPrimaryIdentifier("Protein2");
        osw.store(storedProtein2);
        Protein storedProtein3 =
            (Protein) DynamicUtil.createObject(Collections.singleton(Protein.class));
        storedProtein3.setPrimaryIdentifier("Protein3");
        storedProtein3.setName("Protein name 3");
        osw.store(storedProtein3);

        Synonym prot1Synonym1 =
            (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));
        prot1Synonym1.setValue("Protein1");
        prot1Synonym1.setSubject(storedProtein1);
        osw.store(prot1Synonym1);
        Synonym prot1Synonym2 =
            (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));
        prot1Synonym2.setValue("non match");
        prot1Synonym2.setSubject(storedProtein1);
        osw.store(prot1Synonym2);
        Synonym prot1Synonym3 =
            (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));
        prot1Synonym3.setValue("Protein name");
        prot1Synonym3.setSubject(storedProtein1);
        osw.store(prot1Synonym3);
        Synonym prot2Synonym1 =
            (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));
        prot2Synonym1.setValue("Protein2");
        prot2Synonym1.setSubject(storedProtein2);
        osw.store(prot2Synonym1);
        osw.commitTransaction();
        Synonym prot3Synonym1 =
            (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));
        prot3Synonym1.setValue("random name");
        prot3Synonym1.setSubject(storedProtein3);
        prot3Synonym1.setIsPrimary(Boolean.TRUE);
        osw.store(prot3Synonym1);
        Synonym prot3Synonym2 =
            (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));
        prot3Synonym2.setValue("non-primary name");
        prot3Synonym2.setSubject(storedProtein3);
        prot3Synonym2.setIsPrimary(Boolean.FALSE);
        osw.store(prot3Synonym2);
        Synonym prot3Synonym3 =
            (Synonym) DynamicUtil.createObject(Collections.singleton(Synonym.class));
        prot3Synonym3.setValue("Protein name 3");
        prot3Synonym3.setSubject(storedProtein3);
        prot3Synonym3.setIsPrimary(Boolean.TRUE);
        osw.store(prot3Synonym3);

        SynonymUpdater synonymUpdater = new SynonymUpdater() {
            protected InputStream getClassKeysInputStream() {
                return new StringInputStream("Protein = name, primaryIdentifier\n");
            }

        };
        synonymUpdater.setObjectStoreWriter(osw);
        synonymUpdater.update();

        Query q = new Query();
        QueryClass qcSynonym = new QueryClass(Synonym.class);
        q.addFrom(qcSynonym);
        q.addToSelect(qcSynonym);

        ObjectStore os = osw.getObjectStore();

        Results res = os.execute(q);
        Iterator iter = res.iterator();

        while (iter.hasNext()) {
            ResultsRow row = (ResultsRow) iter.next();
            Synonym synonym = (Synonym) row.get(0);
            if (synonym.getValue().equals("Protein1")) {
                assertTrue(synonym.getIsPrimary().booleanValue());
                assertEquals(synonym.getSubject().getPrimaryIdentifier(), "Protein1");
            } else {
                if (synonym.getValue().equals("Protein name")) {
                    assertTrue(synonym.getIsPrimary().booleanValue());
                    assertEquals(synonym.getSubject().getName(), "Protein name");
                } else {
                    if (synonym.getValue().equals("Protein2")) {
                        assertTrue(synonym.getIsPrimary().booleanValue());
                    } else {
                        if (synonym.getValue().equals("non match")) {
                            assertFalse(synonym.getIsPrimary().booleanValue());
                        } else {
                            if (synonym.getValue().equals("random name")) {
                                assertTrue(synonym.getIsPrimary().booleanValue());
                            } else {
                                if (synonym.getValue().equals("non-primary name")) {
                                    assertFalse(synonym.getIsPrimary().booleanValue());
                                } else {
                                    if (synonym.getValue().equals("Protein name 3")) {
                                        assertTrue(synonym.getIsPrimary().booleanValue());
                                    } else {
                                        throw new RuntimeException("unknown synonym: "
                                                                   + synonym.getValue());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
