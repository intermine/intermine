package org.intermine.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.model.fulldata.Item;
import org.intermine.model.fulldata.Reference;
import org.intermine.model.fulldata.ReferenceList;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.intermine.ObjectStoreInterMineImpl;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.objectstore.query.SimpleConstraint;

public class ObjectStoreItemPathFollowingImplTest extends TestCase {
    public ObjectStoreItemPathFollowingImplTest(String arg1) {
        super(arg1);
    }

    public void testReferenceFollow() throws Exception {
        ObjectStoreWriterInterMineImpl osw = (ObjectStoreWriterInterMineImpl)
            ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) osw.getObjectStore();
        try {
            Item i1 = createItem("1_0", "flibble", "flobble");
            Reference r1 = new Reference();
            i1.addReferences(r1);
            r1.setItem(i1);
            r1.setName("a");
            r1.setRefId("1_1");
            Item i2 = createItem("1_1", "fish", "fosh");
            Reference r2 = new Reference();
            i2.addReferences(r2);
            r2.setItem(i2);
            r2.setName("b");
            r2.setRefId("1_2");
            Item i3 = createItem("1_2", "floible", "flooble");
            Item i4 = createItem("2_0", "flibble", "flobble");
            Reference r4 = new Reference();
            i4.addReferences(r4);
            r4.setItem(i4);
            r4.setName("a");
            r4.setRefId("2_1");
            Item i5 = createItem("2_1", "fish", "fosh");
            Reference r5 = new Reference();
            i5.addReferences(r5);
            r5.setItem(i5);
            r5.setName("b");
            r5.setRefId("2_2");
            Item i6 = createItem("2_2", "floible", "flooble");
            osw.beginTransaction();
            osw.store(i1);
            osw.store(r1);
            osw.store(i2);
            osw.store(r2);
            osw.store(i3);
            osw.store(i4);
            osw.store(r4);
            osw.store(i5);
            osw.store(r5);
            osw.store(i6);

            Query q = new Query();
            QueryClass qc1 = new QueryClass(Item.class);
            q.addFrom(qc1);
            q.addToSelect(qc1);
            q.setConstraint(new SimpleConstraint(new QueryField(qc1, "className"), ConstraintOp
                        .EQUALS, new QueryValue("flibble")));
            ObjectStoreItemPathFollowingImpl osipfi = new ObjectStoreItemPathFollowingImpl(osw);
            Results r = osipfi.execute(q);
            assertEquals(2, r.size());
            Item gI1 = (Item) ((ResultsRow) r.get(0)).get(0);
            assertEquals("1_0", gI1.getIdentifier());
            Reference gR1 = (Reference) gI1.getReferences().iterator().next();
            assertEquals("a", gR1.getName());
            assertEquals("1_1", gR1.getRefId());
            List refList = osipfi.getItemsByDescription(Collections.singleton(
                        new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "1_1",
                            false)));
            assertEquals(1, refList.size());
            Item gI2 = (Item) refList.get(0);
            assertEquals("1_1", gI2.getIdentifier());
            Reference gR2 = (Reference) gI2.getReferences().iterator().next();
            assertEquals("b", gR2.getName());
            assertEquals("1_2", gR2.getRefId());
            refList = osipfi.getItemsByDescription(Collections.singleton(
                        new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "1_2",
                            false)));
            assertEquals(1, refList.size());
            Item gI3 = (Item) refList.get(0);
            assertEquals("1_2", gI3.getIdentifier());

            Map paths = new HashMap();
            ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("a");
            desc.addConstraint(new ItemPrefetchConstraintDynamic("a",
                        ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            ItemPrefetchDescriptor desc2 = new ItemPrefetchDescriptor("a.b");
            desc2.addConstraint(new ItemPrefetchConstraintDynamic("b",
                        ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            desc.addPath(desc2);
            paths.put("flibble", Collections.singleton(desc));

            osipfi = new ObjectStoreItemPathFollowingImpl(osw, paths);
            r = osipfi.execute(q);
            assertEquals(2, r.size());
            Connection c = osw.getConnection();
            try {
                c.createStatement().execute("flibble"); // invalidate transaction.
                                                        // This stops the OS from working until
                                                        // abortTransaction.
            } catch (SQLException e) {
            }
            osw.releaseConnection(c);
            gI1 = (Item) ((ResultsRow) r.get(0)).get(0);
            assertEquals("1_0", gI1.getIdentifier());
            gR1 = (Reference) gI1.getReferences().iterator().next();
            assertEquals("a", gR1.getName());
            assertEquals("1_1", gR1.getRefId());
            refList = osipfi.getItemsByDescription(Collections.singleton(
                        new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "1_1",
                            false)));
            assertEquals(1, refList.size());
            gI2 = (Item) refList.get(0);
            assertEquals("1_1", gI2.getIdentifier());
            gR2 = (Reference) gI2.getReferences().iterator().next();
            assertEquals("b", gR2.getName());
            assertEquals("1_2", gR2.getRefId());
            refList = osipfi.getItemsByDescription(Collections.singleton(
                        new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "1_2",
                            false)));
            assertEquals(1, refList.size());
            gI3 = (Item) refList.get(0);
            assertEquals("1_2", gI3.getIdentifier());
        } finally {
            osw.abortTransaction();
            osw.close();
        }
    }

    public void testCollectionFollow() throws Exception {
        ObjectStoreWriterInterMineImpl osw = (ObjectStoreWriterInterMineImpl)
            ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) osw.getObjectStore();
        try {
            Item i1 = createItem("1_0", "flibble", "flobble");
            ReferenceList r1 = new ReferenceList();
            i1.addCollections(r1);
            r1.setItem(i1);
            r1.setName("a");
            r1.setRefIds("1_1 1_2");
            Item i2 = createItem("1_1", "fish", "fosh");
            Item i3 = createItem("1_2", "floible", "flooble");
            Item i4 = createItem("2_0", "flibble", "flobble");
            ReferenceList r4 = new ReferenceList();
            i4.addCollections(r4);
            r4.setItem(i4);
            r4.setName("a");
            r4.setRefIds("1_1");
            osw.beginTransaction();
            osw.store(i1);
            osw.store(r1);
            osw.store(i2);
            osw.store(i3);
            osw.store(i4);
            osw.store(r4);

            Query q = new Query();
            QueryClass qc1 = new QueryClass(Item.class);
            q.addFrom(qc1);
            q.addToSelect(qc1);
            q.setConstraint(new SimpleConstraint(new QueryField(qc1, "className"), ConstraintOp
                        .EQUALS, new QueryValue("flibble")));
            Set q2 = Collections.singleton(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.IDENTIFIER, "1_1 1_2", true));

            ObjectStoreItemPathFollowingImpl osipfi = new ObjectStoreItemPathFollowingImpl(osw);
            Results r = osipfi.execute(q);
            assertEquals(2, r.size());
            Item gI1 = (Item) ((ResultsRow) r.get(0)).get(0);
            assertEquals("1_0", gI1.getIdentifier());
            ReferenceList gR1 = (ReferenceList) gI1.getCollections().iterator().next();
            assertEquals("a", gR1.getName());
            assertEquals("1_1 1_2", gR1.getRefIds());
            List refList = osipfi.getItemsByDescription(q2);
            assertEquals(2, refList.size());
            Item gI2 = (Item) refList.get(0);
            assertEquals("1_1", gI2.getIdentifier());
            Item gI3 = (Item) refList.get(1);
            assertEquals("1_2", gI3.getIdentifier());

            Map paths = new HashMap();
            ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("a");
            desc.addConstraint(new ItemPrefetchConstraintDynamic("a",
                        ObjectStoreItemPathFollowingImpl.IDENTIFIER));
            paths.put("flibble", Collections.singleton(desc));

            osipfi = new ObjectStoreItemPathFollowingImpl(osw, paths);
            r = osipfi.execute(q);
            assertEquals(2, r.size());
            Connection c = osw.getConnection();
            try {
                c.createStatement().execute("flibble"); // invalidate transaction.
                                                        // This stops the OS from working until
                                                        // abortTransaction.
            } catch (SQLException e) {
            }
            osw.releaseConnection(c);
            gI1 = (Item) ((ResultsRow) r.get(0)).get(0);
            assertEquals("1_0", gI1.getIdentifier());
            gR1 = (ReferenceList) gI1.getCollections().iterator().next();
            assertEquals("a", gR1.getName());
            assertEquals("1_1 1_2", gR1.getRefIds());
            refList = osipfi.getItemsByDescription(q2);
            assertEquals(2, refList.size());
            gI2 = (Item) refList.get(0);
            assertEquals("1_1", gI2.getIdentifier());
            gI3 = (Item) refList.get(1);
            assertEquals("1_2", gI3.getIdentifier());
        } finally {
            osw.abortTransaction();
            osw.close();
        }
    }

    public void testReverseReferenceFollow() throws Exception {
        ObjectStoreWriterInterMineImpl osw = (ObjectStoreWriterInterMineImpl)
            ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) osw.getObjectStore();
        try {
            Item i1 = createItem("1_0", "flibble", "flobble");
            Item i2 = createItem("1_1", "fish", "fosh");
            Reference r2 = new Reference();
            i2.addReferences(r2);
            r2.setItem(i2);
            r2.setName("a");
            r2.setRefId("1_0");
            Item i3 = createItem("1_2", "floible", "flooble");
            Reference r3 = new Reference();
            i3.addReferences(r3);
            r3.setItem(i3);
            r3.setName("a");
            r3.setRefId("1_0");
            Item i4 = createItem("2_0", "flibble", "flobble");
            osw.beginTransaction();
            osw.store(i1);
            osw.store(i2);
            osw.store(r2);
            osw.store(i3);
            osw.store(r3);
            osw.store(i4);

            Query q = new Query();
            QueryClass qc1 = new QueryClass(Item.class);
            q.addFrom(qc1);
            q.addToSelect(qc1);
            q.setConstraint(new SimpleConstraint(new QueryField(qc1, "className"), ConstraintOp
                        .EQUALS, new QueryValue("flibble")));
            Set q2 = Collections.singleton(new FieldNameAndValue("a", "1_0", true));

            ObjectStoreItemPathFollowingImpl osipfi = new ObjectStoreItemPathFollowingImpl(osw);
            Results r = osipfi.execute(q);
            assertEquals(2, r.size());
            Item gI1 = (Item) ((ResultsRow) r.get(0)).get(0);
            assertEquals("1_0", gI1.getIdentifier());
            List refList = osipfi.getItemsByDescription(q2);
            assertEquals(2, refList.size());
            Item gI2 = (Item) refList.get(0);
            assertEquals("1_1", gI2.getIdentifier());
            Item gI3 = (Item) refList.get(1);
            assertEquals("1_2", gI3.getIdentifier());

            Map paths = new HashMap();
            ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("a");
            desc.addConstraint(new ItemPrefetchConstraintDynamic(
                        ObjectStoreItemPathFollowingImpl.IDENTIFIER, "a"));
            paths.put("flibble", Collections.singleton(desc));

            osipfi = new ObjectStoreItemPathFollowingImpl(osw, paths);
            r = osipfi.execute(q);
            assertEquals(2, r.size());
            Connection c = osw.getConnection();
            try {
                c.createStatement().execute("flibble"); // invalidate transaction.
                                                        // This stops the OS from working until
                                                        // abortTransaction.
            } catch (SQLException e) {
            }
            osw.releaseConnection(c);
            gI1 = (Item) ((ResultsRow) r.get(0)).get(0);
            assertEquals("1_0", gI1.getIdentifier());
            refList = osipfi.getItemsByDescription(q2);
            assertEquals(2, refList.size());
            gI2 = (Item) refList.get(0);
            assertEquals("1_1", gI2.getIdentifier());
            gI3 = (Item) refList.get(1);
            assertEquals("1_2", gI3.getIdentifier());
        } finally {
            osw.abortTransaction();
            osw.close();
        }
    }

    public void testCombinedRules() throws Exception {
        ObjectStoreWriterInterMineImpl osw = (ObjectStoreWriterInterMineImpl)
            ObjectStoreWriterFactory.getObjectStoreWriter("osw.fulldatatest");
        ObjectStoreInterMineImpl os = (ObjectStoreInterMineImpl) osw.getObjectStore();
        try {
            Item i1 = createItem("1_0", "flibble", "flobble");
            Item i2 = createItem("1_1", "fish", "fosh");
            Reference r2 = new Reference();
            i2.addReferences(r2);
            r2.setItem(i2);
            r2.setName("a");
            r2.setRefId("1_0");
            Item i3 = createItem("1_2", "floible", "flooble");
            Reference r3 = new Reference();
            i3.addReferences(r3);
            r3.setItem(i3);
            r3.setName("a");
            r3.setRefId("1_0");
            Item i4 = createItem("2_0", "flibble", "flobble");
            osw.beginTransaction();
            osw.store(i1);
            osw.store(i2);
            osw.store(r2);
            osw.store(i3);
            osw.store(r3);
            osw.store(i4);

            Query q = new Query();
            QueryClass qc1 = new QueryClass(Item.class);
            q.addFrom(qc1);
            q.addToSelect(qc1);
            q.setConstraint(new SimpleConstraint(new QueryField(qc1, "className"), ConstraintOp
                        .EQUALS, new QueryValue("flibble")));
            Set q2 = new HashSet();
            q2.add(new FieldNameAndValue("a", "1_0", true));
            q2.add(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "fish", false));

            ObjectStoreItemPathFollowingImpl osipfi = new ObjectStoreItemPathFollowingImpl(osw);
            Results r = osipfi.execute(q);
            assertEquals(2, r.size());
            Item gI1 = (Item) ((ResultsRow) r.get(0)).get(0);
            assertEquals("1_0", gI1.getIdentifier());
            List refList = osipfi.getItemsByDescription(q2);
            assertEquals(1, refList.size());
            Item gI2 = (Item) refList.get(0);
            assertEquals("1_1", gI2.getIdentifier());

            Map paths = new HashMap();
            ItemPrefetchDescriptor desc = new ItemPrefetchDescriptor("a (className = \"fish\")");
            desc.addConstraint(new ItemPrefetchConstraintDynamic(
                        ObjectStoreItemPathFollowingImpl.IDENTIFIER, "a"));
            desc.addConstraint(new FieldNameAndValue(ObjectStoreItemPathFollowingImpl.CLASSNAME, "fish", false));
            paths.put("flibble", Collections.singleton(desc));

            osipfi = new ObjectStoreItemPathFollowingImpl(osw, paths);
            r = osipfi.execute(q);
            assertEquals(2, r.size());
            Connection c = osw.getConnection();
            try {
                c.createStatement().execute("flibble"); // invalidate transaction.
                                                        // This stops the OS from working until
                                                        // abortTransaction.
            } catch (SQLException e) {
            }
            osw.releaseConnection(c);
            gI1 = (Item) ((ResultsRow) r.get(0)).get(0);
            assertEquals("1_0", gI1.getIdentifier());
            refList = osipfi.getItemsByDescription(q2);
            assertEquals(1, refList.size());
            gI2 = (Item) refList.get(0);
            assertEquals("1_1", gI2.getIdentifier());
        } finally {
            osw.abortTransaction();
            osw.close();
        }
    }

    private Item createItem(String identifier, String className, String implementations) {
        Item item = new Item();
        item.setIdentifier(identifier);
        item.setClassName(className);
        item.setImplementations(implementations);
        return item;
    }
}
