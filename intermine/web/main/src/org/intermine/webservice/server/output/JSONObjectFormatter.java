/**
 * 
 */
package org.intermine.webservice.server.output;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Alex Kalderimis
 *
 */
public class JSONObjectFormatter extends Formatter {
	
	/**
	 * Constructor
	 */
	public JSONObjectFormatter() {
		// empty constructor
	}

	/**
	 * Closes the remaining open brackets (the root class array, and the 
	 * overall result set object).
	 * @see org.intermine.webservice.server.output.Formatter#formatFooter()
	 */
	@Override
	public String formatFooter() {
		return "]}";
	}

	/* (non-Javadoc)
	 * @see org.intermine.webservice.server.output.Formatter#formatHeader(java.util.Map)
	 */
	@Override
	public String formatHeader(Map<String, String> attributes) {
		String rootClass = attributes.get("rootClass");
		String views = attributes.get("views");
		String model = attributes.get("modelName");
		String time  = attributes.get("executionTime");
		return "{" +
					"'views':" + views + "," +
					"'model':'" + model + "'," +
					"'executed_at':'" + time + "'," + 
					"'" + rootClass + "':[";
	}

	/**
	 * In normal cases a list with a single json string item is expected. 
	 * But just in case, this formatter will simply join any strings
	 * it gets given, delimiting with a comma. It is the responsibility of 
	 * whoever is feeding me these lines to add any necessary commas between
	 * them.
	 * @see org.intermine.webservice.server.output.Formatter#formatResult(java.util.List)
	 */
	@Override
	public String formatResult(List<String> resultRow) {
		if (resultRow.isEmpty()) return "";
		Iterator<String> iter = resultRow.iterator();
		StringBuffer buffer = new StringBuffer(iter.next());
		while (iter.hasNext()) {
			buffer.append(",").append(iter.next());
		}
		return buffer.toString();	
	}

}
