package org.flymine.ontology;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;

public class Dag2OwlTest extends TestCase{
    String namespace = "http://www.flymine.org/namespace#";

    public void testGenerateClassName() throws Exception {
        DagTerm a = new DagTerm("SO:42", "");
        Dag2Owl owler = new  Dag2Owl(namespace);
        assertEquals(namespace + "SO_42", owler.generateClassName(a));
    }

    public void testPropertyName() throws Exception {
        DagTerm a = new DagTerm("SO:42", "");
        DagTerm b = new DagTerm("SO:56", "");
        Dag2Owl owler = new  Dag2Owl(namespace);
        assertEquals(namespace + "SO_42_SO_56", owler.generatePropertyName(a, b));
    }

    public void testProcessSimple() {
        OntModel model = ModelFactory.createOntologyModel();
        OntClass cls = model.createClass(namespace + "A");
        Dag2Owl owler = new  Dag2Owl(namespace);
        DagTerm a = new DagTerm("A", "");
        OntClass result = owler.process(a);
        assertEquals(cls, result);
        assertTrue(result == owler.process(a));
    }

    public void testProcessChild() {
        OntModel model = ModelFactory.createOntologyModel();
        OntClass clsA = model.createClass(namespace + "A");
        OntClass clsB = model.createClass(namespace + "B");

        Dag2Owl owler = new  Dag2Owl(namespace);
        DagTerm a = new DagTerm("A", "");
        DagTerm b = new DagTerm("B", "");
        a.getChildren().add(b);
        OntClass result = owler.process(a);

        assertEquals(clsA, result);
        assertEquals(clsB, result.listSubClasses().next());
    }

    public void testProcessComponent() {
        OntModel model = ModelFactory.createOntologyModel();
        OntClass clsA = model.createClass(namespace + "A");
        OntClass clsB = model.createClass(namespace + "B");

        Dag2Owl owler = new  Dag2Owl(namespace);
        DagTerm a = new DagTerm("A", "");
        DagTerm b = new DagTerm("B", "");
        a.getComponents().add(b);
        owler.process(a);
        OntProperty prop = (OntProperty) owler.getOntModel().getOntClass(namespace + "B").listDeclaredProperties().next();

        assertEquals(clsB, prop.getDomain());
        assertEquals(clsA, prop.getRange());
    }
}
