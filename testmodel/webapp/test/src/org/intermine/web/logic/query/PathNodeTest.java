package org.intermine.web.logic.query;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Properties;

import org.intermine.metadata.Model;
import org.intermine.web.logic.ClassKeyHelper;

import junit.framework.TestCase;

/**
 * Tests for the PathNode class
 *
 * @author Kim Rutherford
 */
public class PathNodeTest extends TestCase
{
    Map savedQueries, expected, classKeys;

    public void setUp() throws Exception {
        Model model = Model.getInstanceByName("testmodel");
        Properties classKeyProps = new Properties();
            classKeyProps.load(getClass().getClassLoader()
                               .getResourceAsStream("class_keys.properties"));
        classKeys = ClassKeyHelper.readKeys(model, classKeyProps);
    }

    public PathNodeTest(String arg) {
        super(arg);
    }
    
    public void testConstruct() {
        Node parent = new PathNode("Employee");
        Node pathNode = new PathNode(parent, "department");
    }
}
