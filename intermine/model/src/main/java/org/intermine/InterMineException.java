package org.intermine;

public class InterMineException extends Exceptionx
{
    /**
     * Constructs an InterMineException
     */
    protected InterMineException() {
        super();
    }

    /**
     * Constructs an InterMineException with the specified detail message.
     *
     * @param msg the detail message
     */
    public InterMineException(String msg) {
        super(msg);
    }

    /**
     * Constructs an InterMineException with the specified nested throwable.
     *
     * @param t the nested throwable
     */
    public InterMineException(Throwable t) {
        super(t);
    }

    /**
     * Constructs an InterMineException with the specified detail message and nested throwable.
     *
     * @param msg the detail message
     * @param t the nested throwable
     */
    public InterMineException(String msg, Throwable t) {
        super(msg, t);
    }
}
