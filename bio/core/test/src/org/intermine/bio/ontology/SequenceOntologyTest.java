package org.intermine.bio.ontology;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.File;
import java.net.URISyntaxException;
import java.util.Set;

import junit.framework.TestCase;

import org.intermine.metadata.Model;

public class SequenceOntologyTest extends TestCase
{
    protected static final String ENDL = System.getProperty("line.separator");

    public SequenceOntologyTest(String arg) {
        super(arg);
    }

    public void testGetAllClasses()  throws URISyntaxException {
        File terms = new File(SequenceOntologyTest.class.getClassLoader().getResource("so_terms-test").toURI());
        File oboFile = new File(SequenceOntologyTest.class.getClassLoader().getResource("SequenceOntology.obo").toURI());
        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology(oboFile, terms);

        Model model = so.getModel();
        assertEquals(model.toString(), getModelXML(), model.toString());
    }
    public void testParents() {
        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology();
        String className = "exon";
        Set<String> parents = so.getAllPartOfs(className);
        System.out.print(parents);
        assertEquals(2, parents.size());
        assertTrue(parents.contains("transcript"));
    }
    public void testNoFile() {
        SequenceOntology so = SequenceOntologyFactory.getSequenceOntology();
        Model model = so.getModel();
        assertEquals(model.toString(), getModelXML(), model.toString());
    }

    private String getModelXML() {
        StringBuffer sb = new StringBuffer();
        sb.append("<model name=\"so\" package=\"org.intermine.model.bio\">" + ENDL);
        sb.append("<class name=\"Chromosome\" extends=\"SequenceFeature\" is-interface=\"true\"></class>" + ENDL);
        sb.append("<class name=\"Exon\" extends=\"SequenceFeature\" is-interface=\"true\"></class>" + ENDL);
        sb.append("<class name=\"Gene\" extends=\"SequenceFeature\" is-interface=\"true\"></class>" + ENDL);
        sb.append("<class name=\"SequenceFeature\" is-interface=\"true\"></class>" + ENDL);
        sb.append("</model>");
        return sb.toString();

    }


}
