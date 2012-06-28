package org.intermine.api.query;

import org.intermine.objectstore.ObjectStoreException;

public class BagNotFound extends ObjectStoreException
{
    private static final long serialVersionUID = -5015680970494799392L;

    private final String name;
    
    public BagNotFound(String name) {
        super(String.format( "A bag (%s) used by this query does not exist", name));
        this.name = name;
    }

    public BagNotFound(String name, Throwable t) {
        super(String.format( "A bag (%s) used by this query does not exist", name), t);
        this.name = name;
    }

    /**
     * @return The name of the missing bag.
     */
    public String getName() {
        return name;
    }

}
