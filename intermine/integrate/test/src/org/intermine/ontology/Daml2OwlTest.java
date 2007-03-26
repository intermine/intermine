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

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;

public class Daml2OwlTest extends TestCase{
    String ENDL = System.getProperty("line.separator");

    public void testProcess() throws Exception {
        String daml = "<rdf:RDF"
            + " xmlns:daml=\"http://www.daml.org/2001/03/daml+oil#\""
            + " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"
            + "<daml:UniqueProperty rdf:about=\"http://www.intermine.org/namespace#uniqProp\"/>"
            + "</rdf:RDF>";
        String expectedOwl = "<rdf:RDF" + ENDL
            + "    xmlns:rss=\"http://purl.org/rss/1.0/\"" + ENDL
            + "    xmlns:jms=\"http://jena.hpl.hp.com/2003/08/jms#\"" + ENDL
            + "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"" + ENDL
            + "    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"" + ENDL
            + "    xmlns:owl=\"http://www.w3.org/2002/07/owl#\"" + ENDL
            + "    xmlns:vcard=\"http://www.w3.org/2001/vcard-rdf/3.0#\"" + ENDL
            + "    xmlns:daml=\"http://www.daml.org/2001/03/daml+oil#\"" + ENDL
            + "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\" >" + ENDL
            + "  <rdf:Description rdf:about=\"http://www.intermine.org/namespace#uniqProp\">\n"
            + "    <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#FunctionalProperty\"/>\n"
            + "  </rdf:Description>\n"
            + "</rdf:RDF>\n";
        StringWriter owl = new StringWriter();
        new Daml2Owl().process(new BufferedReader(new StringReader(daml))).write(owl);
        System.out.println(owl.toString());
        assertEquals(expectedOwl, owl.toString());
    }
}
