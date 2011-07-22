package org.intermine.bio.web.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.bio.web.model.ProteinAtlasExpressions.ByCellCountComparator;
import org.intermine.bio.web.model.ProteinAtlasExpressions.ExpressionList;

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
     *     -> list of probe sets for each cell type (ExpressionList obj)
     *       -> map of key-value attributes of that given expression ("pValue", "tStatistic")
     */
    private Map<String, ExpressionList> results;

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
    public Map<String, ExpressionList> getByName() {
        return results;
    }

    /**
    *
    * @return the map of lists sorted by Cell types count
    */
    public Map<String, ExpressionList> getByCells() {
        TreeMap<String, ExpressionList> n = new TreeMap<String, ExpressionList>(new ByTStatisticComparator());
        n.putAll(results);
        return n;
    }

    /**
     * Convert Path results into a List (ProteinAtlasDisplayer.java)
     * @param values
     */
    public GeneExpressionAtlasExpressions(ExportResultsIterator values) {
        results = new TreeMap<String, ExpressionList>(new CaseInsensitiveComparator());

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
            ExpressionList listOfCellTypeExpressions;
            if (results.containsKey(cellKey)) {
                listOfCellTypeExpressions = results.get(cellKey);
            } else {
                listOfCellTypeExpressions = new ExpressionList();
                // put
                results.put(cellKey, listOfCellTypeExpressions);
            }

            // push the result
            listOfCellTypeExpressions.add(resultRow);
        }
    }

    /**
     * Represents a list of expressions (taken from multiple probe sets) for a given tissue type
     * @author radek
     *
     */
    public class ExpressionList {

        private List<Map<String, String>> values;

        public ExpressionList() {
            values = new ArrayList<Map<String, String>>();
        }

        /**
         * Put/add to the list
         * @param resultRow
         */
        public void add(Map<String, String> resultRow) {
            values.add(resultRow);
        }

        /**
         * Get the internal list of expressions
         * @return
         */
        public List<Map<String, String>> getValues() {
            return values;
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

    /**
     * Sort by t-statistic
     * @author radek
     *
     */
    public class ByTStatisticComparator implements Comparator<String> {

        @Override
        public int compare(String aK, String bK) {
            // fetch the underlying lists
            ExpressionList aCells = results.get(aK);
            ExpressionList bCells = results.get(bK);



            /*Integer aSize = (Integer)results.get(aK).getValues().size();
            Integer bSize = (Integer)results.get(bK).getValues().size();

            if (aSize < bSize) {
                return 1;
            } else {
                if (aSize > bSize) {
                    return -1;
                } else {
                    return aK.compareTo(bK);
                }
            }*/
            return 0;
        }
    }

}
