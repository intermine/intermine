package org.flymine.sql.precompute;

/**
 * An Exception that may be thrown by an BestQuery.
 *
 * @author Andrew Varley
 */
public class BestQueryException extends Exception
{
    /**
     * Constructs an BestQueryException
     */
    public BestQueryException() {
        super();
    }

    /**
     * Constructs an BestQueryException with the specified detail message.
     *
     * @param msg the detail message
     */
    public BestQueryException(String msg) {
        super(msg);
    }

    /**
     * Constructs an BestQueryException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public BestQueryException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an BestQueryException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public BestQueryException(String msg, Throwable t) {
        super(msg, t);
    }
}
