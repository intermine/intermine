package org.flymine.objectstore;

/**
 * An Exception that may be thrown by an ObjectStore.
 *
 * @author Andrew Varley
 */
public class ObjectStoreException extends Exception
{
    /**
     * Constructs an ObjectStoreException
     */
    public ObjectStoreException() {
        super();
    }

    /**
     * Constructs an ObjectStoreException with the specified detail message.
     *
     * @param msg the detail message
     */
    public ObjectStoreException(String msg) {
        super(msg);
    }

    /**
     * Constructs an ObjectStoreException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public ObjectStoreException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an ObjectStoreException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public ObjectStoreException(String msg, Throwable t) {
        super(msg, t);
    }
}
