package org.intermine.api.bag.operations;

public class NotCurrent extends BagOperationException {

    private static final long serialVersionUID = 4859189430080794926L;

    public NotCurrent() {
        super("Not all bags are current");
    }
}
