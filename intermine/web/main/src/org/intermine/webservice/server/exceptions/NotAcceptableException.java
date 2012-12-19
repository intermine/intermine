package org.intermine.webservice.server.exceptions;


public class NotAcceptableException extends ServiceException {

    private static final long serialVersionUID = 6348869247603849879L;

    public NotAcceptableException() {
        super("Cannot serve any format that is acceptable to you");
    }

    @Override
    public int getHttpErrorCode() {
        return 406;
    }
}
