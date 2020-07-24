package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2020 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;


import java.io.PrintWriter;
import java.util.List;

public class RDFOutput extends Output {
    private PrintWriter writer;
    private int resultsCount;

    public RDFOutput(PrintWriter writer) {
        this.writer = writer;
    }

    /** Forwards data to associated writer
     * @param item data
     * **/
    @Override
    public void addResultItem(List<String> item) {
        //do nothing, we need itemModel to print triples
    }

    /** Forwards data to associated writer
     * @param itemModel data
     * **/
    public void addResultItem(Model itemModel) {
        //generate ntriples
        itemModel.write(writer, "N-TRIPLE");
        resultsCount++;
    }

    @Override
    public void flush() {
        writer.flush();
        writer.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getResultsCount() {
        return resultsCount;
    }
}
