package org.intermine.web.bag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.iql.IqlQuery;

public class BagQuery {
	private Query query;
	private String message, queryString, pkg;
	private boolean matchesAreIssues;
	
	public BagQuery(String queryString, String message, String pkg, boolean matchesAreIssues) {
		this.queryString = queryString;
		this.message = message;
		this.matchesAreIssues = matchesAreIssues;
		this.pkg = pkg;
		this.query = null;
	}

	public BagQuery(Query query, String message, boolean matchesAreIssues) {
		this.queryString = "";
		this.query = query;
		this.message = message;
		this.matchesAreIssues = matchesAreIssues;
	}
	
	public String getQueryString() {
		return this.queryString;
	}
	
	public Query getQuery(Collection bag) {
		if (query == null) {
			IqlQuery q = new IqlQuery(queryString, pkg, new ArrayList(Collections.singleton(bag)));
			query = q.toQuery();
		}
		return query;
	}
	
	public boolean matchesAreIssues() {
		return this.matchesAreIssues;
	}	
	
	public String getMessage() {
		return this.message;
	}	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("query=" + queryString);
		sb.append(" message=" + message);
		sb.append(" matchesAreIssues= " + matchesAreIssues);
		return sb.toString();
	}
}

