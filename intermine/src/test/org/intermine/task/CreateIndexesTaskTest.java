package org.intermine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
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
import java.util.HashSet;

import org.intermine.metadata.Model;

public class CreateIndexesTaskTest extends TestCase
{
    Model m;

    public void setUp() throws Exception {
        m = Model.getInstanceByName("testmodel");
    }

    //test defined keys and N-1 keys
    public void testCreateStandardIndexes1() throws Exception {
        List expected = new ArrayList();
        expected.add("drop index Department__key");
        expected.add("create index Department__key on Department(name, companyId, id)");
        expected.add("drop index Department__key__nulls");
        expected.add("create index Department__key__nulls on Department((name IS NULL))");
        expected.add("drop index Department__company");
        expected.add("create index Department__company on Department(companyId, id)");

        DummyCreateIndexesTask task = new DummyCreateIndexesTask();
        task.createStandardIndexes(m.getClassDescriptorByName("org.intermine.model.testmodel.Department"));
        assertEquals(expected, task.sqlStatements);
    }

    //test indirection table columns
    public void testCreateStandardIndexes2() throws Exception {
        List expected = new ArrayList();
        expected.add("drop index HasSecretarysSecretarys__Secretarys");
        expected.add("drop index HasSecretarysSecretarys__HasSecretarys");
        expected.add("create index HasSecretarysSecretarys__Secretarys on HasSecretarysSecretarys(Secretarys, HasSecretarys)");
        expected.add("create index HasSecretarysSecretarys__HasSecretarys on HasSecretarysSecretarys(HasSecretarys, Secretarys)");

        DummyCreateIndexesTask task = new DummyCreateIndexesTask();
        task.createStandardIndexes(m.getClassDescriptorByName("org.intermine.model.testmodel.HasSecretarys"));
        assertEquals(expected, task.sqlStatements);

        expected = new ArrayList();
        expected.add("drop index Secretary__key");
        expected.add("create index Secretary__key on Secretary(name, id)");
        expected.add("drop index Secretary__key__nulls");
        expected.add("create index Secretary__key__nulls on Secretary((name IS NULL))");
        task = new DummyCreateIndexesTask();
        task.createStandardIndexes(m.getClassDescriptorByName("org.intermine.model.testmodel.Secretary"));
        assertEquals(expected, task.sqlStatements);
    }

    // test that primary key indexes are created on subclasses
    public void testCreateIndexesSubclasses() throws Exception {
        List expected = new ArrayList();
        expected.add("drop index ImportantPerson__key");
        expected.add("create index ImportantPerson__key on ImportantPerson(seniority, id)");
        expected.add("drop index ImportantPerson__key__nulls");
        expected.add("create index ImportantPerson__key__nulls on ImportantPerson((seniority IS NULL))");
        expected.add("drop index Contractor__key");
        expected.add("create index Contractor__key on Contractor(seniority, id)");
        expected.add("drop index Contractor__key__nulls");
        expected.add("create index Contractor__key__nulls on Contractor((seniority IS NULL))");
        expected.add("drop index Manager__key");
        expected.add("create index Manager__key on Manager(seniority, id)");
        expected.add("drop index Manager__key__nulls");
        expected.add("create index Manager__key__nulls on Manager((seniority IS NULL))");
        expected.add("drop index CEO__key");
        expected.add("create index CEO__key on CEO(seniority, id)");
        expected.add("drop index CEO__key__nulls");
        expected.add("create index CEO__key__nulls on CEO((seniority IS NULL))");

        DummyCreateIndexesTask task = new DummyCreateIndexesTask();
        task.createStandardIndexes(m.getClassDescriptorByName("org.intermine.model.testmodel.ImportantPerson"));
        //assertEquals(expected, task.sqlStatements);

        assertEquals(new HashSet(expected), new HashSet(task.sqlStatements));
    }


    public void testCreateAttributeIndexes() throws Exception {
        List expected = new ArrayList();
        expected.add("drop index CEO__salary");
        expected.add("create index CEO__salary on CEO(salary, id)");
        expected.add("drop index CEO__title");
        expected.add("create index CEO__title on CEO(title, id)");
        expected.add("drop index CEO__title__nulls");
        expected.add("create index CEO__title__nulls on CEO((title IS NULL))");
        expected.add("drop index CEO__age");
        expected.add("create index CEO__age on CEO(age, id)");
        expected.add("drop index CEO__end");
        expected.add("create index CEO__end on CEO(intermine_end, id)");
        expected.add("drop index CEO__fullTime");
        expected.add("create index CEO__fullTime on CEO(fullTime, id)");
        expected.add("drop index CEO__name");
        expected.add("create index CEO__name on CEO(name, id)");
        expected.add("drop index CEO__name__nulls");
        expected.add("create index CEO__name__nulls on CEO((name IS NULL))");
        expected.add("drop index CEO__seniority");
        expected.add("create index CEO__seniority on CEO(seniority, id)");
        expected.add("drop index CEO__seniority__nulls");
        expected.add("create index CEO__seniority__nulls on CEO((seniority IS NULL))");

        DummyCreateIndexesTask task = new DummyCreateIndexesTask();
        task.createAttributeIndexes(m.getClassDescriptorByName("org.intermine.model.testmodel.CEO"));
        assertEquals(expected, task.sqlStatements);

    }

    class DummyCreateIndexesTask extends CreateIndexesTask {
        protected List sqlStatements = new ArrayList();
        protected void execute(String sql) throws SQLException {
            sqlStatements.add(sql);
        }
    }
}
