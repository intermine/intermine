package org.intermine.webservice.server.exceptions;

public class MissingParameterException extends BadRequestException {

    private static final String MESSAGE_FMT = "Missing parameter: '%s'";

    public MissingParameterException(String parameterName) {
        super(String.format(MESSAGE_FMT, parameterName));
    }

    public MissingParameterException(String parameterName, Throwable cause) {
        super(String.format(MESSAGE_FMT, parameterName), cause);
    }

}
