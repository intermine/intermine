package org.flymine;

/**
 * An Exception that may be thrown by client facing parts
 * of FlyMine code.
 *
 * @author Richard Smith
 */
public class FlyMineException extends Exception
{
    /**
     * Constructs an FlyMineException
     */
    public FlyMineException() {
        super();
    }

    /**
     * Constructs an FlyMineException with the specified detail message.
     *
     * @param msg the detail message
     */
    public FlyMineException(String msg) {
        super(msg);
    }

    /**
     * Constructs an FlyMineException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public FlyMineException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an FlyMineException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public FlyMineException(String msg, Throwable t) {
        super(msg, t);
    }
}
