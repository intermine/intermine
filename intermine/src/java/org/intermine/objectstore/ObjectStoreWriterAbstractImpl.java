package org.flymine.objectstore;

import org.flymine.metadata.Model;

/**
 * Abstract ObjectStoreWriter implementation to hold metadata
 *
 * @author Mark Woodbridge
 */
public abstract class ObjectStoreWriterAbstractImpl implements ObjectStoreWriter
{
    protected Model model;

    /**
     * No argument constructor for testing purposes
     */
    protected ObjectStoreWriterAbstractImpl() {
    }

    /**
     * Constructor
     * @param os the ObjectStore used to access metadata
     */
    public ObjectStoreWriterAbstractImpl(ObjectStore os) {
        model = os.getModel();
    }
}
