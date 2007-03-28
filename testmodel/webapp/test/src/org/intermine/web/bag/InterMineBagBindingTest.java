package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.intermine.model.testmodel.Department;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.dummy.ObjectStoreDummyImpl;
import org.intermine.web.logic.bag.BagElement;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.bag.InterMineBagBinding;
import org.intermine.web.logic.results.ResultElement;


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
        ObjectStore uos = new ObjectStoreDummyImpl();
        Department d1 = new Department();
        d1.setId(new Integer(1));
        os.cacheObjectById(new Integer(1), d1);

        Integer userId = new Integer(101);
        InputStream is =
            getClass().getClassLoader().getResourceAsStream("InterMineBagBindingTest.xml");

        Map savedBags = InterMineBagBinding.unmarshal(new InputStreamReader(is), uos, os,
                                                      new PkQueryIdUpgrader(), userId);
        Map expected = new LinkedHashMap();

        HashSet objectContents = new HashSet();
        objectContents.add(new Integer(1));

        Collection c = new ArrayList();
        c.add(new BagElement(new Integer(10), "Gene"));
        c.add(new BagElement(new Integer(20), "Gene"));
        c.add(new BagElement(new Integer(30), "Gene"));
        InterMineBag objects = new InterMineBag(userId, "bag2", "Gene", uos, os, c);
        expected.put("objects", objects);

        assertEquals(expected, savedBags);
    }
}
