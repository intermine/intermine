package org.flymine.dataloader;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.Model;
import org.flymine.model.datatracking.Source;

import junit.framework.TestCase;

public class DataLoaderHelperTest extends TestCase
{
    Model model;

    public void setUp() throws Exception {
        model = Model.getInstanceByName("testmodel");
    }

    public void testGetPrimaryKeysCld() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
        Map expected = new HashMap();
        expected.put("key1", new PrimaryKey("name, address"));
        expected.put("key2", new PrimaryKey("vatNumber"));        
        assertEquals(expected, DataLoaderHelper.getPrimaryKeys(cld));
    }

    public void testGetPrimaryKeysCldSource() throws Exception {
        ClassDescriptor cld = model.getClassDescriptorByName("org.flymine.model.testmodel.Company");
        Source source = new Source();
        source.setName("testsource");
        assertEquals(Collections.singleton(new PrimaryKey("vatNumber")), DataLoaderHelper.getPrimaryKeys(cld, source));
    }
}
