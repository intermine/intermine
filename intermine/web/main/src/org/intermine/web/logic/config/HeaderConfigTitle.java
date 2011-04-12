package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.LinkedHashMap;
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
     * @see using LinkedHashMap to add elements in order while easily being able to retrieve them
     **/
    private HashMap<String, LinkedHashMap<String, Object>> objectTitles = null;

    /** @var decides whether to append or only inherit config from parent, used by WebConfig only */
    private Boolean appendConfig = false;

    private HeaderConfigLink link = new HeaderConfigLink();

    private Integer numberOfMainTitlesToShow = null;

    /**
     * Set main title(s) path(s) for the object, e.g.: symbol, primaryIdentifier => eve FBgn0000606
     * @param mainTitles a '|' delineated string of paths
     */
    public void setMainTitles(String mainTitles) {
        setTitles(mainTitles, "main");
    }

    /**
     * Set subtitle(s) path(s) for the object, e.g.: organism.shortName => D. melanogaster
     * @param subTitles a '|' delineated string of paths
     */
    public void setSubTitles(String subTitles) {
        setTitles(subTitles, "sub");
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
    private void setTitles(String titles, String key) {
        // init biz
        if (objectTitles == null) {
            objectTitles = new HashMap<String, LinkedHashMap<String, Object>>();
        }

        LinkedHashMap<String, Object> l = null;
        // do we have the titles already?
        if (objectTitles.containsKey(key)) {
            // get
            l = objectTitles.get(key);
        } else {
            // new
            l = new LinkedHashMap<String, Object>();
        }
        // split on a pipe character "|"
        StringTokenizer st = new StringTokenizer(titles, "|");
        // traverse
        while (st.hasMoreTokens()) {
            // get the string
            String token = st.nextToken();
            // due to the inheritance in WebConfig we need to check we have NOT
            //  saved the path before
            if (!l.containsKey(token)) {
                // add to the map
                l.put(token, null);
            }
        }
        // save
        objectTitles.put(key, l);
    }

    /**
     *
     * @return map of titles
     */
    public HashMap<String, LinkedHashMap<String, Object>> getTitles() {
        return objectTitles;
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
