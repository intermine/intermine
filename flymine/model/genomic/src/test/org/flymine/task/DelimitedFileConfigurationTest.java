package org.flymine.task;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;

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
            "className: org.flymine.model.genomic.Gene\n"
            + "keyColumn: 1\n"
            + "column.0: identifier\n"
            + "column.1: symbol\n"
            // no column 2 config
            + "column.3: organismDbId\n";
        StringInputStream stringInputStream = new StringInputStream(config);

        Model model = Model.getInstanceByName("genomic");
        DelimitedFileConfiguration dfc = new DelimitedFileConfiguration(model, stringInputStream);

        assertEquals("org.flymine.model.genomic.Gene",
                     dfc.getConfigClassDescriptor().getName());

        assertEquals("symbol", dfc.getKeyFieldDescriptor().getName());
        assertEquals("identifier",
                     ((FieldDescriptor) dfc.getColumnFieldDescriptors().get(0)).getName());
        assertEquals("symbol",
                     ((FieldDescriptor) dfc.getColumnFieldDescriptors().get(1)).getName());
        assertEquals("organismDbId",
                     ((FieldDescriptor) dfc.getColumnFieldDescriptors().get(3)).getName());
        assertEquals(4, dfc.getColumnFieldDescriptors().size());
    }
}
