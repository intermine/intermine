package org.flymine.objectstore;

/**
 * An exception that may be thrown by the objectstore, when an access is made outside the allowable
 * maximum query size.
 *
 * @author Matthew Wakeling
 * @author Andrew Varley
 */
public class ObjectStoreLimitReachedException extends IndexOutOfBoundsException
{
    /**
     * Constructs an ObjectStoreLimitReachedException.
     */
    public ObjectStoreLimitReachedException() {
        super();
    }

    /**
     * Constructs an ObjectStoreLimitReachedException with the specified detail message.
     *
     * @param msg the detail message
     */
    public ObjectStoreLimitReachedException(String msg) {
        super(msg);
    }
}
