package org.intermine.task;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.Model;

public class CreateIndexesTaskTest extends TestCase
{
    Model m;

    public void setUp() throws Exception {
        m = Model.getInstanceByName("testmodel");
    }

    //test defined keys and N-1 key
    public void testCreateStandardIndexes1() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("create index department__key1 on department(name, companyId, id)");
        expected.add("create index department__key2 on department(name, managerId, id)");
        expected.add("create index department__company on department(companyId, id)");
        expected.add("create index department__manager on department(managerId, id)");

        CreateIndexesTask task = new CreateIndexesTask();
        task.setAlias("os.unittest");
        task.setUp();
        Map<String, IndexStatement> statements = new LinkedHashMap<String, IndexStatement>();
        task.getStandardIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.Department"),
                                        statements);
        assertEquals(expected, getIndexStatementStrings(statements));
    }

    //test indirection table columns
    public void testCreateStandardIndexes2() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("create index hassecretaryssecretarys__hassecretarys on hassecretaryssecretarys(HasSecretarys, Secretarys)");
        expected.add("create index hassecretaryssecretarys__secretarys on hassecretaryssecretarys(Secretarys, HasSecretarys)");

        CreateIndexesTask task = new CreateIndexesTask();
        task.setAlias("os.unittest");
        task.setUp();

        Map<String, IndexStatement> statements = new LinkedHashMap<String, IndexStatement>();
        task.getStandardIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.HasSecretarys"),
                                   statements);
        assertEquals(expected, getIndexStatementStrings(statements));

        expected = new HashSet<String>();
        expected.add("create index secretary__key on secretary(name, id)");
        task = new CreateIndexesTask();
        task.setAlias("os.unittest");
        task.setUp();
        statements = new LinkedHashMap<String, IndexStatement>();
        task.getStandardIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.Secretary"),
                                   statements);
        assertEquals(expected, getIndexStatementStrings(statements));
    }

    // test that primary key indexes are created on subclasses
    public void testCreateIndexesSubclasses() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("create index contractor__importantperson__key on contractor(seniority, id)");
        expected.add("create index ceo__importantperson__key on ceo(seniority, id)");
        expected.add("create index importantperson__key on importantperson(seniority, id)");
        expected.add("create index manager__importantperson__key on manager(seniority, id)");

        CreateIndexesTask task = new CreateIndexesTask();
        task.setAlias("os.unittest");
        task.setUp();
        Map<String, IndexStatement> statements = new LinkedHashMap<String, IndexStatement>();
        task.getStandardIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.ImportantPerson"),
                                   statements);
        assertEquals(expected, new HashSet<String>(getIndexStatementStrings(statements)));

        //assertEquals(new HashSet(expected), new HashSet(task.sqlStatements));
    }


    public void testCreateAttributeIndexes() throws Exception {
        Set<String> expected = new HashSet<String>();
        expected.add("create index ceo__salary on ceo(salary)");
        expected.add("create index ceo__title_like on ceo(lower(title) text_pattern_ops)");
        expected.add("create index ceo__title_equals on ceo(lower(title))");
        expected.add("create index ceo__fulltime on ceo(fullTime)");
        expected.add("create index ceo__age on ceo(age)");
        expected.add("create index ceo__end_like on ceo(lower(intermine_end) text_pattern_ops)");
        expected.add("create index ceo__end_equals on ceo(lower(intermine_end))");
        expected.add("create index ceo__name_like on ceo(lower(name) text_pattern_ops)");
        expected.add("create index ceo__name_equals on ceo(lower(name))");
        expected.add("create index ceo__seniority on ceo(seniority)");

        CreateIndexesTask task = new CreateIndexesTask();
        task.setAlias("os.unittest");
        task.setUp();
        Map<String, IndexStatement> statements = new LinkedHashMap<String, IndexStatement>();
        task.getAttributeIndexStatements(m.getClassDescriptorByName("org.intermine.model.testmodel.CEO"),
                                         statements);
        assertEquals(expected.toString(), getIndexStatementStrings(statements).toString());
    }

    private Set<String> getIndexStatementStrings(Map<String, IndexStatement> statements) {
        Set<String> retval = new HashSet<String>();
        for (String indexName: statements.keySet()) {
            IndexStatement indexStatement = (IndexStatement) statements.get(indexName);
            retval.add(indexStatement.getStatementString(indexName));
        }
        return retval;
    }
}
