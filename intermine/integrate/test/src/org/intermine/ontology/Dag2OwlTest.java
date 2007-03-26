package org.intermine.ontology;

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

public class Dag2OwlTest extends TestCase{
    String namespace = "http://www.intermine.org/namespace#";

    public void testGenerateClassName() throws Exception {
        DagTerm a = new DagTerm("SO:42", "large gene");
        Dag2Owl owler = new  Dag2Owl(namespace);
        assertEquals(namespace + "LargeGene", owler.generateClassName(a));
    }

    public void testGeneratePropertyName() throws Exception {
        DagTerm a = new DagTerm("SO:42", "large gene");
        DagTerm b = new DagTerm("SO:56", "Transcript");
        Dag2Owl owler = new  Dag2Owl(namespace);
        String got = owler.generatePropertyName(a, b);
        assertEquals(got, namespace + "LargeGene__transcripts", got);
    }
/*
    public void testProcessSimple() {
        OntModel model = ModelFactory.createOntologyModel();
        OntClass cls = model.createClass(namespace + "A");
        Dag2Owl owler = new  Dag2Owl(namespace);
        DagTerm a = new DagTerm("", "A");
        OntClass result = owler.process(a);
        assertEquals(cls, result);
        assertTrue(result == owler.process(a));
    }

    public void testProcessChild() {
        OntModel model = ModelFactory.createOntologyModel();
        OntClass clsA = model.createClass(namespace + "A");
        OntClass clsB = model.createClass(namespace + "B");

        Dag2Owl owler = new  Dag2Owl(namespace);
        DagTerm a = new DagTerm("", "A");
        DagTerm b = new DagTerm("", "B");
        a.getChildren().add(b);
        OntClass result = owler.process(a);

        assertEquals(clsA, result);
        assertEquals(clsB, result.listSubClasses().next());
    }

    public void testProcessComponent() {
        OntModel model = ModelFactory.createOntologyModel();
        OntClass clsA = model.createClass(namespace + "AA");
        OntClass clsB = model.createClass(namespace + "Bb");

        Dag2Owl owler = new  Dag2Owl(namespace);
        DagTerm a = new DagTerm("", "AA");
        DagTerm b = new DagTerm("", "Bb");
        a.getComponents().add(b);
        owler.process(a);

        assertNotNull(owler.getOntModel().getOntProperty(namespace + "AA__bbs"));
        assertNotNull(owler.getOntModel().getOntProperty(namespace + "Bb__AAs"));
        OntProperty abs = owler.getOntModel().getOntProperty(namespace + "AA__bbs");
        OntProperty bas = owler.getOntModel().getOntProperty(namespace + "Bb__AAs");
        assertTrue(abs.hasDomain(clsA));
        assertTrue(abs.hasRange(clsB));
        assertTrue(bas.isInverseOf(abs));
    }
    */
}
