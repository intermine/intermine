package org.flymine.owlproduction;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import junit.framework.*;

import java.io.StringReader;
import java.io.BufferedReader;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;

public class DagToOwlTest extends TestCase
{
    private DagToOwl parser;
    private String namespace = "namespace#";
    private String prefix = "pre";

    public DagToOwlTest(String arg) {
        super(arg);
    }


    public void setUp() throws Exception {
        parser = new DagToOwl(namespace, prefix);
    }

//     public void testProcess() throws Exception {
//         String dag = "$term one ; info\n"
//             + " %term two ; info\n"
//             + "  %term three ; info\n";

//         OntModel example = ModelFactory.createOntologyModel();
//         OntClass parent = example.createClass(namespace + "term one");

//         StringReader reader = new StringReader(dag);
//         OntModel m = parser.process(new BufferedReader(reader));
//     }

    /***
    public void testReadTerms() throws Exception {
        String snippet = "term1\n"
            + " term2\n"
            + "  term3\n"
            + "  term4\n"
            + "term5\n";

        StringReader reader = new StringReader(snippet);
    }
    **/

    public void testMakeOntClassIsa() {
        String line = " %term one ; info";

        OntModel m = ModelFactory.createOntologyModel();
        OntClass parent = m.createClass(namespace + "parent_term");
        parser.parents.push(parent);

        OntClass example= m.createClass(namespace + "term_one");
        example.addSuperClass(parent);
        OntClass ont = parser.makeOntClass(line);
        assertEquals(example, ont);
        assertEquals(example.getSuperClass(), ont.getSuperClass());
    }

    public void testMakeOntClassIsaExtraParents() {
        String line = " %term one ; info % parent two ; info % parent three ; info";

        OntModel m = ModelFactory.createOntologyModel();
        OntClass parent1 = m.createClass(namespace + "parent_one");
        OntClass parent2 = m.createClass(namespace + "parent_two");
        OntClass parent3 = m.createClass(namespace + "parent_three");
        parser.parents.push(parent1);

        OntClass example= m.createClass(namespace + "term_one");
        example.addSuperClass(parent1);
        example.addSuperClass(parent2);
        example.addSuperClass(parent3);

        OntClass ont = parser.makeOntClass(line);
        assertEquals(example, ont);

        Set exParents = new HashSet();
        Iterator iter = example.listSuperClasses();
        while (iter.hasNext()) {
            exParents.add((OntClass) iter.next());
        }

        Set ontParents = new HashSet();
        iter = example.listSuperClasses();
        while (iter.hasNext()) {
            ontParents.add((OntClass) iter.next());
        }
        assertEquals(exParents, ontParents);

    }

    public void testOntClassFromString() throws Exception {
        String test = "name ; other ; info";

        OntModel m = ModelFactory.createOntologyModel();
        OntClass ont = m.createClass(namespace + "name");

        assertEquals(ont, parser.ontClassFromString(test));
    }


//     public void testStripEscaped() throws Exception {
//         assertEquals("%£", parser.stripEscaped("\\%\\£"));
//         assertEquals("a\\d", parser.stripEscaped("a\\d"));
//     }

    public void testTrimLeft() throws Exception {
        assertEquals("string  ", parser.trimLeft("  string  "));
        assertEquals("string  ", parser.trimLeft("string  "));
        assertEquals("", parser.trimLeft(""));

    }


}
