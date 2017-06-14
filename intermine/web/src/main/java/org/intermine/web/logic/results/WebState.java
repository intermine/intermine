package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;

/**
 * This class saves things related to the state of web GUI. It  is meant as box for future
 * staff that must be hold in session and is related to web application GUI.
 * @author Jakub Kulaviak
 */
public class WebState
{

    private Map<String, Boolean> toggledElements = new HashMap<String, Boolean>();
    protected Map<String, String> subtabs = new HashMap<String, String>();
    private Map<String, Object> statesMap = new HashMap<String, Object>();
    private Map<String, Integer> hintCounts = new HashMap<String, Integer>();

    /**
     * Gets map of ids of elements that were in the past (during session) toggled
     * - if they are opened or closed.
     * @return map of element ids
     */
    public Map<String, Boolean> getToggledElements() {
        return toggledElements;
    }

    /**
     * Sets map of ids and its state. @see getToggleElements()
     * @param toggledElements a map of Strings to Booleans
     */
    public void setToggledElements(Map<String, Boolean> toggledElements) {
        this.toggledElements = toggledElements;
    }

    /**
     * Set attribute.
     * @param name name of attribute
     * @param value value of attribute
     */
    public void setState(String name, Object  value) {
        statesMap.put(name, value);
    }

    /**
     * @param name name of state
     * @return value of state or null if state wasn't set
     */
    public Object getState(String name) {
        return statesMap.get(name);
    }

    /**
     * @return map of states
     */
    public Map<String, Object> getStates() {
        return statesMap;
    }

    /**
     * Set the subtab for a particular tab
     * @param tab the tab/pageName name
     * @param subtab the subtab value
     */
    public void addSubtab(String tab, String subtab) {
        subtabs.put(tab, subtab);
    }

    /**
     *  @param tab the tab/pageName name
     * @return the subtab for the specified page, if any
     */
    public String getSubtab(String tab) {
        return subtabs.get(tab);
    }

    /**
     * map of tab --&gt; subtab
     * @return the subtabs
     */
    public Map<String, String> getSubtabs() {
        return subtabs;
    }

    /**
     * Increment the number of times a hint has been displayed.
     * @param hint the hint that has been displayed
     */
    public void incrementHintCount(String hint) {
        int count = 0;
        if (hintCounts.containsKey(hint)) {
            count = hintCounts.get(hint).intValue();
        }
        hintCounts.put(hint, new Integer(count + 1));
    }

    /**
     * Get the number of times a particular hint has been shown.
     * @param hint the hint to fetch count for
     * @return number of times the hint has been displayed
     */
    public int getHintCount(String hint) {
        if (hintCounts.containsKey(hint)) {
            return hintCounts.get(hint).intValue();
        }
        return 0;
    }
}


