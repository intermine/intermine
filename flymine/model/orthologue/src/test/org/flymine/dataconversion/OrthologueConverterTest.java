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
import org.intermine.dataconversion.TargetItemsTestCase;
import org.intermine.dataconversion.MockItemWriter;

public class OrthologueConverterTest extends TargetItemsTestCase
{
    private String ENDL = System.getProperty("line.separator");

    public void setUp() throws Exception {
        super.setUp();
    }

    public void testProcess() throws Exception {
        // the input file format is 5 tab-delimited columns:
        // gene1   gene2   orthologue_type   algorithm   method   source

        String input = "Title" + ENDL + "GENE1\tGENE2\tSEED\tBRH\tBest Reciprocal Hit\tEnsembl Database" + ENDL;

        MockItemWriter itemWriter = new MockItemWriter(new HashMap());
        OrthologueConverter converter = new OrthologueConverter(new BufferedReader(new StringReader(input)), itemWriter);
        converter.setParam1("ORG1");
        converter.setParam2("ORG2");
        converter.process();

        Set expected = new HashSet(getExpectedItems());
        assertEquals(expected, itemWriter.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/OrthologueConverterTest.xml"));
    }

    protected String getModelName() {
        return "genomic";
    }
}
