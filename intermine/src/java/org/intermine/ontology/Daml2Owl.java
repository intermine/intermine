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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * This class (simplistically) converts a daml+oil schema to owl by swapping the daml namespace
 * for the owl one and translating all renamed tags
 * (see http://www.w3.org/TR/2003/CR-owl-ref-20030818/#appD).
 * It does not handle daml tags deprecated by, or assimilated into, RDF(S) nor does it handle
 * documents in which the daml namespace is the default.
 * The output should therefore probably be validated using an owl validator
 * @author Mark Woodbridge
*/
public class Daml2Owl
{
    protected static final String DAML_NS_PATTERN = "http://www.daml.org/2001/03/daml[+]oil#";
    protected static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
    protected static final String ENDL = System.getProperty("line.separator");

    /**
     * Convert a DAML document to the corresponding OWL OntModel
     * @param daml the input document
     * @return the corresponding OntModel
     * @throws IOException if something goes wrong in accessing the input
     */
    public static OntModel process(URL daml) throws IOException {
        return process(new BufferedReader(new InputStreamReader(daml.openStream())));
    }

    /**
     * Convert a DAML document to the corresponding OWL OntModel
     * @param in the input document reader
     * @return the corresponding OntModel
     * @throws IOException if something goes wrong in accessing the input
     */
    protected static OntModel process(BufferedReader in) throws IOException {
        StringBuffer sb = new StringBuffer();
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            line = line.replaceAll("xmlns:daml=\"" + DAML_NS_PATTERN + "\"",
                                   "xmlns:owl=\"" + OWL_NS + "\"");
            line = line.replaceAll("daml:differentIndividualFrom", "owl:differentFrom");
            line = line.replaceAll("daml:equivalentTo", "owl:sameAs");
            line = line.replaceAll("daml:sameClassAs", "owl:equivalentClass");
            line = line.replaceAll("daml:samePropertyAs", "owl:equivalentProperty");
            line = line.replaceAll("daml:hasClass", "owl:someValuesFrom");
            line = line.replaceAll("daml:toClass", "owl:allValuesFrom");
            line = line.replaceAll("daml:UnambiguousProperty", "owl:InverseFunctionalProperty");
            line = line.replaceAll("daml:UniqueProperty", "owl:FunctionalProperty");
            line = line.replaceAll("daml:", "owl:");
            sb.append(line + ENDL);
        }
        in.close();
        OntModel ontModel = ModelFactory.createOntologyModel();
        ontModel.read(new ByteArrayInputStream(sb.toString().getBytes()), null);
        return ontModel;
    }
}
