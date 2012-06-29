package org.intermine.webservice.server.output;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.intermine.api.InterMineAPI;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.pathquery.Path;
import org.json.JSONArray;

/**
 * @author Alexis Kalderimis
 *
 */
public class JSONRowIterator implements Iterator<JSONArray>
{

    private final ExportResultsIterator subIter;
    private final List<Path> viewPaths = new ArrayList<Path>();
    private final InterMineAPI im;
    private final TableCellFormatter tableCellFormatter;

    /**
     * Constructor
     * @param it An ExportResultsIterator that will be used internally to process the data.
     * @param im A reference to the the API settings bundle.
     */
    public JSONRowIterator(ExportResultsIterator it, InterMineAPI im) {
        this.subIter = it;
        this.im = im;
        this.tableCellFormatter = new TableCellFormatter(im);
        init();
    }

    private void init() {
        viewPaths.addAll(subIter.getViewPaths());
    }

    @Override
    public boolean hasNext() {
        return subIter.hasNext();
    }

    @Override
    public JSONArray next() {
        List<ResultElement> row = subIter.next();
        List<Object> jsonRow = new ArrayList<Object>();
        for (int i = 0; i < row.size(); i++) {
            ResultElement re = row.get(i);
            jsonRow.add(tableCellFormatter.toJSON(re));
        }
        JSONArray next = new JSONArray(jsonRow);
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported for this implementation");
    }

}
