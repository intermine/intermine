package org.intermine.api.bag.operations;

import org.intermine.metadata.MetaDataException;

public class IncompatibleTypes extends BagOperationException {

    private static final long serialVersionUID = 5382508035006834031L;

    public IncompatibleTypes(MetaDataException e) {
        super("Incompatible types", e);
    }
}
