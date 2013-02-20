package org.intermine.api.bag.operations;

public class BagOperationException extends Exception {

    private static final long serialVersionUID = -7187065324138215891L;

    public BagOperationException() {
    }

    public BagOperationException(String message) {
        super(message);
    }

    public BagOperationException(Throwable cause) {
        super(cause);
    }

    public BagOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
