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
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Gene Expression Atlas Tissues Expressions
 */
@SuppressWarnings("serial")
public class GeneExpressionAtlasTissuesExpressions {

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
    * @return the map of lists sorted by highest t-statistic of a cell type expression
    */
    public Map<String, ExpressionList> getByTStatistic() {
        TreeMap<String, ExpressionList> n = new TreeMap<String, ExpressionList>(new ByTStatisticComparator());
        n.putAll(results);
        return n;
    }

    /**
    *
    * @return the map of lists sorted by lowest p-value of a cell type expression
    */
    public Map<String, ExpressionList> getByPValue() {
        TreeMap<String, ExpressionList> n = new TreeMap<String, ExpressionList>(new ByPValueComparator());
        n.putAll(results);
        return n;
    }

    /**
     * Convert Path results into a List (ProteinAtlasDisplayer.java)
     * @param values
     */
    public GeneExpressionAtlasTissuesExpressions(ExportResultsIterator values) {
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

        /** @List store the values */
        private List<Map<String, String>> values;
        /** @float the highest t-statistic */
        public float tStatistic = -1000;
        /** @float the lowest p-value */
        public double pValue = 1;

        public ExpressionList() {
            values = new ArrayList<Map<String, String>>();
        }

        /**
         * Put/add to the list
         * @param resultRow
         */
        public void add(Map<String, String> resultRow) {
            updateTStatistic(resultRow.get("tStatistic"));
            updatePValue(resultRow.get("pValue"));

            values.add(resultRow);
        }

        /**
         * Get the internal list of expressions
         * @return
         */
        public List<Map<String, String>> getValues() {
            return values;
        }

        private void updateTStatistic(String tStatistic) {
            Float f = new Float(tStatistic);
            if (f > this.tStatistic) {
                this.tStatistic = f;
            }
        }

        private void updatePValue(String pValue) {
            double d = Double.parseDouble(pValue);
            if (d < this.pValue) {
                this.pValue = d;
            }
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
            ExpressionList aExpressions = results.get(aK);
            ExpressionList bExpressions = results.get(bK);

            if (aExpressions.tStatistic < bExpressions.tStatistic) {
                return 1;
            } else {
                if (aExpressions.tStatistic > bExpressions.tStatistic) {
                    return -1;
                } else {
                    CaseInsensitiveComparator cic = new CaseInsensitiveComparator();
                    return cic.compare(aK, bK);
                }
            }
        }
    }

    /**
     * Sort by p-value inversely
     * @author radek
     *
     */
    public class ByPValueComparator implements Comparator<String> {

        @Override
        public int compare(String aK, String bK) {
            double aDouble = results.get(aK).pValue;
            double bDouble = results.get(bK).pValue;

            if (aDouble < bDouble) {
                return 1;
            } else {
                if (aDouble > bDouble) {
                    return -1;
                } else {
                    CaseInsensitiveComparator cic = new CaseInsensitiveComparator();
                    return cic.compare(aK, bK);
                }
            }
        }
    }

}
