package org.intermine.web.context;

public class ContextNotInitialisedException extends RuntimeException {

    public ContextNotInitialisedException() {
    }

    public ContextNotInitialisedException(String arg0) {
        super(arg0);
    }

    public ContextNotInitialisedException(Throwable arg0) {
        super(arg0);
    }

    public ContextNotInitialisedException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
