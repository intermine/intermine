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

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileWriter;
import java.io.Reader;
import java.io.FileReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import org.intermine.xml.full.FullParser;
import org.intermine.dataconversion.DataTranslator;
import org.intermine.dataconversion.DataTranslatorTestCase;
import org.intermine.dataconversion.MockItemReader;
import org.intermine.dataconversion.MockItemWriter;
import org.intermine.dataconversion.XmlConverter;
import org.intermine.metadata.Model;
import org.intermine.xml.full.FullRenderer;

public class PsiDataTranslatorTest extends DataTranslatorTestCase {
    private String tgtNs = "http://www.flymine.org/model/genomic#";

    public void testTranslate() throws Exception {
        Collection srcItems = getSrcItems();

        // print out source items XML - result of running XmlConverter on PSI XML
        FileWriter writer = new FileWriter(new File("src.xml"));
        writer.write(FullRenderer.render(srcItems));
        writer.flush(); writer.close();

        DataTranslator translator = new PsiDataTranslator(new MockItemReader(writeItems(srcItems)),
                                                              getOwlModel(), tgtNs);
        MockItemWriter tgtIw = new MockItemWriter(new LinkedHashMap());
        translator.translate(tgtIw);

        assertEquals(new HashSet(getExpectedItems()), tgtIw.getItems());
    }

    protected String getModelName() {
        return "genomic";
    }

    protected Collection getExpectedItems() throws Exception {
        return FullParser.parse(getClass().getClassLoader().getResourceAsStream("test/PsiDataTranslatorTest_tgt.xml"));
    }

    protected Collection getSrcItems() throws Exception {
        Model psiModel = Model.getInstanceByName("psi");
        Reader srcReader = (new InputStreamReader(getClass().getClassLoader().getResourceAsStream("test/PsiDataTranslatorTest_src.xml")));
        MockItemWriter mockIw = new MockItemWriter(new HashMap());
        Reader xsdReader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("psi.xsd"));

        XmlConverter converter = new XmlConverter(psiModel, xsdReader, mockIw);
        converter.process(srcReader);

        return mockIw.getItems();
    }

    protected OntModel getOwlModel() {
        InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream("genomic.n3"));

        OntModel ont = ModelFactory.createOntologyModel();
        ont.read(reader, null, "N3");
        return ont;
    }
}
