package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2021 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLTestCase;
import org.intermine.metadata.Model;
import org.xml.sax.SAXException;

public class SequenceOntologyTest extends XMLTestCase
{
    protected static final String ENDL = System.getProperty("line.separator");


    public SequenceOntologyTest(String arg) throws IOException {
        super(arg);

    }

    public void testGetAllClasses()  throws URISyntaxException, IOException, SAXException, ParserConfigurationException {
        File terms = new File(SequenceOntologyTest.class.getClassLoader().getResource("so_terms-test").toURI());
        File oboFile = new File(SequenceOntologyTest.class.getClassLoader().getResource("SequenceOntology.obo").toURI());
        String targetXML = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("so-target.xml"));
        SequenceOntologyFactory.reset();
        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology(oboFile, terms);
        Model model = so.getModel();
        assertXMLEqual(targetXML, model.toString());
    }

    // test generating SO without providing the data file. use the default
    public void testNoFile() throws IOException, SAXException, ParserConfigurationException {
        SequenceOntologyFactory.reset();
        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology();
        Model model = so.getModel();
        String targetXML = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("so-target-default.xml"));

//        PrintWriter writer = new PrintWriter("so-target-default-ACTUAL.xml", "UTF-8");
//        writer.println(model.toString());
//        writer.close();

        assertXMLEqual(targetXML, model.toString());
    }

    public void testParents() {
        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology();
        String className = "exon";
        Set<String> parents = so.getAllPartOfs(className);

        assertEquals(2, parents.size());
        assertTrue(parents.contains("transcript"));
    }
}
