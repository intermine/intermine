package org.intermine.objectstore.translating;

/*
 * Copyright (C) 2002-2022 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.objectstore.*;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.model.InterMineObject;

import org.intermine.model.testmodel.Bank;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ObjectStoreTranslatingImplTest extends ObjectStoreTestCase
{
    private static ObjectStoreTranslatingImpl os;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        os = new ObjectStoreTranslatingImpl(
            Model.getInstanceByName("testmodel"),
            ObjectStoreFactory.getObjectStore("os.unittest"),
            new DummyTranslator());

        ObjectStoreTestCase.oneTimeSetUp(os, "osw.unittest", "testmodel", "testmodel_data.xml");
    }

    public void testNullFields() throws Exception { // Don't run this test
    }

    @Test
    public void testCheckStartLimit() throws Exception { ObjectStoreAbstractImplTestCase.testCheckStartLimit(os); }

    @Test
    public void testGetObjectByExampleNull() throws Exception {
        try {
            super.testGetObjectByExampleNull();
            Assert.fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testGetObjectByExampleNonExistent() throws Exception {
        try {
            super.testGetObjectByExampleNonExistent();
            Assert.fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testGetObjectByExampleAttribute() throws Exception {
        try {
            super.testGetObjectByExampleAttribute();
            Assert.fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testGetObjectByExampleFields() throws Exception {
        try {
            super.testGetObjectByExampleFields();
            Assert.fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    @Test
    public void testTranslation() throws Exception {
        ObjectStore os2
            = new ObjectStoreTranslatingImpl(
                Model.getInstanceByName("testmodel"),
                ObjectStoreFactory.getObjectStore("os.unittest"),
                new CompanyTranslator());

        Query q = new Query();
        QueryClass qc = new QueryClass(InterMineObject.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        Results res = os2.execute(q);
        Assert.assertEquals(2, res.size());
        Assert.assertEquals("CompanyA", ((Bank) ((ResultsRow) res.get(0)).get(0)).getName());
        Assert.assertEquals("CompanyB", ((Bank) ((ResultsRow) res.get(1)).get(0)).getName());
    }
}
