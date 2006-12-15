package org.intermine.web.bag;

import java.util.List;
import java.util.Map;

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

	/**
	 * Get any results that require some user input before adding to the bag.
	 * [issue type -> [query -> [input string -> InterMineObjects]]] 
	 * @return a map from issues type to queries to input to possible objects
	 */
	public Map getIssues() {
		return null;
	}
	
	/**
	 * Get any exact matches found by the queries [input string -> id].
	 * @return a map from input String to matched object ids
	 */
	public Map getMatches() {
		return null;
	}
	
	/**
	 * Get a list of any input Strings for which objects could not be found.
	 * @return a list of input strings not resolved to objects
	 */
	public List getUnresolved() {
		return null;
	}
}
