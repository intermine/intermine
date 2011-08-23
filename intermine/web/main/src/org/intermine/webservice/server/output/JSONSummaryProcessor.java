package org.intermine.webservice.server.output;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.api.results.ResultElement;
import org.json.JSONObject;

public class JSONSummaryProcessor extends JSONResultProcessor {

	@Override
	protected Iterator<? extends Object> getResultsIterator(Iterator<List<ResultElement>> it) {
		return new SummaryIterator(it);
	}
	
	private static class SummaryIterator implements Iterator<JSONObject>{
		
		private final Iterator<List<ResultElement>> it;
		
		public SummaryIterator(Iterator<List<ResultElement>> it) {
			this.it = it;
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public JSONObject next() {
			List<ResultElement> row = it.next();
			Map<String, Object> dict = new HashMap<String, Object>();
			// 4 = numeric, 2 = string...
			if (row.size() == 4) {
				dict.put("min", row.get(0).getField());
				dict.put("max", row.get(1).getField());
				dict.put("average", row.get(2).getField());
				dict.put("stdev", row.get(3).getField());
			} else if (row.size() == 2) {
				dict.put("item", row.get(0).getField());
				dict.put("count", row.get(1).getField());
			}
			return new JSONObject(dict);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
