package org.intermine.web.logic.results;

import java.util.HashMap;
import java.util.Map;

/**
 * This class saves things related to GUI. It  is meant as wrapper for future 
 * staff that must be hold in session and is related to web application GUI. 
 *
 */
public class GuiObject {

	private Map<String, Boolean> toggledElements = new HashMap<String, Boolean>();

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
	 * @return map of element ids
	 */
	public void setToggledElements(Map<String, Boolean> toggledElements) {
		this.toggledElements = toggledElements;
	}
}


