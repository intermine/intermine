package org.flymine.dataconversion;

/*
 * Copyright (C) 2002-2004 FlyMine
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
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

import org.intermine.xml.full.FullParser;
import org.intermine.dataconversion.DataConversionTestCase;
import org.intermine.dataconversion.MockItemWriter;

public class OrthologueConverterTest extends DataConversionTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public void setUp() throws Exception {
        super.setUp();
        expectedItems = getExpectedItems();
        modelName = "genomic";
    }

    public void testProcess() throws Exception {
        // the input file format is 5 tab-delimited columns:
        // gene1   gene2   orthologue_type   method   source

        String input = "Title" + ENDL + "GENE1\tGENE2\tSEED\tBest Reciprocal Hit\tEnsembl Database" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        OrthologueConverter converter = new OrthologueConverter(new BufferedReader(new StringReader(input)), itemWriter);
        converter.setParam1("ORG1");
        converter.setParam2("ORG2");
        converter.process();

        Set expected = new HashSet(expectedItems);
        assertEquals(expected, itemWriter.getItems());
    }

    private Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/OrthologueConverterTest.xml"));
    }

    protected OntModel getOwlModel() {
        InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("genomic.n3"));

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(reader, null, "N3");
        return ont;
    }


}
