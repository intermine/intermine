package org.intermine.web.bag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Copyright (C) 2002-2006 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * Class to hold the results of querying for a bag of objects.  Makes 
 * available the matched objects, results that require some user input
 * (issues) and unresolved input.
 * 
 * @author Richard Smith
 */
public class BagQueryResult {
	public static final String DUPLICATE = "DUPLICATE"; 
	public static final String OTHER = "OTHER"; 
	
	private Map matches = new HashMap();
	private Map issues = new HashMap();
	private List unresolved = new ArrayList();
	
	/**
	 * Get any results that require some user input before adding to the bag.
	 * [issue type -> [query -> [input string -> Set of InterMineObjects]]] 
	 * @return a map from issues type to queries to input to possible objects
	 */
	public Map getIssues() {
		return issues;
	}
	
	public void addIssue(String type, String query, String input, Set objects) {
		Map issuesOfType = (Map) issues.get(type);
		if (issuesOfType == null) {
			issuesOfType = new HashMap();
			issues.put(type, issuesOfType);
		}
		Map queryIssues = (Map) issuesOfType.get(query);
		if (queryIssues == null) {
			queryIssues = new HashMap();
			issuesOfType.put(query, queryIssues);
		}
		queryIssues.put(input, objects);
	}
	
	/**
	 * Get any exact matches found by the queries [input string -> id].
	 * @return a map from input String to matched object ids
	 */
	public Map getMatches() {
		return matches;
	}
	
	
	public void addMatch(String input, Integer id) {
		matches.put(input, id);
	}
    
	/**
	 * Get a list of any input Strings for which objects could not be found.
	 * @return a list of input strings not resolved to objects
	 */
	public List getUnresolved() {
		return unresolved;
	}
	
	public void setUnresolved(List unresolved) {
		this.unresolved = unresolved;
	}
}
