package org.intermine.task;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import org.intermine.metadata.Model;

public class CreateIndexesTaskTest extends TestCase
{
    Model m;

    public void setUp() throws Exception {
        m = Model.getInstanceByName("testmodel");
    }
    
    //test defined keys and N-1 keys
    public void testProcessClassDescriptor1() throws Exception {
        List expected = new ArrayList();
        expected.add("drop index Department__key");
        expected.add("create index Department__key on Department(name, companyId)");
        expected.add("drop index Department__company");
        expected.add("create index Department__company on Department(companyId)");

        DummyCreateIndexesTask task = new DummyCreateIndexesTask();
        task.processClassDescriptor(m.getClassDescriptorByName("org.intermine.model.testmodel.Department"));
        assertEquals(expected, task.sqlStatements);
    }

    //test indirection table columns
    public void testProcessClassDescriptor2() throws Exception {
        List expected = new ArrayList();
        expected.add("drop index HasSecretarysSecretarys__Secretarys");
        expected.add("create index HasSecretarysSecretarys__Secretarys on HasSecretarysSecretarys(Secretarys)");

        DummyCreateIndexesTask task = new DummyCreateIndexesTask();
        task.processClassDescriptor(m.getClassDescriptorByName("org.intermine.model.testmodel.HasSecretarys"));
        assertEquals(expected, task.sqlStatements);
    }

   class DummyCreateIndexesTask extends CreateIndexesTask {
        protected List sqlStatements = new ArrayList();
        protected void execute(String sql) throws SQLException {
            sqlStatements.add(sql);
        }
    }
}
