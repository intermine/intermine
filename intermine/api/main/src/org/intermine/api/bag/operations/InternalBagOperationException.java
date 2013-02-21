package org.intermine.api.bag.operations;

/**
 * Exceptions that are the fault of underlying infrastructure, and not the way the
 * classes themselves were used. ie. "our fault".
 * @author alex
 *
 */
public class InternalBagOperationException extends BagOperationException {

    private static final long serialVersionUID = -5954984605945169071L;

    public InternalBagOperationException() {
    }

    public InternalBagOperationException(String message) {
        super(message);
    }

    public InternalBagOperationException(Throwable cause) {
        super(cause);
    }

    public InternalBagOperationException(String message, Throwable cause) {
        super(message, cause);
    }

}
