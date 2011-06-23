package org.intermine.bio.web.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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
 * Protein Atlas Expressions
 */
public class ProteinAtlasExpressions {

    /** @var holds mapped queue of mapped results (organ -> queue of results by db column key) */
    private TreeMap<String, ExpressionList> results;

    /** @var reliability of expressions */
    private String reliability;

    /** @var column keys we have in the results table */
    private ArrayList<String> expressionColumns =  new ArrayList<String>() {{
        add("cellType");
        add("expressionType");
        add("level");
        add("reliability");
        add("tissue");
    }};

    // XXX remove
    private ArrayList<String> organs =  new ArrayList<String>() {{
        add("Central nervous system (Brain)");
        add("Blood and immune system (Hematopoietic)");
        add("Liver and pancreas");
        add("Digestive tract (GI-tract)");
        add("Respiratory system (Lung)");
        add("Cardiovascular system (Heart and blood vessels)");
        add("Breast and female reproductive system (Female tissues)");
        add("Placenta");
        add("Male reproductive system (Male tissues)");
        add("Urinary tract (Kidney and bladder)");
        add("Skin and soft tissues");
        add("Endocrine glands");
    }};

    /**
     *
     * @return the map of lists sorted by Organ name
     */
    public Map<String, ExpressionList> getByOrgan() {
        return results;
    }

    /**
     *
     * @return the expressions reliability
     */
    public String getReliability() {
        return reliability;
    }

    /**
     * Convert Path results into a List (ProteinAtlasDisplayer.java)
     * @param values
     */
    public ProteinAtlasExpressions(ExportResultsIterator values) {
        // TODO add a clause correctly adding expressions to the appropriate organ list

        results = new TreeMap<String, ExpressionList>();
        Comparator<Map<String, String>> byLevelComparator = new ByLevelComparator();

        // ResultElement -> Map of Lists
        while (values.hasNext()) {
            List<ResultElement> valuesRow = values.next();

            LinkedHashMap<String, String> resultRow = new LinkedHashMap<String, String>();
            // convert into a map
            for (int i=0; i < expressionColumns.size(); i++)  {
                resultRow.put(expressionColumns.get(i), valuesRow.get(i).getField().toString());
            }

            // XXX remove
            Double which = Math.random() * 10;
            resultRow.put("organ", organs.get((int) (which - (which % 1))));

            // add to the appropriate organ list
            String organSlug = resultRow.get("organ").toLowerCase().replaceAll("[^a-z0-9-]", "");
            if (!results.containsKey(organSlug)) {
                results.put(organSlug, new ExpressionList(15, byLevelComparator));
            }
            PriorityQueue<Map<String, String>> q = results.get(organSlug);
            q.add(resultRow);

            // setup reliability for this set
            if (reliability == null) {
                reliability = resultRow.get("reliability");
            }
        }
    }

    @SuppressWarnings("serial")
    public class ExpressionList extends PriorityQueue<Map<String, String>> {

        public ExpressionList(int i, Comparator<Map<String, String>> comparator) {
            super(i, comparator);
        }

        public Map<String, String> getItem() {
            return (Map<String, String>) this.poll();
        }
    }

    public class ByLevelComparator implements Comparator<Map<String, String>> {

        private Integer levelOrder(String level) {
            if ("strong".equals(level)) {
                return 1;
            } else if ("moderate".equals(level)) {
                return 2;
            } else if ("weak".equals(level)) {
                return 3;
            } else if ("negative".equals(level)) {
                return 4;
            } else {
                return 5;
            }
        }

        @Override
        public int compare(Map<String, String> a, Map<String, String> b) {
            Integer aLevel = levelOrder(a.get("level").toLowerCase());
            Integer bLevel = levelOrder(b.get("level").toLowerCase());

            if (aLevel < bLevel) {
                return -1;
            } else {
                if (aLevel > bLevel) {
                    return 1;
                } else {
                    return 0;
                }
            }
        }
    }

}
