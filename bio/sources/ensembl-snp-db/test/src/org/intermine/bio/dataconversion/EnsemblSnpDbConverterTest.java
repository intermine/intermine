package org.intermine.bio.dataconversion;

import java.util.Collections;
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
    }
}
