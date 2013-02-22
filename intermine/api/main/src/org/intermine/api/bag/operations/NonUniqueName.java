package org.intermine.api.bag.operations;

public class NonUniqueName extends BagOperationException {

    public NonUniqueName(String name) {
        super("A bag called " + name + " already exists");
    }
}
