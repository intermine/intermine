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

import java.io.Reader;
import java.io.IOException;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * Convert a "merge specification" document into a skeletal OWL document
 * At present this just uses Jena's N3 input mechanism, so this Class is a hook to allow
 * alternative (non-N3) specification formats at a later date
 * @author Mark Woodbridge
*/
public class MergeSpec2Owl extends URL2Model
{
    /**
     * Convert a merge specification document to the corresponding OWL OntModel
     * @param in the input BufferedReader
     * @return the corresponding OntModel
     * @throws IOException if something goes wrong in accessing the input
     */
    protected OntModel process(Reader in) throws IOException {
        OntModel model = ModelFactory.createOntologyModel();
        model.read(in, null, "N3");
        return model;
    }
}
