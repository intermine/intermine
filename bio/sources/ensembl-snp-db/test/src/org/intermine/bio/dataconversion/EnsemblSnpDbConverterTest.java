package org.intermine.bio.dataconversion;

import java.util.HashMap;

import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;
import org.intermine.sql.Database;
import org.intermine.sql.DatabaseFactory;

public class EnsemblSnpDbConverterTest extends ItemsTestCase
{
    private EnsemblSnpDbConverter converter;
    private MockItemWriter itemWriter;


    public EnsemblSnpDbConverterTest(String arg) {
        super(arg);
    }
    public void setUp() throws Exception {
        Database db = DatabaseFactory.getDatabase("db.bio-test");
        itemWriter = new MockItemWriter(new HashMap());
        converter = new EnsemblSnpDbConverter(db, Model.getInstanceByName("genomic"), itemWriter);
        super.setUp();
    }

    public void testDetermineType() throws Exception {
        assertEquals("snp", converter.determineType("A|G"));
        assertEquals("snp", converter.determineType("A\\G"));
        assertEquals("snp", converter.determineType("A/G"));
        assertEquals("snp", converter.determineType("A|G|C"));
        assertEquals("snp", converter.determineType("a|g"));

        assertEquals("cnv", converter.determineType("CNV"));
        assertEquals("cnv", converter.determineType("cnv"));

        assertEquals("cnv probe", converter.determineType("cnv_probe"));
        assertEquals("cnv probe", converter.determineType("CNV_PROBE"));

        assertEquals("cnv", converter.determineType("CNV"));
        assertEquals("cnv", converter.determineType("cnv"));

        assertEquals("hgmd_mutation", converter.determineType("HGMD_MUTATION"));
        assertEquals("hgmd_mutation", converter.determineType("hgmd_mutation"));

        assertEquals("het", converter.determineType("A"));
        assertEquals("het", converter.determineType("GGG"));

        assertEquals("in-del", converter.determineType("A/-"));
        assertEquals("in-del", converter.determineType("-/C"));
        assertEquals("in-del", converter.determineType("-/GAC"));
        assertEquals("in-del", converter.determineType("TN/-"));

        assertEquals("named", converter.determineType("LARGE/-"));
        assertEquals("named", converter.determineType("-/INSERTION"));
        assertEquals("named", converter.determineType("INS/-"));
        assertEquals("named", converter.determineType("-/DEL"));
        assertEquals("named", converter.determineType("-/(LARGEINSERTION)"));
        assertEquals("named", converter.determineType("-/(224 BP INSERTION)"));

        assertEquals("substitution", converter.determineType("AA/GC"));

        assertEquals("microsat", converter.determineType("(CA)14/25/26"));

        assertEquals("mixed", converter.determineType("-/A/T/TTA"));
        assertEquals("mixed", converter.determineType("C/A/-/TTA"));
    }
}
