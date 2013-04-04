package org.intermine.api.query;

public class QueryStoreException extends Exception {

    public QueryStoreException(String message, Exception e) {
        super(message, e);
    }

    public QueryStoreException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 477829937698800861L;

}
