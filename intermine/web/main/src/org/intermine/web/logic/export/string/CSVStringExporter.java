package org.intermine.web.logic.export.string;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.io.PrintWriter;

import org.intermine.web.logic.export.rowformatters.CSVRowFormatter;


/**
 * Exporter exporting table of strings in the format of comma separated strings.
 * @author Jakub Kulaviak
 **/
public class CSVStringExporter extends StringExporterImpl
{

    /**
     * Constructor
     * @param writer used writer
     */
    public CSVStringExporter(PrintWriter writer) {
        super(writer, new CSVRowFormatter());
    }
}
