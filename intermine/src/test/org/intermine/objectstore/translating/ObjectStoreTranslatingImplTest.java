package org.flymine.objectstore.translating;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import org.flymine.metadata.Model;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.Results;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.model.FlyMineBusinessObject;
import org.flymine.objectstore.ObjectStoreAbstractImplTestCase;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.ObjectStoreException;

import org.flymine.model.testmodel.Bank;
import org.flymine.model.testmodel.Company;

public class ObjectStoreTranslatingImplTest extends ObjectStoreAbstractImplTestCase
{
    public static void oneTimeSetUp() throws Exception {
        os = new ObjectStoreTranslatingImpl(Model.getInstanceByName("testmodel"), ObjectStoreFactory.getObjectStore("os.unittest"), new DummyTranslator());
        ObjectStoreAbstractImplTestCase.oneTimeSetUp();
    }

    public ObjectStoreTranslatingImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(ObjectStoreTranslatingImplTest.class);
    }
    
    public void testGetObjectByExampleNull() throws Exception {
        try {
            super.testGetObjectByExampleNull();
            fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    public void testGetObjectByExampleNonExistent() throws Exception {
        try {
            super.testGetObjectByExampleNonExistent();
            fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    public void testGetObjectByExampleAttribute() throws Exception {
        try {
            super.testGetObjectByExampleAttribute();
            fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }

    public void testGetObjectByExampleFields() throws Exception {
        try {
            super.testGetObjectByExampleFields();
            fail("Expected: UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
        }
    }
    
    public void testTranslation() throws Exception {
        ObjectStore os2 = new ObjectStoreTranslatingImpl(Model.getInstanceByName("testmodel"), ObjectStoreFactory.getObjectStore("os.unittest"), new CompanyTranslator());
        Query q = new Query();
        QueryClass qc = new QueryClass(FlyMineBusinessObject.class);
        q.addToSelect(qc);
        q.addFrom(qc);
        Results res = os2.execute(q);
        assertEquals(2, res.size());
        assertEquals("CompanyA", ((Bank) ((ResultsRow) res.get(0)).get(0)).getName());
        assertEquals("CompanyB", ((Bank) ((ResultsRow) res.get(1)).get(0)).getName());
    }
    
    static class DummyTranslator extends Translator
    {
        public void setObjectStore(ObjectStore os) {
        }
        
        public Query translateQuery(Query query) throws ObjectStoreException {
            return query;
        }
        
        public FlyMineBusinessObject translateToDbObject(FlyMineBusinessObject o) {
            return o;
        }
        
        public FlyMineBusinessObject translateFromDbObject(FlyMineBusinessObject o) {
            return o;
        }
    }
    
    static class CompanyTranslator extends Translator
    {
        public void setObjectStore(ObjectStore os) {
        }
        
        public Query translateQuery(Query query) throws ObjectStoreException {
            Query q = new Query();
            QueryClass qc = new QueryClass(Company.class);
            q.addToSelect(qc);
            q.addFrom(qc);
            return q;
        }
        
        public FlyMineBusinessObject translateToDbObject(FlyMineBusinessObject o) {
            return o;
        }
        
        public FlyMineBusinessObject translateFromDbObject(FlyMineBusinessObject o) {
            if (o instanceof Company) {
                Bank bank = new Bank();
                bank.setName(((Company) o).getName());
                return bank;
            } else {
                return o;
            }
        }
    }
}
