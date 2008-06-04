package org.intermine.web.logic.results;

/*
 * Copyright (C) 2002-2008 FlyMine
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
 * This class saves things related to GUI. It  is meant as wrapper for future
 * staff that must be hold in session and is related to web application GUI.
 * @author Jakub Kulaviak
 */
public class GuiObject 
{

    private Map<String, Boolean> toggledElements = new HashMap<String, Boolean>();

    private Map<String, Object> attributesMap = new HashMap<String, Object>();
    
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
    
    public void setAttribute(String name, Object  value) {
        attributesMap.put(name, value);
    }
    
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }
    
    public Map<String, Object> getAttributes() {
        return attributesMap;
    }
}


