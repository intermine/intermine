package org.intermine.bio.dataconversion;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

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
import org.intermine.bio.dataconversion.InparanoidConverter;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.FileConverter;

public class InparanoidConverterTest extends TestCase
{
    private String ENDL = System.getProperty("line.separator");

    public InparanoidConverterTest(String arg) {
        super(arg);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        // the input file format is 5 tab-delimited columns (eg. '14 1217 CE 1.000 O01438')
        // the first two are cluster IDs and are ignored
        // the third is the species (orthologues are cross-species, paralogues are within-species)
        // the fourth is a confidence relative to the closest match (defined as the orthologue and given a confidence of 1.000)
        // note that the confidence for the first member of a group appears to be meaningless (row 1).
        // the fifth is some form of swissprot id to identify the protein
        // so...this input should produce one orthologue (rows 1 & 3) and two paralogues (1 & 2, 3 & 4)
        String input = "14\t1217\tmodSC\t1.000\tS000001208\t100%\tUbiquitin.\t\tF25B5.4" + ENDL
            + "14\t1217\tmodSC\t0.997\tS000003666\t100%\t(Lethal protein 363).\t\tQ95Q95" + ENDL
            + "14\t1217\tensAG\t1.000\tENSANGP00000028450\t100%\t40S RIBOSOMAL S27A Ensembl-family member\tAnopheles_gambiae.MOZ2a.dec.pep.fa:>ENSANGP00000028450 pep:novel chromosome:MOZ2a:2R:12996734:12999022:1 gene:ENSANGG00000024959 transcript:ENSANGT00000029080" + ENDL
            + "14\t1217\tensAG\t0.566\tENSANGP00000008615\t100%\tF25C8.3 PROTEIN.\tAnopheles_gambiae.MOZ2a.dec.pep.fa:>ENSANGP00000008615 pep:known chromosome:MOZ2a:2R:6308197:6324138:-1 gene:ENSANGG00000006494 transcript:ENSANGT00000008615\tQ8T5I0" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        FileConverter converter = new InparanoidConverter(itemWriter);
        converter.process(new StringReader(input));
        converter.close();

        // uncomment to write out a new target items file
//           FileWriter fw = new FileWriter(new File("orth_tgt.xml"));
//           fw.write(FullRenderer.render(itemWriter.getItems()));
//           fw.close();

        System.out.println(DataTranslatorTestCase.printCompareItemSets(new HashSet(getExpectedItems()), itemWriter.getItems()));
        assertEquals(new HashSet(getExpectedItems()), itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/InparanoidConverterTest.xml"));
    }
}
