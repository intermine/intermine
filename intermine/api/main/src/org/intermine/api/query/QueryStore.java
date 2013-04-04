package org.intermine.api.query;

public interface QueryStore {

    public String putQuery(String xml) throws BadQueryException;

    public String getQuery(String key) throws KeyFormatException, NotPresentException;
}
