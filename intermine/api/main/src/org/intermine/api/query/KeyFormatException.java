package org.intermine.api.query;

public class KeyFormatException extends QueryStoreException {

    public KeyFormatException(String message, NumberFormatException e) {
        super(message, e);
    }

}
