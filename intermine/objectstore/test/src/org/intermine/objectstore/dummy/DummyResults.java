package org.intermine.objectstore.dummy;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;

import org.intermine.objectstore.query.Query;
import org.intermine.objectstore.query.Results;
import org.intermine.objectstore.query.ResultsBatches;

/**
 * @author Alexis Kalderimis
 *
 */
public class DummyResults extends Results {
	
	private List<Object> delegateList;
	private Query query;
	public  int batchSize;        // public so tests can mess with it
	public boolean isSingleBatch; // public so tests can mess with it
	
	public DummyResults(Query q, List<Object> results) {
		this.delegateList = results;
		this.query = q;
	}
	
	@Override
	public Query getQuery() {
		return this.query;
	}
	
	@Override
	public List<Object> range(int start, int end) {
		return delegateList.subList(start, end + 1);
	}
	
	@Override
	public int size() {
		return delegateList.size();
	}

	@Override
	public boolean isEmpty() {
		 return delegateList.isEmpty();	
	}
	
	@Override
	public synchronized void setBatchSize(int size) {
		if (immutable) {
			throw new IllegalArgumentException("Cannot change settings of Results object in cache");
        }
        this.batchSize = size;
    }
	
	@Override
	public int getBatchSize() {
        return this.batchSize;
    }
	
	@Override
	public List<Object> asList() {
		return delegateList;
	}
	
	@Override
	public boolean isSingleBatch() {
        return isSingleBatch;
    }
	
	@Override
	public Iterator<Object> iterator() {
		return delegateList.iterator();
	}

	@Override
	public ResultsBatches getResultsBatches() {
		throw new MethodNotMockedException();
	}
}
