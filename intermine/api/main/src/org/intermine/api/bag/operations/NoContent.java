package org.intermine.api.bag.operations;

public class NoContent extends BagOperationException {

    private static final long serialVersionUID = -1679235731173923331L;

    public NoContent() {
        super("This operation failed to produce any content");
    }
}
