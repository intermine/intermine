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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

import org.intermine.metadata.Model;

public class CreateIndexesTaskTest extends TestCase
{
    Model m;

    public void setUp() throws Exception {
        m = Model.getInstanceByName("testmodel");
    }

    //test defined keys and N-1 key
    public void testCreateStandardIndexes1() throws Exception {
        List expected = new ArrayList();
        expected.add("create index Department__key1 on Department(name, companyId, id)");
        expected.add("create index Department__key1__nulls on Department((name IS NULL))");
        expected.add("create index Department__key2 on Department(name, managerId, id)");
        expected.add("create index Department__key2__nulls on Department((name IS NULL))");
        expected.add("create index Department__company on Department(companyId, id)");
        expected.add("create index Department__manager on Department(managerId, id)");

        CreateIndexesTask task = new CreateIndexesTask();
        task.setAlias("os.unittest");
        task.setUp();
        Map statements = new LinkedHashMap();
        task.getStandardIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.Department"),
                                        statements);
        assertEquals(expected, getIndexStatementStrings(statements));
    }

    //test indirection table columns
    public void testCreateStandardIndexes2() throws Exception {
        List expected = new ArrayList();
        expected.add("create index HasSecretarysSecretarys__Secretarys on HasSecretarysSecretarys(Secretarys, HasSecretarys)");
        expected.add("create index HasSecretarysSecretarys__HasSecretarys on HasSecretarysSecretarys(HasSecretarys, Secretarys)");

        CreateIndexesTask task = new CreateIndexesTask();
        
        Map statements = new LinkedHashMap();
        task.getStandardIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.HasSecretarys"),
                                   statements);
        assertEquals(expected, getIndexStatementStrings(statements));

        expected = new ArrayList();
        expected.add("create index Secretary__key on Secretary(name, id)");
        expected.add("create index Secretary__key__nulls on Secretary((name IS NULL))");
        task = new CreateIndexesTask();
        task.setAlias("os.unittest");
        task.setUp();
        statements = new LinkedHashMap();
        task.getStandardIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.Secretary"),
                                   statements);
        assertEquals(expected, getIndexStatementStrings(statements));
    }

    // test that primary key indexes are created on subclasses
    public void testCreateIndexesSubclasses() throws Exception {
        List expected = new ArrayList();
        expected.add("create index Contractor__ImportantPerson__key on Contractor(seniority, id)");
        expected.add("create index Contractor__ImportantPerson__key__nulls on Contractor((seniority IS NULL))");
        expected.add("create index CEO__ImportantPerson__key on CEO(seniority, id)");
        expected.add("create index CEO__ImportantPerson__key__nulls on CEO((seniority IS NULL))");
        expected.add("create index ImportantPerson__key on ImportantPerson(seniority, id)");
        expected.add("create index ImportantPerson__key__nulls on ImportantPerson((seniority IS NULL))");
        expected.add("create index Manager__ImportantPerson__key on Manager(seniority, id)");
        expected.add("create index Manager__ImportantPerson__key__nulls on Manager((seniority IS NULL))");

        CreateIndexesTask task = new CreateIndexesTask();
        task.setAlias("os.unittest");
        task.setUp();
        Map statements = new LinkedHashMap();
        task.getStandardIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.ImportantPerson"),
                                   statements);
        assertEquals(expected, getIndexStatementStrings(statements));

        //assertEquals(new HashSet(expected), new HashSet(task.sqlStatements));
    }


    public void testCreateAttributeIndexes() throws Exception {
        List expected = new ArrayList();
        expected.add("create index CEO__salary on CEO(salary)");
        expected.add("create index CEO__title on CEO(lower(title))");
        expected.add("create index CEO__title__nulls on CEO((title IS NULL))");
        expected.add("create index CEO__fullTime on CEO(fullTime)");
        expected.add("create index CEO__age on CEO(age)");
        expected.add("create index CEO__end on CEO(lower(intermine_end))");
        expected.add("create index CEO__end on CEO((end IS NULL))");
        expected.add("create index CEO__name on CEO(lower(name))");
        expected.add("create index CEO__name__nulls on CEO((name IS NULL))");
        expected.add("create index CEO__seniority on CEO(seniority)");
        expected.add("create index CEO__seniority__nulls on CEO((seniority IS NULL))");

        CreateIndexesTask task = new CreateIndexesTask();
        task.setAlias("os.unittest");
        task.setUp();
        Map statements = new LinkedHashMap();
        task.getAttributeIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.CEO"),
                                         statements);
        assertEquals(expected, getIndexStatementStrings(statements));
    }

    private List getIndexStatementStrings(Map statements) {
        List returnList = new ArrayList();

        Iterator statementsIter = statements.keySet().iterator();

        while (statementsIter.hasNext()) {
            String indexName = (String) statementsIter.next();  

            IndexStatement indexStatement = (IndexStatement) statements.get(indexName);
            
            returnList.add(indexStatement.getStatementString(indexName));
        }
        
        return returnList;
    }
}
