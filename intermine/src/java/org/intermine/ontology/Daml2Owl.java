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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
     * @throws IOException if something goes wrong in accessing either of the files
     */
    public static OntModel process(URL daml) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(daml.openStream()));
        StringBuffer sb = new StringBuffer();
        String damlNS = null;
        Pattern pattern = Pattern.compile("xmlns:(.*)=.*" + DAML_NS_PATTERN);
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                line = line.replaceAll("xmlns:" + matcher.group(1), "xmlns:owl");
                line = line.replaceAll(DAML_NS_PATTERN, OWL_NS);
                damlNS = matcher.group(1).replaceAll(" ", "");
            }
            if (damlNS != null) {
                line = line.replaceAll(damlNS + ":", "owl:");
            }
            line.replaceAll("differentIndividualFrom", "differentFrom");
            line.replaceAll("equivalentTo", "sameAs");
            line.replaceAll("sameClassAs", "equivalentClass");
            line.replaceAll("samePropertyAs", "equivalentProperty");
            line.replaceAll("hasClass", "someValuesFrom");
            line.replaceAll("toClass", "allValuesFrom");
            line.replaceAll("UnambiguousProperty", "InverseFunctionalProperty");
            line.replaceAll("UniqueProperty", "FunctionalProperty");
            sb.append(line + ENDL);
        }
        in.close();
        OntModel ontModel = ModelFactory.createOntologyModel();
        ontModel.read(new ByteArrayInputStream(sb.toString().getBytes()), null);
        return ontModel;
    }
}
