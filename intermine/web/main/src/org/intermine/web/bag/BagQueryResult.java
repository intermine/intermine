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
