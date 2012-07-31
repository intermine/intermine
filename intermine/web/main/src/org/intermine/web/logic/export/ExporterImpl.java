package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2012 FlyMine
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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.Path;


/**
 * Simple exporter exporting data as tab separated, comma separated
 * and so. It depends at used row formatter.
 *
 * @author Jakub Kulaviak
 **/
public class ExporterImpl implements Exporter
{

    private PrintWriter out;

    private RowFormatter rowFormatter;

    private int writtenResultsCount = 0;

    private final List<String> headers;

    /**
     * Constructor.
     * @param out output stream
     * @param rowFormatter used row formatter.
     */
    public ExporterImpl(OutputStream out, RowFormatter rowFormatter) {
        this.headers = null;
        this.out = new PrintWriter(out);
        this.rowFormatter = rowFormatter;
    }

    /**
     * Constructor.
     * @param out output stream
     * @param rowFormatter used row formatter.
     * @param separator line separator
     * @param headers if non-null, a list of the column headers which will be written by export()
     */
    public ExporterImpl(OutputStream out, RowFormatter rowFormatter, String separator,
                        List<String> headers) {
        this.headers = headers;
        if (separator.equals(Exporter.WINDOWS_SEPARATOR)) {
            this.out = new CustomPrintWriter(out, Exporter.WINDOWS_SEPARATOR);
        } else {
            this.out = new PrintWriter(out);
        }
        this.rowFormatter = rowFormatter;
    }

    /**
     * {@inheritDoc}
     */
    public void export(Iterator<? extends List<ResultElement>> resultIt,
            Collection<Path> unionPathCollection, Collection<Path> newPathCollection) {
        try {
            if (headers != null) {
                out.println(rowFormatter.format(new ArrayList<Object>(headers)));
            }
            out.flush();
            ResultElementConverter converter = new ResultElementConverter();
            while (resultIt.hasNext()) {
                List<ResultElement> result = resultIt.next();
                out.println(rowFormatter.format(converter.convert(result,
                        unionPathCollection, newPathCollection)));
                writtenResultsCount++;
                if (writtenResultsCount % 10000 == 0) {
                    if (out.checkError()) {
                        throw new ExportException("Output closed");
                    }
                }
            }
            out.flush();
        } catch (RuntimeException e) {
            throw new ExportException("Export failed.", e);
        }
    }

    @Override
    public void export(Iterator<? extends List<ResultElement>> resultIt) {
        export(resultIt, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    }

    /**
     * {@inheritDoc}
     */
    public int getWrittenResultsCount() {
        return writtenResultsCount;
    }

    /**
     * {@inheritDoc}
     * Universal exporter.
     * @return always true
     */
    public boolean canExport(List<Class<?>> clazzes) {
        return true;
    }
}
