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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Test;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.query.Query;

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
    public void testExecute() throws Exception {
        DummyPrecomputeTask task = new DummyPrecomputeTask();

        task.setAlias("os.unittest");
        task.setMinRows(new Integer(1));

        Properties summaryProperties;

        String configFile = "objectstoresummary.config.properties";

        InputStream is = PrecomputeTask.class.getClassLoader().getResourceAsStream(configFile);

        if (is == null) {
            throw new Exception("Cannot find " + configFile + " in the class path");
        }

        summaryProperties = new Properties();
        summaryProperties.load(is);

        ObjectStore os = ObjectStoreFactory.getObjectStore("os.unittest");
        ObjectStoreSummary oss = new ObjectStoreSummary(os, summaryProperties);

        task.precomputeModel(os, oss);

        String[] expectedQueries = new String[] {
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_, a6_, a6_.salary AS a7_, a6_.title AS a8_, a6_.age AS a9_, a6_.end AS a10_, a6_.fullTime AS a11_, a6_.name AS a12_, a6_.seniority AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.CEO AS a6_ WHERE (a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a6_)",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_, a6_, a6_.salary AS a7_, a6_.title AS a8_, a6_.age AS a9_, a6_.end AS a10_, a6_.fullTime AS a11_, a6_.name AS a12_, a6_.seniority AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.CEO AS a6_ WHERE (a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a6_) ORDER BY a4_",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_, a6_, a6_.salary AS a7_, a6_.title AS a8_, a6_.age AS a9_, a6_.end AS a10_, a6_.fullTime AS a11_, a6_.name AS a12_, a6_.seniority AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.CEO AS a6_ WHERE (a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a6_) ORDER BY a6_",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_, a6_, a6_.age AS a7_, a6_.end AS a8_, a6_.fullTime AS a9_, a6_.name AS a10_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Employee AS a6_ WHERE (a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a6_)",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_, a6_, a6_.age AS a7_, a6_.end AS a8_, a6_.fullTime AS a9_, a6_.name AS a10_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Employee AS a6_ WHERE (a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a6_) ORDER BY a4_",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_, a6_, a6_.age AS a7_, a6_.end AS a8_, a6_.fullTime AS a9_, a6_.name AS a10_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Employee AS a6_ WHERE (a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a6_) ORDER BY a6_",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_, a6_, a6_.title AS a7_, a6_.age AS a8_, a6_.end AS a9_, a6_.fullTime AS a10_, a6_.name AS a11_, a6_.seniority AS a12_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Manager AS a6_ WHERE (a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a6_)",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_, a6_, a6_.title AS a7_, a6_.age AS a8_, a6_.end AS a9_, a6_.fullTime AS a10_, a6_.name AS a11_, a6_.seniority AS a12_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Manager AS a6_ WHERE (a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a6_) ORDER BY a4_",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_, a6_, a6_.title AS a7_, a6_.age AS a8_, a6_.end AS a9_, a6_.fullTime AS a10_, a6_.name AS a11_, a6_.seniority AS a12_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_, org.intermine.model.testmodel.Manager AS a6_ WHERE (a1_.departments CONTAINS a4_ AND a4_.employees CONTAINS a6_) ORDER BY a6_",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_ WHERE a1_.departments CONTAINS a4_",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.name AS a5_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a4_ WHERE a1_.departments CONTAINS a4_ ORDER BY a4_",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.address AS a5_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a4_ WHERE a1_.address CONTAINS a4_",
            "SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a4_, a4_.address AS a5_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a4_ WHERE a1_.address CONTAINS a4_ ORDER BY a4_",
            "SELECT DISTINCT a1_, a1_.name AS a2_, a3_, a3_.salary AS a4_, a3_.title AS a5_, a3_.age AS a6_, a3_.end AS a7_, a3_.fullTime AS a8_, a3_.name AS a9_, a3_.seniority AS a10_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a3_ WHERE a1_.employees CONTAINS a3_",
            "SELECT DISTINCT a1_, a1_.name AS a2_, a3_, a3_.salary AS a4_, a3_.title AS a5_, a3_.age AS a6_, a3_.end AS a7_, a3_.fullTime AS a8_, a3_.name AS a9_, a3_.seniority AS a10_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a3_ WHERE a1_.employees CONTAINS a3_ ORDER BY a3_",
            "SELECT DISTINCT a1_, a1_.name AS a2_, a3_, a3_.age AS a4_, a3_.end AS a5_, a3_.fullTime AS a6_, a3_.name AS a7_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a3_ WHERE a1_.employees CONTAINS a3_",
            "SELECT DISTINCT a1_, a1_.name AS a2_, a3_, a3_.age AS a4_, a3_.end AS a5_, a3_.fullTime AS a6_, a3_.name AS a7_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a3_ WHERE a1_.employees CONTAINS a3_ ORDER BY a3_",
            "SELECT DISTINCT a1_, a1_.name AS a2_, a3_, a3_.title AS a4_, a3_.age AS a5_, a3_.end AS a6_, a3_.fullTime AS a7_, a3_.name AS a8_, a3_.seniority AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a3_ WHERE a1_.employees CONTAINS a3_",
            "SELECT DISTINCT a1_, a1_.name AS a2_, a3_, a3_.title AS a4_, a3_.age AS a5_, a3_.end AS a6_, a3_.fullTime AS a7_, a3_.name AS a8_, a3_.seniority AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a3_ WHERE a1_.employees CONTAINS a3_ ORDER BY a3_",
            "SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.HasSecretarys AS a1_, org.intermine.model.testmodel.Secretary AS a2_ WHERE a1_.secretarys CONTAINS a2_",
            "SELECT DISTINCT a1_, a2_, a2_.name AS a3_ FROM org.intermine.model.testmodel.HasSecretarys AS a1_, org.intermine.model.testmodel.Secretary AS a2_ WHERE a1_.secretarys CONTAINS a2_ ORDER BY a2_",
            "SELECT DISTINCT emp, add FROM org.intermine.model.testmodel.Employee AS emp, org.intermine.model.testmodel.Address AS add WHERE emp.address CONTAINS add",
            "SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a2_ WHERE a1_.employees CONTAINS a2_",
        };

        assertEquals(expectedQueries.length, task.queries.size());

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
