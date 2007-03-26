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

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import org.intermine.xml.full.FullParser;
import org.intermine.xml.full.FullRenderer;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.FileConverter;

public class Drosophila2ProbeConverterTest extends TestCase
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
            + "Flybase;Flybase;http://flybase.bio.indiana.edu/.bin/fbidq.html?%s\n"
            + "GenBank;Entrez;http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=Search&term=%s&doptcmdl=GenBank\n"
            + "Index\tIdentifier\tIdentifier\tSource\tProbe Set Name\tDescription\tGroup\tSequence Type\trep_id\trep_description\tsource_type\tgene_name\tmap\tchromosome\tstrand\tgenome_start\tgenome_stop\tfeature\n"
            + "1\tJ04423.1\tGenBank\tAFFX-BioB-3_at\tE. coli  GEN=bioB  DB_XREF=gb:J04423.1  NOTE=SIF corresponding to nucleotides 2755-3052 of gb:J04423.1  DEF=E.coli 7,8-diamino-pelargonic acid (bioA), biotin synthetase (bioB), 7-keto-8-amino-pelargonic acid synthetase (bioF), bioC protein, and dethiobiotin synthetase (bioD), complete cds.\t\tControl Sequence\n"
            + "2\t\t\t1616608_a_at\tCG9042-RB FEA=BDGP GEN=Gpdh DB_XREF=CG9042 FBgn0001128  SEG=chr2L:+5935896,5940528  MAP=26A3-26A3  LEN=1934  DEF=(CG9042 gene symbol:Gpdh FBgn0001128 (GO:0005737 cytoplasm) (GO:0006127 glycerophosphate shuttle) (GO:0004367 glycerol-3-phosphate dehydrogenase (NAD+)))\t\tBDGP\tCG9042-RB\t[CG9042 gene symbol:Gpdh FBgn0001128 (GO:0005737  cytoplasm ) (GO:0006127  glycerophosphate shuttle ) (GO:0004367  glycerol-3-phosphate dehydrogenase (NAD+) )]\t\tGpdh\t26A3-26A3\tchr2L\t+\t5935896\t5940528\tBDGP\n";

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new Drosophila2ProbeConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
        //FileWriter fw = new FileWriter(new File("affy_tgt.xml"));
        //fw.write(FullRenderer.render(itemWriter.getItems()));
        //fw.close();

        System.out.println(DataTranslatorTestCase.printCompareItemSets(new HashSet(getExpectedItems()), itemWriter.getItems()));
        assertEquals(new HashSet(getExpectedItems()), itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/Drosophila2ProbeConverterTest.xml"));
    }
}
