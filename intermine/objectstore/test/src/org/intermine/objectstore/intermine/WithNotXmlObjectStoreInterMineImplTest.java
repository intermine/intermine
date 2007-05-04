package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.Test;

import org.intermine.model.testmodel.Company;
import org.intermine.model.testmodel.Employee;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.Results;

public class WithNotXmlObjectStoreInterMineImplTest extends ObjectStoreInterMineImplTest
{
    public static void oneTimeSetUp() throws Exception {
        storeDataWriter = (ObjectStoreWriterInterMineImpl) ObjectStoreWriterFactory
            .getObjectStoreWriter("osw.notxmlunittest");
        ObjectStoreInterMineImplTest.oneTimeSetUp();
        os = (ObjectStoreInterMineImpl) ObjectStoreFactory.getObjectStore("os.notxmlunittest");
    }

    public WithNotXmlObjectStoreInterMineImplTest(String arg) throws Exception {
        super(arg);
    }

    public static Test suite() {
        return buildSuite(WithNotXmlObjectStoreInterMineImplTest.class);
    }

    public void testFailFast2() throws Exception {
        Query q = new Query();
        QueryClass qc = new QueryClass(Employee.class);
        q.addFrom(qc);
        q.addToSelect(qc);

        Results r = os.execute(q);
        storeDataWriter.store((Company) data.get("CompanyA"));
        r.iterator().hasNext();
    }
}
