package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.flymine.metadata.Model;
import org.flymine.model.testmodel.*;

public class DynamicUtilTest extends TestCase
{
    public DynamicUtilTest(String arg) {
        super(arg);
    }

    private Model model;

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    // NEED MORE TESTS FOR MULTIPLE INHERITED INTERFACES WHEN AVAILABLE
    // eg.    - B
    //       /
    //      A     - D
    //       \   /   \
    //        - C     - F
    //           \   /
    //            - E

    public void testCreateObjectInterfaceAsClass() throws Exception {
        try {
            DynamicUtil.createObject(model, Collections.singleton("org.flymine.model.testmodel.Company"));
            fail("Expected: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testCreateObjectRubbishClass() throws Exception {
        try {
            DynamicUtil.createObject(model, Collections.singleton("org.flymine.model.testmodel.BlahBlahBlah"));
            fail("Expected: ClassNotFoundException");
        } catch (ClassNotFoundException e) {
        }
    }

    public void testCreateObjectOneInterfaceWithParents() throws Exception {
        Object obj = DynamicUtil.createObject(model, Collections.singleton("org.flymine.model.testmodel.Employable"));
        assertTrue(obj instanceof Employable);
        assertTrue(obj instanceof Thing);
    }

    public void testCreateObjectNoClassTwoInterfaces() throws Exception {
        Set intSet = new HashSet();
        intSet.add("org.flymine.model.testmodel.Employable");
        intSet.add("org.flymine.model.testmodel.ImportantPerson");
        Object obj = DynamicUtil.createObject(model, intSet);
        assertTrue(obj instanceof Employable);
        assertTrue(obj instanceof Thing);
        assertTrue(obj instanceof ImportantPerson);
    }
}
