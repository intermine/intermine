package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2014 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.intermine.metadata.Model;

public class SequenceOntologyTest extends TestCase
{
    protected static final String ENDL = System.getProperty("line.separator");


    public SequenceOntologyTest(String arg) throws IOException {
        super(arg);

    }

    public void testGetAllClasses()  throws URISyntaxException, IOException {
        File terms = new File(SequenceOntologyTest.class.getClassLoader().getResource("so_terms-test").toURI());
        File oboFile = new File(SequenceOntologyTest.class.getClassLoader().getResource("SequenceOntology.obo").toURI());
        String targetXML = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("so-target.xml"));
        SequenceOntologyFactory.reset();
        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology(oboFile, terms);

        Model model = so.getModel();
        assertEquals("testing using so-test failed", targetXML, model.toString());
    }

    // test generating SO without providing the data file. use the default
    public void testNoFile() throws IOException {
        SequenceOntologyFactory.reset();
        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology();
        Model model = so.getModel();
        String targetXML = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("so-target-default.xml"));
        assertEquals(model.toAdditionsXML(), targetXML, model.toString());
    }

    public void testParents() {
        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology();
        String className = "exon";
        Set<String> parents = so.getAllPartOfs(className);
        System.out.print(parents);
        assertEquals(2, parents.size());
        assertTrue(parents.contains("transcript"));
    }
}
