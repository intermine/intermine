package org.intermine.bio.web.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * Protein Atlas Expressions
 */
public class ProteinAtlasExpressions {

    /** @var holds list of (initial) results */
    private ArrayList<Map<String, String>> results;

    /** @var the different expressions */
    private Expressions stainingExpressions;
    private Expressions aPEExpressions;

    /** @var column keys we have in the results table */
    private ArrayList<String> expressionColumns =  new ArrayList<String>() {{
        add("cellType");
        add("expressionType");
        add("level");
        add("reliability");
        add("tissue");
    }};

    /**
     * Convert Path results into a List (ProteinAtlasDisplayer.java)
     * @param values
     */
    public ProteinAtlasExpressions(ExportResultsIterator values) {
        results = new ArrayList<Map<String, String>>();

        while (values.hasNext()) {
            List<ResultElement> valuesRow = values.next();

            LinkedHashMap<String, String> resultRow = new LinkedHashMap<String, String>();
            // convert into a map
            for (int i=0; i < expressionColumns.size(); i++)  {
                resultRow.put(expressionColumns.get(i), valuesRow.get(i).getField().toString());
            }
            results.add(resultRow);
        }
    }

    /**
     * Get Staining Expressions
     * @return
     */
    public Expressions getStainingExpressions() {
        if (stainingExpressions == null) {
            stainingExpressions = new StainingExpressions(results);
        }
        return stainingExpressions;
    }

    /**
     * Get APE Expressions
     * @return
     */
    public Expressions getAPEExpressions() {
        if (aPEExpressions == null) {
            aPEExpressions = new APEExpressions(results);
        }
        return aPEExpressions;
    }

    /**
     * Hold Expressions of a given type (Staining/APE)
     * @author radek
     *
     */
    public class Expressions {

        protected ArrayList<Map<String, String>> expressions = new ArrayList<Map<String, String>>();

        public Expressions(ArrayList<Map<String, String>> results, String type) {
            for (Map<String, String> expression : results) {
                if (expression.get("expressionType").toLowerCase().contains(type.toLowerCase())) {
                    expressions.add(expression);
                }
            }
        }

        public List<Map<String, String>> getFilter() {
            return results;
        }

        /**
         * Uncached expression filtered on reliability and level conditions
         * @param reliability
         * @param level
         * @return
         */
        public List<Map<String, String>> getFilter(String reliability, String level) {
            List<Map<String, String>> result = new ArrayList<Map<String, String>>();

            for (Map<String, String> expression : results) {
                if (
                        reliability.toLowerCase().equals(expression.get("reliability").toLowerCase()) &&
                        level.toLowerCase().equals(expression.get("level").toLowerCase())
                        ) {
                    result.add(expression);
                }
            }

            return result;
        }

    }

    public class StainingExpressions extends Expressions {

        public StainingExpressions(ArrayList<Map<String, String>> results) {
            super(results, "Staining");
        }

    }

    public class APEExpressions extends Expressions {

        public APEExpressions(ArrayList<Map<String, String>> results) {
            super(results, "APE");
        }
    }

}
