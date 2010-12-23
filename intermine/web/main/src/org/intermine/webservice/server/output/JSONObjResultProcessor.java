/**
 * 
 */
package org.intermine.webservice.server.output;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.intermine.webservice.server.core.ResultProcessor;
import org.intermine.api.results.ExportResultsIterator;
import org.intermine.api.results.ResultElement;
import org.intermine.webservice.server.output.Output;
import org.json.JSONObject;

/**
 * @author Alexis Kalderimis
 *
 */
public class JSONObjResultProcessor extends ResultProcessor {

	
	/**
	 * Constructor.
	 */
	public JSONObjResultProcessor() {
		// Empty constructor
	}
	
	@Override
	public void write(Iterator<List<ResultElement>> resultIt, Output output) {
		if (! (resultIt instanceof ExportResultsIterator)) {
			throw new IllegalArgumentException("The iterator must be an ExportResultsIterator");
		}
		ExportResultsIterator exportIter = (ExportResultsIterator) resultIt;
		JSONResultsIterator jsonIter = new JSONResultsIterator(exportIter);
		while (jsonIter.hasNext()) {
			JSONObject next = jsonIter.next();
			List<String> outputLine = Arrays.asList(next.toString());
			output.addResultItem(outputLine);
		}
	}
}