package org.intermine.webservice.server.exceptions;

public class NotImplementedException extends InternalErrorException
{
    private static final long serialVersionUID = -1593418347158889396L;

    public NotImplementedException(String message) {
        super(message);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotImplementedException(Throwable cause) {
        super(cause);
    }
    
    public NotImplementedException(Class<?> location, String methodName) {
        super(String.format("%s is not defined for %s", methodName, location.getName()));
    }

}
