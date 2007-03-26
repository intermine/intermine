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

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import com.hp.hpl.jena.ontology.OntModel;

/**
 * Class that maps a document specified by URL to an OWL model
 * Subclasses should override the process(BufferedReader) method
 * @author Mark Woodbridge
*/
public abstract class URL2Model
{
    /**
     * Convert a document to the corresponding OWL OntModel
     * @param url the location of the input document
     * @return the corresponding OntModel
     * @throws IOException if something goes wrong in accessing the input
     */
    public  OntModel process(URL url) throws IOException {
        return process(new BufferedReader(new InputStreamReader(url.openStream())));
    }

    /**
     * Convert a document to the corresponding OWL OntModel
     * @param in the input BufferedReader
     * @return the corresponding OntModel
     * @throws IOException if something goes wrong in accessing the input
     */
    protected abstract OntModel process(Reader in) throws IOException;
}
