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
        String zbd_history_txt = "ZDB-GENE-111111-1111\tZDB-GENE-111111-1112" + ENDL
        + "ZDB-BAC-030131-406\tZDB-BAC-030131-7240\t" + ENDL;

        String ensembl = "#\tZDBID\tSYMBOL\tEnsembl(Zv7)\t" + ENDL
        + "ZDB-GENE-111111-1111\tsymbol1111\tENSDARG00000001111\t" + ENDL
        + "ZDB-GENE-222222-2222\tsymbol2222\tENSDARG00000002222\t" + ENDL;

        String aliases_txt = "ZDB-GENE-222222-2222\tcalcium channel, voltage-dependent, beta 1 subunit\tsymbol2222\tsyn2222" + ENDL
        + "ZDB-GENE-111111-1111\tSTARD3 N-terminal like\tstard3nl\twu:fa01c03" + ENDL
        + "ZDB-GENE-111111-1111\tSTARD3 N-terminal like\tstard3nl\tzgc:86628" + ENDL;



        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        BioFileConverter converter = new ZfinIdentifiersConverter(itemWriter,
                                                                   Model.getInstanceByName("genomic"));
        converter.process(new StringReader(zbd_history_txt));
        converter.process(new StringReader(ensembl));
        converter.process(new StringReader(aliases_txt));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "zfin_tgt.xml");

        assertEquals(readItemSet("ZfinIdentifiersConverterTest.xml"), itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("ZfinIdentifiersConverterTest.xml"));
    }
}
