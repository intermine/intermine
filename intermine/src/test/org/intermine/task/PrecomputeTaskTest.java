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

import org.intermine.metadata.Model;

import junit.framework.*;

import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.io.InputStream;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.*;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.web.TemplateQuery;
import org.intermine.web.MainHelper;

import org.intermine.model.testmodel.Employee;
import org.intermine.model.testmodel.Department;

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
        task.setTestMode(Boolean.FALSE);
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
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_)",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a1_.vatNumber",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a1_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a1_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a5_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a5_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a5_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.salary",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.title",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.age",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.end",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.fullTime",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.salary AS a9_, a8_.title AS a10_, a8_.age AS a11_, a8_.end AS a12_, a8_.fullTime AS a13_, a8_.name AS a14_, a8_.id AS a15_, a8_.seniority AS a16_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.CEO AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.seniority",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_)",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a1_.vatNumber",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a1_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a1_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a5_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a5_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a5_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.age",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.end",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.fullTime",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.age AS a9_, a8_.end AS a10_, a8_.fullTime AS a11_, a8_.name AS a12_, a8_.id AS a13_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Employee AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_)",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a1_.vatNumber",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a1_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a1_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a5_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a5_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a5_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.title",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.age",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.end",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.fullTime",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_, a8_, a8_.title AS a9_, a8_.age AS a10_, a8_.end AS a11_, a8_.fullTime AS a12_, a8_.name AS a13_, a8_.id AS a14_, a8_.seniority AS a15_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_, org.intermine.model.testmodel.Manager AS a8_ WHERE (a1_.departments CONTAINS a5_ AND a5_.employees CONTAINS a8_) ORDER BY a8_.seniority",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_ WHERE a1_.departments CONTAINS a5_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_ WHERE a1_.departments CONTAINS a5_ ORDER BY a1_.vatNumber",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_ WHERE a1_.departments CONTAINS a5_ ORDER BY a1_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_ WHERE a1_.departments CONTAINS a5_ ORDER BY a1_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_ WHERE a1_.departments CONTAINS a5_ ORDER BY a5_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_ WHERE a1_.departments CONTAINS a5_ ORDER BY a5_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.name AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Department AS a5_ WHERE a1_.departments CONTAINS a5_ ORDER BY a5_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.address AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a5_ WHERE a1_.address CONTAINS a5_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.address AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a5_ WHERE a1_.address CONTAINS a5_ ORDER BY a1_.vatNumber",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.address AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a5_ WHERE a1_.address CONTAINS a5_ ORDER BY a1_.name",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.address AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a5_ WHERE a1_.address CONTAINS a5_ ORDER BY a1_.id",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.address AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a5_ WHERE a1_.address CONTAINS a5_ ORDER BY a5_",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.address AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a5_ WHERE a1_.address CONTAINS a5_ ORDER BY a5_.address",
"SELECT DISTINCT a1_, a1_.vatNumber AS a2_, a1_.name AS a3_, a1_.id AS a4_, a5_, a5_.address AS a6_, a5_.id AS a7_ FROM org.intermine.model.testmodel.Company AS a1_, org.intermine.model.testmodel.Address AS a5_ WHERE a1_.address CONTAINS a5_ ORDER BY a5_.id",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a1_.name",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a1_.id",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.salary",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.title",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.age",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.end",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.fullTime",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.name",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.id",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.salary AS a5_, a4_.title AS a6_, a4_.age AS a7_, a4_.end AS a8_, a4_.fullTime AS a9_, a4_.name AS a10_, a4_.id AS a11_, a4_.seniority AS a12_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.CEO AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.seniority",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.age AS a5_, a4_.end AS a6_, a4_.fullTime AS a7_, a4_.name AS a8_, a4_.id AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a4_ WHERE a1_.employees CONTAINS a4_",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.age AS a5_, a4_.end AS a6_, a4_.fullTime AS a7_, a4_.name AS a8_, a4_.id AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a1_.name",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.age AS a5_, a4_.end AS a6_, a4_.fullTime AS a7_, a4_.name AS a8_, a4_.id AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a1_.id",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.age AS a5_, a4_.end AS a6_, a4_.fullTime AS a7_, a4_.name AS a8_, a4_.id AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.age AS a5_, a4_.end AS a6_, a4_.fullTime AS a7_, a4_.name AS a8_, a4_.id AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.age",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.age AS a5_, a4_.end AS a6_, a4_.fullTime AS a7_, a4_.name AS a8_, a4_.id AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.end",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.age AS a5_, a4_.end AS a6_, a4_.fullTime AS a7_, a4_.name AS a8_, a4_.id AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.fullTime",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.age AS a5_, a4_.end AS a6_, a4_.fullTime AS a7_, a4_.name AS a8_, a4_.id AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.name",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.age AS a5_, a4_.end AS a6_, a4_.fullTime AS a7_, a4_.name AS a8_, a4_.id AS a9_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.id",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a1_.name",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a1_.id",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.title",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.age",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.end",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.fullTime",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.name",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.id",
"SELECT DISTINCT a1_, a1_.name AS a2_, a1_.id AS a3_, a4_, a4_.title AS a5_, a4_.age AS a6_, a4_.end AS a7_, a4_.fullTime AS a8_, a4_.name AS a9_, a4_.id AS a10_, a4_.seniority AS a11_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Manager AS a4_ WHERE a1_.employees CONTAINS a4_ ORDER BY a4_.seniority",
"SELECT DISTINCT a1_, a1_.id AS a2_, a3_, a3_.name AS a4_, a3_.id AS a5_ FROM org.intermine.model.testmodel.HasSecretarys AS a1_, org.intermine.model.testmodel.Secretary AS a3_ WHERE a1_.secretarys CONTAINS a3_",
"SELECT DISTINCT a1_, a1_.id AS a2_, a3_, a3_.name AS a4_, a3_.id AS a5_ FROM org.intermine.model.testmodel.HasSecretarys AS a1_, org.intermine.model.testmodel.Secretary AS a3_ WHERE a1_.secretarys CONTAINS a3_ ORDER BY a1_.id",
"SELECT DISTINCT a1_, a1_.id AS a2_, a3_, a3_.name AS a4_, a3_.id AS a5_ FROM org.intermine.model.testmodel.HasSecretarys AS a1_, org.intermine.model.testmodel.Secretary AS a3_ WHERE a1_.secretarys CONTAINS a3_ ORDER BY a3_",
"SELECT DISTINCT a1_, a1_.id AS a2_, a3_, a3_.name AS a4_, a3_.id AS a5_ FROM org.intermine.model.testmodel.HasSecretarys AS a1_, org.intermine.model.testmodel.Secretary AS a3_ WHERE a1_.secretarys CONTAINS a3_ ORDER BY a3_.name",
"SELECT DISTINCT a1_, a1_.id AS a2_, a3_, a3_.name AS a4_, a3_.id AS a5_ FROM org.intermine.model.testmodel.HasSecretarys AS a1_, org.intermine.model.testmodel.Secretary AS a3_ WHERE a1_.secretarys CONTAINS a3_ ORDER BY a3_.id",
"SELECT DISTINCT emp, add FROM org.intermine.model.testmodel.Employee AS emp, org.intermine.model.testmodel.Address AS add WHERE emp.address CONTAINS add",
"SELECT DISTINCT a1_, a2_ FROM org.intermine.model.testmodel.Department AS a1_, org.intermine.model.testmodel.Employee AS a2_ WHERE a1_.employees CONTAINS a2_",
        };

        assertEquals(expectedQueries.length, task.queries.size());

        for (int i = 0; i < expectedQueries.length; i++) {
            org.intermine.web.LogMe.log("pret", "expected: " + expectedQueries[i]);
            org.intermine.web.LogMe.log("pret", "actual: " + task.queries.get(i));
            org.intermine.web.LogMe.log("pret", "done 1");
            assertEquals(expectedQueries[i], "" + task.queries.get(i));
        }
        org.intermine.web.LogMe.log("pret", "done");
    }

    public void testQueries() {

    }

    // test that correct query and list of indexes generate for pre-computing
    public void testPrecomputeTemplate() throws Exception {
        PrecomputeTask task = new PrecomputeTask();
        Map templates = task.getPrecomputeTemplateQueries();
        TemplateQuery template = (TemplateQuery) templates.get("employeesOverACertainAgeFromDepartmentA");
        System.out.println(MainHelper.makeQuery(template.getQuery(), new HashMap(), new HashMap()));

        Query q = new Query();
        q.setDistinct(true);
        ConstraintSet cs = new ConstraintSet(ConstraintOp.AND);
        QueryClass qcEmp = new QueryClass(Employee.class);
        QueryField qfAge = new QueryField(qcEmp, "age");
        q.addFrom(qcEmp);
        QueryClass qcDept = new QueryClass(Department.class);
        q.addFrom(qcDept);
        QueryObjectReference deptRef = new QueryObjectReference(qcEmp, "department");
        ContainsConstraint cc = new ContainsConstraint(deptRef, ConstraintOp.CONTAINS, qcDept);
        cs.addConstraint(cc);
        QueryField qfName = new QueryField(qcDept, "name");
        SimpleConstraint sc = new SimpleConstraint(qfName, ConstraintOp.EQUALS, new QueryValue("DepartmentA"));
        cs.addConstraint(sc);
        q.addToSelect(qcEmp);
        q.addToSelect(qfAge);
        q.setConstraint(cs);

        PrecomputeTask.QueryAndIndexes expected = task.new QueryAndIndexes();
        expected.setQuery(q);
        expected.addIndex(qfAge);
        assertEquals(expected.toString(), task.processTemplate(template).toString());
    }

    class DummyPrecomputeTask extends PrecomputeTask {
        protected List queries = new ArrayList();
        protected void precompute(ObjectStore os, Query query) {
            queries.add(query);
        }
    }
}
