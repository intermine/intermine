package org.intermine.bio.web.model;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Protein Atlas Expressions
 * @author Fengyuan
 */
public class ProteinAtlasExpressions
{

    /** @var holds mapped queue of mapped results (organ -> queue of results by db column key) */
    private TreeMap<String, ExpressionList> results;

    /** @var reliability of expressions */
    private String reliability;

    /** @var expression type for these expressions */
    private ExpressionType type;

    /** @var column keys we have in the results table */
    private ArrayList<String> expressionColumns =  new ArrayList<String>() {
        {
            add("cellType");
            add("expressionType");
            add("level");
            add("reliability");
            add("tissue");
            add("organ");
        }
    };

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
        TreeMap<String, ExpressionList> n = new TreeMap<String, ExpressionList>(
                new ByCellCountComparator());
        n.putAll(results);
        return n;
    }

    /**
    *
    * @return the map of lists sorted by Overall level count
    */
    public Map<String, ExpressionList> getByLevel() {
        TreeMap<String, ExpressionList> n = new TreeMap<String, ExpressionList>(
                new ByOverallLevelComparator());
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
     *
     * @return the expressions type (Staining vs APE)
     */
    public ExpressionType getType() {
        return type;
    }

    /**
     * Convert Path results into a List (ProteinAtlasDisplayer.java)
     * @param values values to export
     */
    public ProteinAtlasExpressions(ExportResultsIterator values) {
        results = new TreeMap<String, ExpressionList>();
        Comparator<String> byLevelComparator = new ByLevelComparator();

        // ResultElement -> Map of Lists
        while (values.hasNext()) {
            List<ResultElement> valuesRow = values.next();

            LinkedHashMap<String, String> resultRow = new LinkedHashMap<String, String>();
            // convert into a map
            for (int i = 0; i < expressionColumns.size(); i++)  {
                resultRow.put(expressionColumns.get(i), valuesRow.get(i).getField().toString());
            }

            // add to the appropriate organ list
            String organSlug = resultRow.get("organ").toLowerCase().replaceAll("[^a-z0-9-]", "");
            if (!results.containsKey(organSlug)) {
                ExpressionList q = new ExpressionList(byLevelComparator);

                // set the organ for this list
                q.setOrganName(resultRow.get("organ"));

                results.put(organSlug, q);
            }
            ExpressionList q = results.get(organSlug);
            q.add(resultRow);

            // update the overall level
            q.stainingLevel.add(q.comparator.evaluate(resultRow.get("level")));

            // setup reliability, type for this set
            if (reliability == null) {
                reliability = resultRow.get("reliability");
                this.type = new ExpressionType(resultRow.get("expressionType"));
            }
        }

    }

    /**
     * Represents the type (APE/Staining)
     * @author radek
     *
     */
    public class ExpressionType
    {

        private String text;
        private String clazz;

        /**
         * @param dbString type of expression
         */
        public ExpressionType(String dbString) {
            this.text = dbString;
            this.clazz = (this.text.toLowerCase().indexOf("ape") >= 0) ? "ape" : "staining";
        }

        /**
         * @return true of type is ape
         */
        public Boolean getIsApe() {
            return ("ape".equals(this.clazz));
        }

        /**
         * @return text
         */
        public String getText() {
            return this.text;
        }

        /**
         * @return class
         */
        public String getClazz() {
            return this.clazz;
        }
    }

    /**
     * Represents a treemap structure of maps of expressions
     * @author radek
     *
     */
    public class ExpressionList
    {
        private ByLevelComparator comparator;
        private StainingLevel stainingLevel;
        private String organName;
        private TreeMap<String, Map<String, String>> values;

        /**
         * Constructor
         * @param comparator object used to compare
         */
        public ExpressionList(Comparator<String> comparator) {
            this.comparator = (ByLevelComparator) comparator;

            values = new TreeMap<String, Map<String, String>>(this.comparator);
            stainingLevel = new StainingLevel(this.comparator);
        }

        /**
         * Put/add to the map of expressions
         * @param resultRow results row
         */
        public void add(Map<String, String> resultRow) {
            values.put(resultRow.get("level"), resultRow);
        }

        /**
         * Get the internal map of expressions
         * @return the internal map of expressions
         */
        public Map<String, Map<String, String>> getValues() {
            return values;
        }

        /**
         * @param string name of organ
         */
        public void setOrganName(String string) {
            this.organName = string;
        }

        /**
         * @return name of organ
         */
        public String getOrganName() {
            return organName;
        }

        /**
         * @return staining level
         */
        public StainingLevel getStainingLevel() {
            return stainingLevel;
        }

        /**
         * Gives us 'stats' on the overall staining level of the Expressions
         * @author radek
         *
         */
        public class StainingLevel
        {

            private Integer overall = 0;
            private Integer count = 0;
            private ByLevelComparator levelComparator;

            /**
             * @param levelComparator comparator
             */
            public StainingLevel(ByLevelComparator levelComparator) {
                this.levelComparator = levelComparator;
            }

            /**
             * @param level level
             */
            public void add(Integer level) {
                this.overall += level;
                this.count += 1;
            }

            /**
             * Get the float value representing the average Expression level
             * @return the float value representing the average Expression level
             */
            public float getLevelValue() {
                return overall.floatValue() / count;
            }

            /**
             * Convert the overall staining level of all Expressions into an average
             * (ceiled/floored) value
             * @return average value
             */
            public String getLevelClass() {
                double doubleValue = getLevelValue();
                if (doubleValue % 1 > 0.5) {
                    return levelComparator.reverseEvaluate((int) Math.ceil(doubleValue));
                } else {
                    return levelComparator.reverseEvaluate((int) Math.floor(doubleValue));
                }
            }

        }
    }

    /**
     * Determines the "points" staining levels will get
     * @author radek
     *
     */
    public class StainingLevelEvaluator
    {

        //private static final int STRONG = 3;
        private static final int HIGH = 3;
        //private static final int MODERATE = 2;
        private static final int MEDIUM = 2;
        //private static final int WEAK = 1;
        private static final int LOW = 1;
        //private static final int NEGATIVE = -1;
        private static final int NONE = -1;
        private static final int OTHER = -2;

        /**
         * String representing the level to integer conversion
         * @param strLevel level
         * @return  level to integer conversion
         */
        public Integer evaluate(String strLevel) {
            String level = strLevel.toLowerCase();

            if ("strong".equals(level) || "high".equals(level)) {
                return HIGH;
            } else if ("moderate".equals(level) || "medium".equals(level)) {
                return MEDIUM;
            } else if ("weak".equals(level) || "low".equals(level)) {
                return LOW;
            } else if ("negative".equals(level) || "none".equals(level)) {
                return NONE;
            }
            return OTHER;
        }

        /**
         * Integer value to string conversion
         *
         * @param levelValue integer representation of the level
         * @return value in word form
         */
        public String reverseEvaluate(Integer levelValue) {
            switch (levelValue) {
                case HIGH:
                    return "strong";
                case MEDIUM:
                    return "moderate";
                case LOW:
                    return "weak";
                case NONE:
                default:
                    return "negative";
            }
        }
    }

    /**
     * Comparator used on Expressions to order them by staining level
     * @author radek
     *
     */
    public class ByLevelComparator extends StainingLevelEvaluator implements Comparator<String>
    {

        @Override
        public int compare(String aK, String bK) {
            Integer aLevel = evaluate(aK);
            Integer bLevel = evaluate(bK);

            if (aLevel < bLevel) {
                return 1;
            } else {
                //if (aLevel > bLevel) {
                return -1;
                //} else {
                //    return 1;
                //}
            }
        }
    }

    /**
     * Reorder a TreeMap based on the number of cells contained in Expressions
     * @author radek
     *
     */
    public class ByCellCountComparator implements Comparator<String>
    {

        @Override
        public int compare(String aK, String bK) {
            Integer aSize = results.get(aK).getValues().size();
            Integer bSize = results.get(bK).getValues().size();

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

    /**
     * Reorder a TreeMap based on the overall staining level of the Expressions
     * @author radek
     *
     */
    public class ByOverallLevelComparator implements Comparator<String>
    {

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
