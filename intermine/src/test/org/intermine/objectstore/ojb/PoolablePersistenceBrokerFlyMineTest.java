package org.flymine.objectstore.ojb;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.singlevm.PoolablePersistenceBroker;

import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.util.TypeUtil;

public class PoolablePersistenceBrokerFlyMineTest extends TestCase
{
    ObjectStoreOjbImpl os;

    public PoolablePersistenceBrokerFlyMineTest(String arg1) {
        super(arg1);
    }

    public void setUp() throws Exception {
        os = (ObjectStoreOjbImpl) ObjectStoreFactory.getObjectStore("os.unittest");
    }

    public void testPooling() throws Exception {
        List pbList = new ArrayList();
        PoolablePersistenceBroker ppb = (PoolablePersistenceBroker) os.getPersistenceBroker();
        GenericKeyedObjectPool pool = (GenericKeyedObjectPool) TypeUtil.getFieldValue(ppb, "pool");
        pool.setMaxActive(10);
        pool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL);
        for (int i=10-pool.getNumActive(); i>0; i--) {
            pbList.add(os.getPersistenceBroker());
            System.err.println(pool.getNumActive()+" "+pool.getNumIdle());
        }
        try {
            os.getPersistenceBroker();
            fail("Expected PBFactoryException");
        } catch (PBFactoryException e) {
        } finally {
            //pool.clear() doesn't seem to work here
            Iterator iter = pbList.iterator();
            while(iter.hasNext()) {
                ((PoolablePersistenceBroker) iter.next()).close();
            }
        }
    }
}
