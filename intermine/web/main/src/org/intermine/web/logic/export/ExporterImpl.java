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

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.intermine.web.logic.results.ResultElement;


/**
 * Simple exporter exporting data as tab separated, comma separated 
 * and so. It depends at used row formatter.
 * @author Jakub Kulaviak
 **/
public class ExporterImpl implements Exporter
{

    private PrintWriter out;
    private RowFormatter rowFormatter;

    /**
     * Constructor.
     * @param out output stream
     * @param rowFormatter used row formatter.
     */
    public ExporterImpl(OutputStream out, RowFormatter rowFormatter) {
        this.out = new PrintWriter(out);
        this.rowFormatter = rowFormatter;
    }

    /**
     * Exports results.
     * @param results results to be exported
     */
    public void export(List<List<ResultElement>> results) {
        ResultElementConverter converter = new  ResultElementConverter();
        for (List<ResultElement> result : results) {
            out.println(rowFormatter.format(converter.convert(result)));
        }
        out.flush();
    }
}
