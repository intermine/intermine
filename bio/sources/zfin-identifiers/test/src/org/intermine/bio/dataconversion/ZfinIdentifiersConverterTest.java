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

import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullParser;

public class ZfinIdentifiersConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public ZfinIdentifiersConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        String zbd_history_txt = "ZDB-GENE-050417-192\tZDB-GENE-030131-7190" + ENDL
        + "ZDB-BAC-030131-406\tZDB-BAC-030131-7240\t" + ENDL;
        String ensembl = "#\tZDBID\tSYMBOL\tEnsembl(Zv7)\t" + ENDL
        + "ZDB-GENE-000112-47\tppardb\tENSDARG00000009473\t" + ENDL
        + "ZDB-GENE-000125-12\tigfbp2b\tENSDARG00000052470\t" + ENDL
        + "ZDB-GENE-000125-4\tdlc\tENSDARG00000002336";


        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BioFileConverter converter = new ZfinIdentifiersConverter(itemWriter,
                                                                   Model.getInstanceByName("genomic"));
        converter.process(new StringReader(zbd_history_txt));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "zfin_tgt.xml");

        assertEquals(readItemSet("ZfinIdentifiersConverterTest.xml"), itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("ZfinIdentifiersConverterTest.xml"));
    }
}
