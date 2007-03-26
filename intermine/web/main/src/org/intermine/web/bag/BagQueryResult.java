package org.intermine.web.bag;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Class to hold the results of querying for a bag of objects.  Makes
 * available the matched objects, results that require some user input
 * (issues) and unresolved input.
 *
 * @author Richard Smith
 */
public class BagQueryResult
{ 
    /**
     * Key of the Map returned by getIssues() when the query for the input string found more than
     * one object.
     */
    public static final String DUPLICATE = "DUPLICATE";
    
    /**
     * Key of the Map returned by getIssues() when the input string 
     */ 
    public static final String OTHER = "OTHER";
     
    /**
     * Key of the Map returned by getIssues() when the object found when querying using input string 
     * needed to be translated.
     */ 
    public static final String TYPE_CONVERTED = "TYPE_CONVERTED";

    private Map matches = new LinkedHashMap();
    private Map issues = new LinkedHashMap();
    private Map unresolved = new HashMap();

    /**
     * Get any results that require some user input before adding to the bag.
     * [issue type -> [query -> [input string -> List of InterMineObjects]]] or for issue type of
     * "TYPE_TRANSLATED": [issue type -> [query -> [input string -> List of ConvertedObjectPair]]
     * @return a map from issues type to queries to input to possible objects
     */
    public Map getIssues() {
        return issues;
    }

    public void addIssue(String type, String query, String input, List objects) {
        Map issuesOfType = (Map) issues.get(type);
        if (issuesOfType == null) {
            issuesOfType = new LinkedHashMap();
            issues.put(type, issuesOfType);
        }
        Map queryIssues = (Map) issuesOfType.get(query);
        if (queryIssues == null) {
            queryIssues = new LinkedHashMap();
            issuesOfType.put(query, queryIssues);
        }
        List queryObjects = (List) queryIssues.get(input);
        if (queryObjects == null) {
            queryObjects = new ArrayList();
            queryIssues.put(input, queryObjects);
        }
        queryObjects.addAll(objects);
    }

    /**
     * Get any exact matches found by the queries [id -> [input strings].
     * If the same input string appears twice in the initial list it will
     * appear twice in the list of inputs matching the InterMineObject id.
     * @return a map from InterMineObject id to list of input strings
     */
    public Map getMatches() {
        return matches;
    }

    /**
     * Add a new match from an input string to an InterMineObject id.
     * @param input the original input string entered
     * @param id the id of an InterMineObject
     */
    public void addMatch(String input, Integer id) {
        List inputs = (List) matches.get(id);
        if (inputs == null) {
            inputs = new ArrayList();
            matches.put(id, inputs);
        }
        inputs.add(input);
    }

    /**
     * Get a Map of any input Strings for which objects of the right type could not be found.  
     * @return a Map of from input string to null/object - null when the input doesn't match any 
     * object of any type, otherwise a reference to a Set of the objects that matched
     */
    public Map getUnresolved() {
        return unresolved;
    }

    /**
     * Set the Map of unresolved input strings.  It is Map from input string to null/object - null
     * when the input doesn't match any object of any type, otherwise a reference to the object
     * that matched.
     * @param unresolved the new unresolved Map
     */
    public void setUnresolved(Map unresolved) {
        this.unresolved = unresolved;
    }
}
