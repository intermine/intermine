package org.intermine.web.bag;

import org.intermine.objectstore.query.Query;

public class BagQuery {
	private Query query;
	private String message;
	private boolean matchesAreIssues;

	public BagQuery(Query query, String message, boolean matchesAreIssues) {
		this.query = query;
		this.message = message;
		this.matchesAreIssues = matchesAreIssues;
	}
	
	public Query getQuery() {
		return this.query;
	}
	
	public boolean matchesAreIssues() {
		return this.matchesAreIssues;
	}
	
	public String getMessage() {
		return this.message;
	}	
}

