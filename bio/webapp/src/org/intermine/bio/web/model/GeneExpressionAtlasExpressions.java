package org.intermine.bio.web.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Gene Expression Atlas Expressions
 */
public class GeneExpressionAtlasExpressions {

    /** @var holds mapped queue of mapped results
     *
     *   -> map of cell types ("blood")
     *     -> list of probe sets for each cell type
     *       -> map of key-value attributes of that given expression ("pValue", "tStatistic")
     */
    private Map<String, List<Map<String, String>>> results;

    /** @var column keys we have in the results table */
    private ArrayList<String> expressionColumns =  new ArrayList<String>() {{
        add("condition");
        add("expression");
        add("pValue");
        add("tStatistic");
        add("type");
    }};

    /**
     *
     * @return sorted by tissue name
     */
    public Map<String, List<Map<String, String>>> getByName() {
        return results;
    }

    /**
     * Convert Path results into a List (ProteinAtlasDisplayer.java)
     * @param values
     */
    public GeneExpressionAtlasExpressions(ExportResultsIterator values) {
        results = new TreeMap<String, List<Map<String, String>>>(new CaseInsensitiveComparator());

        // ResultElement -> Map of Lists
        while (values.hasNext()) {
            List<ResultElement> valuesRow = values.next();

            // convert into a map
            HashMap<String, String> resultRow = new HashMap<String, String>();
            for (int i=0; i < expressionColumns.size(); i++) {
                resultRow.put(expressionColumns.get(i), valuesRow.get(i).getField().toString());
            }

            // cell type (blood, brain etc)
            String cellKey = resultRow.get("condition");

            // crete new/add to existing cell type expressions list
            List<Map<String, String>> listOfCellTypeExpressions;
            if (results.containsKey(cellKey)) {
                listOfCellTypeExpressions = results.get(cellKey);
            } else {
                listOfCellTypeExpressions = new ArrayList<Map<String, String>>();
                // put
                results.put(cellKey, listOfCellTypeExpressions);
            }

            // push the result
            listOfCellTypeExpressions.add(resultRow);
        }
    }

    /**
     * Comparator used on "conditions" to sort them (their keys) case insensitively
     * @author radek
     *
     */
    public class CaseInsensitiveComparator implements Comparator<String> {

        @Override
        public int compare(String aK, String bK) {
            return aK.toLowerCase().compareTo(bK.toLowerCase());
        }
    }

}
