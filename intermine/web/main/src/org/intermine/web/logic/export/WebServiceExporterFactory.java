package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.Results;
import org.intermine.webservice.core.ResultProcessor;
import org.intermine.webservice.core.ResultRowParser;
import org.intermine.webservice.output.Output;


/**
 * Exporter factory.
 * @author Jakub Kulaviak
 **/
public class WebServiceExporterFactory
{    
    private Results results;

    private ResultRowParser rowParser;

    private int firstResult;

    private int maxResults;

    private Output output;

    /**
     * Constructor.
     * @param results results 
     * @param rowParser object parsing row
     * @param firstResult index of first result that should be exported
     * @param maxResults maximum count of exported results
     * @param output output
     */
    public WebServiceExporterFactory(Results results, ResultRowParser rowParser,
            int firstResult, int maxResults, Output output) {
        this.results = results;
        this.rowParser = rowParser;
        this.firstResult = firstResult;
        this.maxResults = maxResults;
        this.output = output;
    }

    /**
     * Creates exporter.
     * @return created exporter
     */
    public Exporter createExporter() {
        return null;
//        return new Exporter() {
//
//            public void export() {
//              ResultProcessor processor = new ResultProcessor(results, rowParser, 
//                      firstResult, maxResults);
//              processor.write(output);              
//            }
//            
//        };
    }
}
