package org.flymine.dataconversion;

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

import java.io.Reader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;

import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.TargetItemsTestCase;
import org.intermine.dataconversion.FileConverter;
import org.intermine.xml.full.FullParser;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TaxonomyConverterTest extends TargetItemsTestCase
{
    public TaxonomyConverterTest(String arg) {
        super(arg);
    }

    public void testTranslate() throws Exception {
        Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/TaxonomyConverterTest_src.txt"));
        MockItemWriter writer = new MockItemWriter(new LinkedHashMap());
        FileConverter converter = new TaxonomyConverter(writer);
        converter.process(reader);
        converter.close();
        assertEquals(new HashSet(getExpectedItems()), writer.getItems());
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/TaxonomyConverterTest_tgt.xml"));
    }

    protected String getModelName() {
        return "genomic";
    }
}