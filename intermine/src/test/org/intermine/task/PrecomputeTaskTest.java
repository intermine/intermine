package org.intermine.task;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;

import junit.framework.*;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.ObjectStore;

/**
 * Tests for PrecomputeTask.
 *
 * @author Kim Rutherford
 */

public class PrecomputeTaskTest extends StoreDataTestCase
{
    public PrecomputeTaskTest (String arg) {
        super(arg);
    }
    
    public void setUp() throws Exception {
        super.setUp();
    }
    
    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public void executeTest(String type) {

    }

    public static Test suite() {
        return buildSuite(PrecomputeTaskTest.class);
    }

    /**
     * Test that PrecomputeTask creates the 
     */
    public void testExecute() {
        DummyPrecomputeTask task = new DummyPrecomputeTask();

        task.setAlias("os.unittest");
        task.setModelName("testmodel");
        task.setTestMode(Boolean.FALSE);
        task.setMinRows(new Integer(1));

        task.execute();

        assertEquals(7, task.queries.size());

        String[] expectedQueries = new String[] {
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a2_ WHERE a1_.departments CONTAINS a2_ ORDER BY a1_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a2_ WHERE a1_.address CONTAINS a2_ ORDER BY a1_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a2_ WHERE a1_.employees CONTAINS a2_ ORDER BY a1_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a2_ WHERE a1_.employees CONTAINS a2_ ORDER BY a1_",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a2_ WHERE a1_.employees CONTAINS a2_ ORDER BY a1_",
            "SELECT DISTINCT emp, add FROM org.intermine.model.testmodel.Employee AS emp, org.intermine.model.testmodel.Address AS add WHERE emp.address CONTAINS add",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a2_ WHERE a1_.employees CONTAINS a2_",
        };

        for (int i = 0; i < expectedQueries.length; i++) {
            assertEquals(expectedQueries[i], "" + task.queries.get(i));
        }
    }
    
    public void testQueries() {
        
    }

    class DummyPrecomputeTask extends PrecomputeTask {
        protected List queries = new ArrayList();
        protected void precompute(ObjectStore os, Query query) {
            queries.add(query);
        }
    }
}
