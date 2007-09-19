package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.StringReader;
import java.util.HashMap;

import org.intermine.dataconversion.FileConverter;
import org.intermine.dataconversion.ItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.metadata.Model;

public class Drosophila2ProbeConverterTest extends ItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public Drosophila2ProbeConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        //
        String input = "Version=2\n"
            + "Arrays=Drosophila_2\n"
            + "NURLS=2\n"
            + "Flybase;Flybase;http://flybase.bio.indiana.edu/.bin/fbidq.html?%s" + ENDL
            + "GenBank;Entrez;http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=Search&term=%s&doptcmdl=GenBank" + ENDL
            + "Index\tIdentifier\tIdentifier\tSource\tProbe Set Name\tDescription\tGroup\tSequence Type\trep_id\trep_description\tsource_type\tgene_name\tmap\tchromosome\tstrand\tgenome_start\tgenome_stop\tfeature\n"
            + "1\tJ04423.1\tGenBank\tAFFX-BioB-3_at\tE. coli  GEN=bioB  DB_XREF=gb:J04423.1  NOTE=SIF corresponding to nucleotides 2755-3052 of gb:J04423.1  DEF=E.coli 7,8-diamino-pelargonic acid (bioA), biotin synthetase (bioB), 7-keto-8-amino-pelargonic acid synthetase (bioF), bioC protein, and dethiobiotin synthetase (bioD), complete cds.\t\tControl Sequence" + ENDL
            + "2\t\t\t1616608_a_at\tCG9042-RB FEA=BDGP GEN=Gpdh DB_XREF=CG9042 FBgn0001128  SEG=chr2L:+5935896,5940528  MAP=26A3-26A3  LEN=1934  DEF=(CG9042 gene symbol:Gpdh FBgn0001128 (GO:0005737 cytoplasm) (GO:0006127 glycerophosphate shuttle) (GO:0004367 glycerol-3-phosphate dehydrogenase (NAD+)))\t\tBDGP\tCG9042-RB\t[CG9042 gene symbol:Gpdh FBgn0001128 (GO:0005737  cytoplasm ) (GO:0006127  glycerophosphate shuttle ) (GO:0004367  glycerol-3-phosphate dehydrogenase (NAD+) )]\t\tGpdh\t26A3-26A3\tchr2L\t+\t5935896\t5940528\tBDGP" + ENDL
            + "654\t\t\t1623461_at\tCG40239-RA  FEA=BDGP  GEN=CG40239  DB_XREF=CG40239 FBgn0058239  SEG=chr2h:+119934,120342  LEN=408  DEF=(CG40239 gene symbol:CG40239 FBgn0058239 )\t\tBDGP\tCG40239-RA\t[CG40239 gene symbol:CG40239 FBgn0058239 ]\t\tCG40239\t\tchr2h\t+\t119934\t120342\tBDGP" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new Drosophila2ProbeConverter(itemWriter,
                                                                Model.getInstanceByName("genomic"));
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //writeItemsFile(itemWriter.getItems(), "affy-probes-tgt-items.xml");

        assertEquals(readItemSet("test/Drosophila2ProbeConverterTest.xml"), itemWriter.getItems());
    }

}
