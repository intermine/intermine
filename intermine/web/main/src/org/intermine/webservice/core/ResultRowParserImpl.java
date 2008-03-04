package org.intermine.webservice.core;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ResultsRow;
import org.intermine.path.Path;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.query.PathQuery;


/**
 * Parser that parses ResultsRow to list of strings. 
 * @author Jakub Kulaviak
 **/
public class ResultRowParserImpl implements ResultRowParser
{

    private PathQuery pathQuery;
    
    private Model model;
    
    private HashMap<String, Integer> pathToIndex;
    
    /**
     * I'm afraid all this parameter staff is needed for parsing of ResultsRow. 
     * @param pathQuery pathQuery
     * @param model model
     * @param pathToIndex mapping between path and index of object in ResultsRow
     */
    public ResultRowParserImpl(PathQuery pathQuery, Model model, 
            HashMap<String, Integer> pathToIndex) {
        this.pathQuery = pathQuery;
        this.model = model;
        this.pathToIndex = pathToIndex;
    }
    
    /** 
     * Parses result row according to the view of PathQuery that created this result.
     * @param resultsRow result row
     * @see org.intermine.webservice.query.result.
     *  ResultRowParser#parse(org.intermine.objectstore.query.ResultsRow)
     *  @return result row as a list of strings
     */
    public List<String> parse(ResultsRow resultsRow) {
        ArrayList<String> ret = new ArrayList<String>();
        for (Iterator<Path> iter = pathQuery.getView().iterator(); iter
                .hasNext();) {
            Path columnPath = (Path) iter.next();
            String columnName = columnPath.toStringNoConstraints();
            int columnIndex = ((Integer) pathToIndex.get(columnName))
                    .intValue();
            Object o = resultsRow.get(columnIndex);
            String type = TypeUtil.unqualifiedName(columnPath
                    .getLastClassDescriptor().getName());
            String fieldName = columnName
                    .substring(columnName.lastIndexOf(".") + 1);
            Path path = new Path(model, type + '.' + fieldName);
            Object fieldValue = path.resolve(o);
            if (fieldValue != null) {
                ret.add(fieldValue.toString());
            } else {
                ret.add("");
            }
        }
        return ret;
    }
}


