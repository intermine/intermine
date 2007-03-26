package org.intermine.task;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.task.DelimitedFileConfiguration;

import junit.framework.TestCase;

import org.apache.tools.ant.filters.StringInputStream;

/**
 * Tests for DelimitedFileConfiguration.
 *
 * @author Kim Rutherford
 */

public class DelimitedFileConfigurationTest extends TestCase
{
    public void testConfig() throws Exception {
        String config =
            "className: org.intermine.model.testmodel.Employee\n"
            + "keyColumn: 1\n"
            + "column.0: name\n"
            + "column.1: age\n"
            // no column 2 config
            + "column.3: fullTime\n";
        StringInputStream stringInputStream = new StringInputStream(config);

        Model model = Model.getInstanceByName("testmodel");
        DelimitedFileConfiguration dfc = new DelimitedFileConfiguration(model, stringInputStream);

        assertEquals("org.intermine.model.testmodel.Employee",
                     dfc.getConfigClassDescriptor().getName());

        assertEquals("name",
                     ((FieldDescriptor) dfc.getColumnFieldDescriptors().get(0)).getName());
        assertEquals("age",
                     ((FieldDescriptor) dfc.getColumnFieldDescriptors().get(1)).getName());
        assertEquals("fullTime",
                     ((FieldDescriptor) dfc.getColumnFieldDescriptors().get(3)).getName());
        assertEquals(4, dfc.getColumnFieldDescriptors().size());
    }
}
