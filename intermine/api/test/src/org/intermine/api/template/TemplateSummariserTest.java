package org.intermine.api.template;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.Test;

import org.custommonkey.xmlunit.XMLUnit;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.ProfileManager;
import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.model.testmodel.CEO;
import org.intermine.model.testmodel.Department;
import org.intermine.model.testmodel.Employee;
import org.intermine.model.userprofile.Tag;
import org.intermine.model.userprofile.UserProfile;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.ObjectStoreWriterFactory;
import org.intermine.objectstore.StoreDataTestCase;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.QueryClass;
import org.intermine.objectstore.query.QueryField;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.SingletonResults;
import org.intermine.pathquery.PathNode;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.DynamicUtil;
import org.intermine.web.ProfileBinding;
import org.intermine.web.ProfileManagerBinding;
import org.intermine.web.bag.PkQueryIdUpgrader;

/**
 * Tests for the TemplateSummariser.
 */

public class TemplateSummariserTest extends StoreDataTestCase
{
    private Profile profile;
    private ProfileManager pm;
    private ObjectStore os, uos;
    private ObjectStoreWriter osw, uosw;

    public TemplateSummariserTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
        osw = ObjectStoreWriterFactory.getObjectStoreWriter("osw.unittest");
        os = osw.getObjectStore();

        uosw =  ObjectStoreWriterFactory.getObjectStoreWriter("osw.userprofile-test");
        uos = uosw.getObjectStore();
        pm = new ProfileManager(os, uosw);
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
        profile = pm.getSuperuserProfile();
        if (profile == null) {
            fail("Superuser " + pm.getSuperuser() + " in " + pm.getProfileUserNames() + " is null");
        }
        if (profile.getSavedTemplates() == null) {
            fail("Templates map is null");
        }
        TemplateQuery t = profile.getSavedTemplates().get("employeesOfACertainAge");
        TemplateSummariser summariser = new TemplateSummariser(os, uosw);
        assertFalse(summariser.isSummarised(t));
        summariser.summarise(t);
        assertTrue(summariser.isSummarised(t));
        Map<String, List> possibleValues = summariser.getPossibleValues(t);
        assertEquals(1, possibleValues.size());
        assertEquals("Employee.age", possibleValues.keySet().iterator().next());
        Set expected = new HashSet(Arrays.asList(10, 20, 30, 40, 50, 60));
        assertEquals(expected, new HashSet(possibleValues.values().iterator().next()));
        //fail("" + possibleValues);
    }
}
