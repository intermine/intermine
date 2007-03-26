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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;

import org.intermine.util.XmlUtil;

/**
 * This class (simplistically) converts a daml+oil schema to owl by swapping the daml namespace
 * for the owl one and translating all renamed tags
 * (see http://www.w3.org/TR/2003/CR-owl-ref-20030818/#appD).
 * It does not handle daml tags deprecated by, or assimilated into, RDF(S) nor does it handle
 * documents in which the daml namespace is the default.
 * The output should therefore probably be validated using an owl validator
 * @author Mark Woodbridge
*/
public class Daml2Owl extends URL2Model
{
    protected static final String DAML_NS_PATTERN = "http://www.daml.org/2001/03/daml[+]oil#";
    protected static final String ENDL = System.getProperty("line.separator");
    protected String baseURI = "http://www.intermine.org/daml#";


    /**
     * Convert a DAML document to the corresponding OWL OntModel
     * @param in the input document reader
     * @param baseURI the base URI of of the source daml document
     * @return the corresponding OntModel
     * @throws IOException if something goes wrong in accessing the input
     */
    protected OntModel process(Reader in, String baseURI) throws IOException {
        this.baseURI = XmlUtil.correctNamespace(baseURI);
        return process(in);
    }

    /**
     * Convert a DAML document to the corresponding OWL OntModel
     * @param in the input document reader
     * @return the corresponding OntModel
     * @throws IOException if something goes wrong in accessing the input
     */
    protected OntModel process(Reader in) throws IOException {
        BufferedReader reader = new BufferedReader(in);
        StringBuffer sb = new StringBuffer();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            line = line.replaceAll("xmlns:daml=\"" + DAML_NS_PATTERN + "\"",
                                   "xmlns:owl=\"" + OntologyUtil.OWL_NAMESPACE + "\"");
            line = line.replaceAll("daml:differentIndividualFrom", "owl:differentFrom");
            line = line.replaceAll("daml:equivalentTo", "owl:sameAs");
            line = line.replaceAll("daml:sameClassAs", "owl:equivalentClass");
            line = line.replaceAll("daml:samePropertyAs", "owl:equivalentProperty");
            line = line.replaceAll("daml:hasClass", "owl:someValuesFrom");
            line = line.replaceAll("daml:toClass", "owl:allValuesFrom");
            line = line.replaceAll("daml:UnambiguousProperty", "owl:InverseFunctionalProperty");
            line = line.replaceAll("daml:UniqueProperty", "owl:FunctionalProperty");
            line = line.replaceAll("daml:", "owl:");

            if (line.startsWith("<rdf:Description", line.indexOf('<'))) {
                String fragment = OntologyUtil
                    .validResourceName(line.substring(line.indexOf('#') + 1,
                                                      line.lastIndexOf('"')));
                line = line.substring(0, line.indexOf('#')) + fragment + "\">";
            }
            if (!line.startsWith("<oiled:creationDate>", line.indexOf('<'))
                && !line.startsWith("<oiled:creator>", line.indexOf('<'))) {
                sb.append(line + ENDL);
            }
        }
        reader.close();
        OntModel ontModel = ModelFactory.createOntologyModel();
        ontModel.read(new ByteArrayInputStream(sb.toString().getBytes()), baseURI);
        return ontModel;
    }

    /**
     * Run conversion from DAML to OWL format
     * @param args damlFilename, owlFilename
     * @throws Exception if anthing goes wrong
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new Exception("Usage: Daml2Owl damlfile owlfile baseURI");
        }

        String damlFilename = args[0];
        String owlFilename = args[1];

        try {
            Daml2Owl owler = new Daml2Owl();
            BufferedWriter out = new BufferedWriter(new FileWriter(new File(owlFilename)));
            if (args.length > 2) {
                owler.process(new FileReader(new File(damlFilename)), args[2]).write(out, "N3");
            } else {
                owler.process(new FileReader(new File(damlFilename))).write(out, "N3");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
