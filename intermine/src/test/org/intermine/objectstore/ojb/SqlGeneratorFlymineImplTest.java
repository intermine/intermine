package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.objectstore.query.*;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.metadata.DescriptorRepository;

import org.flymine.model.testmodel.*;

public class SqlGeneratorFlymineImplTest extends TestCase
{
    DescriptorRepository dr;
    SqlGeneratorFlymineImpl gen;

    public SqlGeneratorFlymineImplTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        ObjectStoreOjbImpl os = (ObjectStoreOjbImpl) ObjectStoreFactory.getObjectStore("os.unittest");
        PersistenceBroker broker = os.getPersistenceBroker();
        dr = broker.getDescriptorRepository();
        gen = (SqlGeneratorFlymineImpl) broker.serviceSqlGenerator();
    }

    public void testPreparedSelectStatement() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q, dr);
        assertEquals(s1.getStatement(), gen.getPreparedSelectStatement(q, dr, 0, Integer.MAX_VALUE));
    }

    public void testLimitAndOffset() throws Exception {
        Query q = new Query();
        QueryClass qc1 = new QueryClass(Company.class);
        q.addToSelect(qc1);
        q.addFrom(qc1);
        FlymineSqlSelectStatement s1 = new FlymineSqlSelectStatement(q, dr);
        String string1 = s1.getStatement();
        string1 += " LIMIT 10300 OFFSET 47";

        assertEquals(string1, gen.getPreparedSelectStatement(q, dr, 47, 10300));
    }

}
