package org.intermine.api.template;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathQuery;

/**
 * Tests for the TemplateSummariser.
 */

public class TemplateSummariserTest extends StoreDataTestCase
{
    private Profile profile;
    private ProfileManager pm;
    private ObjectStore os;
    private ObjectStoreWriter osw, uosw;

    public TemplateSummariserTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();

        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        pm = new ProfileManager(os, uosw);
        profile = new Profile(pm, "testUser", null, "password", new HashMap(),
                new HashMap(), new HashMap());
        pm.createProfile(profile);

        TemplateQuery twoConstraints = new TemplateQuery("twoConstraints", "twoConstraints", "", new PathQuery(model));
        twoConstraints.addViews("Employee.name", "Employee.age");
        PathConstraint ageCon = Constraints.greaterThan("Employee.age", "30");
        twoConstraints.addConstraint(ageCon);
        twoConstraints.setEditable(ageCon, true);
        PathConstraint depCon = Constraints.greaterThan("Employee.department.name", "Finance");
        twoConstraints.addConstraint(depCon);
        twoConstraints.setEditable(depCon, true);
        profile.saveTemplate("template", twoConstraints);
    }

    public void executeTest(String type) {
    }

    public void testQueries() throws Throwable {
    }

    public static void oneTimeSetUp() throws Exception {
        StoreDataTestCase.oneTimeSetUp();
    }

    public static Test suite() {
        return buildSuite(TemplateSummariserTest.class);
    }

    public void test1() throws Exception {
        TemplateQuery t = profile.getSavedTemplates().get("template");
        TemplateSummariser summariser = new TemplateSummariser(os, uosw);
        assertFalse(summariser.isSummarised(t));
        summariser.summarise(t);
        assertTrue(summariser.isSummarised(t));
        Map<String, List<Object>> possibleValues = summariser.getPossibleValues(t);
        assertEquals(2, possibleValues.size());
        assertEquals("Employee.age", possibleValues.keySet().iterator().next());
        Set<Object> expected = new HashSet<Object>(Arrays.asList(10, 20, 30, 40, 50, 60));
        assertEquals(expected, new HashSet<Object>(possibleValues.values().iterator().next()));
    }
}
