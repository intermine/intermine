package org.intermine.web.logic.results;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class that saves some attributes concerning type for displaying object. For example,
 * that user opened some aspect at page with object details and it will be opened during
 * the session for others object of the same type.
 * 
 */

public class DisplayType {

	// Maps type -> its opened aspects
	private Map<String, Set<String>> typeMap = new HashMap<String, Set<String>>();
	
	/** Default constructor **/
	public DisplayType() {}
	
	/** Toggle aspect for specified type. 
	 * @param type type of the object, for which is aspect opened
	 * @param aspectId id of aspect
	 * @param opened new aspect state  
	 * **/
	public void toggleAspect(String type, String aspectId, boolean opened) {
		Set<String> aspectIds = typeMap.get(type);
		if (aspectIds == null) {
			aspectIds = new HashSet<String>();
			typeMap.put(type, aspectIds);
		}
		if (opened) {
			aspectIds.add(aspectId);
		} else {
			aspectIds.remove(aspectId);
		}
	}

	/** Return opened aspects of specified type 
	 * @return type type of object
	 */
	public Set<String> getOpenedAspects(String type) {
		return typeMap.get(type) != null ? typeMap.get(type) : new HashSet<String>(); 
	}	
}
