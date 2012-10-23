package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.tools.ant.BuildException;
import org.intermine.api.InterMineAPITestCase;
import org.intermine.metadata.Model;

/**
 * Class to test that the ModelUpdate loads correctly all the changes set
 * in the modelUpdate.properties file
 * @author butano
 *
 */
public class ModelUpdateTest extends InterMineAPITestCase
{
    private ModelUpdate modelUpdate;

    public ModelUpdateTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        Model oldModel = Model.getInstanceByName("oldtestmodel");
        try {
            modelUpdate = new ModelUpdate(os, uosw, oldModel);
        } catch (BuildException be) {
            tearDown();
            throw be;
        }
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testConstructor() throws Exception {
        assertEquals(1, modelUpdate.getDeletedClasses().size());
        assertEquals(true, modelUpdate.getDeletedClasses().contains("Address"));

        assertEquals(1, modelUpdate.getRenamedClasses().size());
        assertEquals(true, modelUpdate.getRenamedClasses().containsKey("CEOTest"));
        assertEquals("CEO", modelUpdate.getRenamedClasses().get("CEOTest"));

        assertEquals(2, modelUpdate.getRenamedFields().size());
        assertEquals(true, modelUpdate.getRenamedFields().containsKey("CEOTest.sal"));
        assertEquals("salary", modelUpdate.getRenamedFields().get("CEOTest.sal"));
        assertEquals(true, modelUpdate.getRenamedFields().containsKey("Company.CEOTest"));
        assertEquals("CEO", modelUpdate.getRenamedFields().get("Company.CEOTest"));
    }
}
