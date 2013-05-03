package org.intermine.webservice.server.exceptions;

import org.intermine.webservice.server.output.Output;

public class UnauthorizedException extends ServiceException {

    private static final long serialVersionUID = 1L;

    public UnauthorizedException() {
        super("This service requires authentication.");
        initResponseCode();
    }

    public UnauthorizedException(String message) {
        super(message);
        initResponseCode();
    }

    private void initResponseCode() {
        setHttpErrorCode(Output.SC_UNAUTHORIZED);
    }
}
