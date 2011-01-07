package org.intermine.webservice.server.output;

public class JSONFormattingException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	 /**
     * @param message message
     */
    public JSONFormattingException(String message) {
        super(message);
    }

    /**
     * @param message message
     * @param cause cause
     */
    public JSONFormattingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param cause cause
     */
    public JSONFormattingException(Throwable cause) {
        super(cause);
    }
	
}
