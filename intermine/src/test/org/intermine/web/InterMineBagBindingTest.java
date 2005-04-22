package org.intermine.web;

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
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;

/**
 * Tests for the InterMineBagBinding class
 *
 * @author Mark Woodbridge
 */
public class InterMineBagBindingTest extends TestCase
{
    InterMineBagBinding bagBinding;

    public InterMineBagBindingTest(String arg) {
        super(arg);
    }

    public void setUp() {
        bagBinding = new InterMineBagBinding();
    }

    public void testProcess() throws Exception {
        ObjectStore os = new ObjectStoreDummyImpl();
        Department d1 = new Department();
        d1.setId(new Integer(1));
        os.cacheObjectById(new Integer(1), d1);

        InputStream is = getClass().getClassLoader().getResourceAsStream("InterMineBagBindingTest.xml");
        Map savedBags = InterMineBagBinding.unmarshal(new InputStreamReader(is), os);
        Map expected = new LinkedHashMap();

        //primitives
        InterMineBag primitives = new InterMineBag(os);
        primitives.add(new Integer(10));
        primitives.add("ten");
        expected.put("primitives", primitives);

        //objects
        InterMineBag objects = new InterMineBag(os);
        objects.add(d1);
        expected.put("objects", objects);

        //mixture
        InterMineBag mixture = new InterMineBag(os);
        mixture.addAll(primitives);
        mixture.addAll(objects);
        expected.put("mixture", mixture);

        assertEquals(expected, savedBags);
    }
}
