package org.flymine.metadata;

/**
 * An Exception that may be thrown if metadata is in an invalid state.
 * Some aspects of metadata cannot be validated on model construction.
 *
 * @author Richard Smith
 */
public class MetaDataException extends Exception
{
    /**
     * Constructs an MetaDataException
     */
    public MetaDataException() {
        super();
    }

    /**
     * Constructs an MetaDataException with the specified detail message.
     *
     * @param msg the detail message
     */
    public MetaDataException(String msg) {
        super(msg);
    }

    /**
     * Constructs an MetaDataException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public MetaDataException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an MetaDataException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public MetaDataException(String msg, Throwable t) {
        super(msg, t);
    }
}
