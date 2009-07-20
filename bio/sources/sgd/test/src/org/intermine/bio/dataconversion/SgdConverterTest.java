package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.intermine.dataconversion.ItemWriter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.xml.full.Item;

public class SgdConverterTest extends ItemsTestCase
{
    public SgdConverterTest(String arg) {
        super(arg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcessTaxon() throws Exception {
        String orgId = "7227";
        doTestProcess(orgId);
    }

    public void testProcessAbbreviation() throws Exception {
        String orgId = "Dmel";
        doTestProcess(orgId);
    }

    private void doTestProcess(String orgId) throws Exception, IOException {
        MockItemWriter itemWriter =
            new MockItemWriter(new HashMap<String, org.intermine.model.fulldata.Item>());
//        SgdConverter converter = new SgdConverter(null, Model.getInstanceByName("genomic"),
//                                                  itemWriter);
//        converter.setDataSourceName("SGD");
//        converter.process();
//        itemWriter.close();
//        // writeItemsFile(itemWriter.getItems(), "/tmp/chado-db-test-items-" + orgId + ".xml");
        assertEquals(readItemSet("SgdConverterTest.xml"), itemWriter.getItems());
    }


}
