package org.intermine.api.query;

public class BadQueryException extends QueryStoreException {

    public BadQueryException(String message, Exception e) {
        super(message, e);
    }

}
