package org.intermine.web.logic.config;

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
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Configuration of an object (Type) header when displayed on report
 * @see bear in mind that we can apply special formatting to the paths, by adding [] or **
 * characters towards the beginning/end of the path and ReportObject needs to deal with that!
 * @author radek
 *
 */
public class HeaderConfigTitle
{

    /**
     * @var title(s) for the object when displaying it, e.g.: eve FBgn0000606 D.melanogaster
     **/
    private HashMap<String, List<TitlePart>> titleParts = new HashMap<String, List<TitlePart>>();

    /** @var decides whether to append or only inherit config from parent, used by WebConfig only */
    private Boolean appendConfig = false;

    private Integer numberOfMainTitlesToShow = Integer.MAX_VALUE;

    /**
     * The first part of a page title.
     */
    public static final String MAIN = "main";
    /**
     * A subtitle.
     */
    public static final String SUB = "sub";
    /**
     * The parts that make up a header (main title, sub title)
     */
    public static final String[] TYPES = new String[] {MAIN, SUB};

    /**
     * Set main title(s) path(s) for the object, e.g.: symbol, primaryIdentifier => eve FBgn0000606
     * @param mainTitles a '|' delineated string of paths
     */
    public void setMainTitles(String mainTitles) {
        setTitles(mainTitles, MAIN);
    }

    /**
     * Set subtitle(s) path(s) for the object, e.g.: organism.shortName => D. melanogaster
     * @param subTitles a '|' delineated string of paths
     */
    public void setSubTitles(String subTitles) {
        setTitles(subTitles, SUB);
    }

    /**
     * How many main titles to show in the main title
     * @param maxNumber integer
     */
    public void setNumberOfMainTitlesToShow(Integer maxNumber) {
        this.numberOfMainTitlesToShow = maxNumber;
    }

    /**
     *
     * @return number of main titles to show at the maximum
     * @see one might not want to show primaryId if we have a symbol etc.
     */
    public Integer getNumberOfMainTitlesToShow() {
        return this.numberOfMainTitlesToShow;
    }

    /**
     * Used by setSub() and setMain() to add a list of strings
     * @param titles
     * @param key
     */
    private void setTitles(String titles, String titlePart) {
        List<TitlePart> titleFields = titleParts.get(titlePart);
        if (titleFields == null) {
            titleFields = new ArrayList<TitlePart>();
            titleParts.put(titlePart, titleFields);
        }

        // split on a pipe character "|"
        StringTokenizer st = new StringTokenizer(titles, "|");
        while (st.hasMoreTokens()) {
            String field = st.nextToken();
            // due to the inheritance in WebConfig we need to check we haven't saved the path before
            TitlePart tp = new TitlePart(field);
            if (!titleFields.contains(tp)) {
                titleFields.add(tp);
            }
        }
    }

    /**
     * Add title parts to this header config.
     * @param partsToAdd parts to add to config
     */
    public void addTitleParts(HashMap<String, List<TitlePart>> partsToAdd) {
        titleParts.putAll(partsToAdd);
    }

    /**
     * Holder for a path that appears in the title and any prefix and suffix values if they have
     * been configured.
     */
    public class TitlePart
    {
        private final String path;
        private String prefix = "";
        private String suffix = "";

        /**
         * Construct with path string from config that may include formatting information.
         * @param styledPath a path from config
         */
        TitlePart(String styledPath) {
            char first = styledPath.charAt(0);
            char last = styledPath.charAt(styledPath.length() - 1);
            // apply special formatting
            if (first == '[' && last == ']') {
                prefix = "" + first;
                suffix = "" + last;
            } else if (first == '*' && first == last) {
                prefix = "<i>";
                suffix = "</i>";
            }
            // strip all "non allowed" characters
            this.path = styledPath.replaceAll("[^a-zA-Z.]", "");
        }

        /**
         * The path that should be in the title, with style removed.
         * @return a path as a string
         */
        public String getPath() {
            return path;
        }

        /**
         * A prefix that may be an HTML tag to appear before this part in the title.
         * @return the prefix or an empty string
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * A suffix that may be an HTML tag to appear after this part in the title.
         * @return the suffix or an empty string
         */
        public String getSuffix() {
            return suffix;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TitlePart) {
                return ((TitlePart) obj).path.equals(path);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }
    }

    /**
     *
     * @return map of titles
     */
    public HashMap<String, List<TitlePart>> getTitles() {
        return titleParts;
    }

    /**
     * Set whether to append config from parent
     * @param value Boolean
     */
    public void setAppendConfig(Boolean value) {
        appendConfig = value;
    }

    /**
     *
     * @return Boolean value as to whether or not to append configuration from parent
     */
    public Boolean getAppendConfig() {
        return appendConfig;
    }
}
