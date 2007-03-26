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

import java.io.StringReader;
import java.io.BufferedReader;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;

public class MergeSpec2OwlTest extends TestCase{
    String namespace = "http://www.intermine.org/namespace#";
    public void testProcess() throws Exception {
        String n3 = "@prefix : <" + namespace + ">.\n"
            + "@prefix owl: <http://www.w3.org/2002/07/owl#>.\n"
            +":gene2 a owl:Class.\n"
            +":gene1 a owl:Class ; owl:equivalentClass :gene2.";
        OntModel model = new MergeSpec2Owl().process(new BufferedReader(new StringReader(n3)));

        OntModel expectedModel = ModelFactory.createOntologyModel();       
        OntClass cls1 = expectedModel.createClass(namespace + "gene1");
        OntClass cls2 = expectedModel.createClass(namespace + "gene2");
        cls1.addEquivalentClass(cls2);

        assertEquals(cls1, model.getOntClass(namespace + "gene1"));
        assertEquals(cls2, model.getOntClass(namespace + "gene1").listEquivalentClasses().next());
    }
}
