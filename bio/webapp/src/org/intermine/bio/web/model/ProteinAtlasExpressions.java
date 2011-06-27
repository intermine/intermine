package org.intermine.bio.web.model;

import java.util.ArrayList;
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
    * @return the map of lists sorted by Cell types count
    */
    public Map<String, ExpressionList> getByCells() {
        TreeMap<String, ExpressionList> n = new TreeMap<String, ExpressionList>(new ByCellCountComparator());
        n.putAll(results);
        return n;
    }

    /**
    *
    * @return the map of lists sorted by Overall level count
    */
    public Map<String, ExpressionList> getByLevel() {
        TreeMap<String, ExpressionList> n = new TreeMap<String, ExpressionList>(new ByOverallLevelComparator());
        n.putAll(results);
        return n;
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
                ExpressionList q = new ExpressionList(15, byLevelComparator);

                // set the organ for this list
                q.setOrganName(resultRow.get("organ"));

                results.put(organSlug, q);
            }
            ExpressionList q = results.get(organSlug);
            q.add(resultRow);

            // update the overall level
            q.stainingLevel.add(q.comparator.evaluate(resultRow.get("level")));

            // setup reliability for this set
            if (reliability == null) {
                reliability = resultRow.get("reliability");
            }
        }
    }

    @SuppressWarnings("serial")
    public class ExpressionList extends PriorityQueue<Map<String, String>> {

        public ByLevelComparator comparator;
        public StainingLevel stainingLevel;
        private String organName;

        public ExpressionList(int i, Comparator<Map<String, String>> comparator) {
            super(i, comparator);
            this.comparator = (ByLevelComparator) comparator;
            stainingLevel = new StainingLevel(this.comparator);
        }

        public void setOrganName(String string) {
            this.organName = string;
        }

        public Map<String, String> getItem() {
            return (Map<String, String>) this.poll();
        }

        public String getOrganName() {
            return organName;
        }

        public StainingLevel getStainingLevel() {
            return stainingLevel;
        }

        public class StainingLevel {

            private Integer overall = 0;
            private Integer count = 0;
            private ByLevelComparator comparator;

            public StainingLevel(ByLevelComparator comparator) {
                this.comparator = comparator;
            }

            public void add(Integer level) {
                this.overall += level;
                this.count += 1;
            }

            public float getLevelValue() {
                return overall.floatValue()/count;
            }

            public String getLevelClass() {
                double doubleValue = (double) getLevelValue();
                if (doubleValue % 1 > 0.5) {
                    return comparator.reverseEvaluate((int) Math.ceil(doubleValue));
                } else {
                    return comparator.reverseEvaluate((int) Math.floor(doubleValue));
                }
            }

        }

    }

    public class StainingLevelEvaluator {

        public static final int STRONG = 3;
        public static final int MODERATE = 2;
        public static final int WEAK = 1;
        public static final int NEGATIVE = -1;
        public static final int OTHER = -2;

        public Integer evaluate(String level) {
            level = level.toLowerCase();

            if ("strong".equals(level)) {
                return STRONG;
            } else if ("moderate".equals(level)) {
                return MODERATE;
            } else if ("weak".equals(level)) {
                return WEAK;
            } else if ("negative".equals(level)) {
                return NEGATIVE;
            }
            return OTHER;
        }

        public String reverseEvaluate(Integer levelValue) {
            switch (levelValue) {
                case STRONG:
                    return "strong";
                case MODERATE:
                    return "moderate";
                case WEAK:
                    return "weak";
                case NEGATIVE:
                default:
                    return "negative";
            }
        }
    }

    public class ByLevelComparator extends StainingLevelEvaluator implements Comparator<Map<String, String>> {

        @Override
        public int compare(Map<String, String> a, Map<String, String> b) {
            Integer aLevel = evaluate(a.get("level"));
            Integer bLevel = evaluate(b.get("level"));

            if (aLevel < bLevel) {
                return 1;
            } else {
                if (aLevel > bLevel) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    }

    public class ByCellCountComparator implements Comparator<String> {

        @Override
        public int compare(String aK, String bK) {
            Integer aSize = (Integer)results.get(aK).size();
            Integer bSize = (Integer)results.get(bK).size();

            if (aSize < bSize) {
                return 1;
            } else {
                if (aSize > bSize) {
                    return -1;
                } else {
                    return aK.compareTo(bK);
                }
            }
        }
    }

    public class ByOverallLevelComparator implements Comparator<String> {

        @Override
        public int compare(String aK, String bK) {
            Float aLevel = results.get(aK).stainingLevel.getLevelValue();
            Float bLevel = results.get(bK).stainingLevel.getLevelValue();

            if (aLevel < bLevel) {
                return 1;
            } else {
                if (aLevel > bLevel) {
                    return -1;
                } else {
                    return aK.compareTo(bK);
                }
            }
        }
    }

}
