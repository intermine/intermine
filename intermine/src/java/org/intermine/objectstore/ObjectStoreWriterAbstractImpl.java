package org.flymine.objectstore;

import java.util.Iterator;

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.MetaDataException;
import org.flymine.util.TypeUtil;

/**
 * Abstract ObjectStoreWriter implementation to hold metadata
 *
 * @author Mark Woodbridge
 */
public abstract class ObjectStoreWriterAbstractImpl implements ObjectStoreWriter
{
    protected ObjectStore os;

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
        this.os = os;
    }

    /**
     * @see ObjectStoreWriter#getObjectStore
     */
    public ObjectStore getObjectStore() {
        return os;
    }

    /**
     * Checks that an object has its primary keys set
     *
     * @param obj the Object to check
     * @return true if primary keys set, false otherwise
     * @throws IllegalAccessException if one of the fields is inaccessible
     * @throws MetaDataException if there is a problem retrieving the metadata
     */
    protected boolean hasValidKey(Object obj)
        throws IllegalAccessException, MetaDataException {
        if (obj == null) {
            throw new NullPointerException("obj must not be null");
        }
        ClassDescriptor cld = os.getModel().getClassDescriptorByName(obj.getClass().getName());
        Iterator keyIter = cld.getPkFieldDescriptors().iterator();
        while (keyIter.hasNext()) {
            String fieldName = ((FieldDescriptor) keyIter.next()).getName();
            if (TypeUtil.getFieldValue(obj, fieldName) == null) {
                return false;
            }
        }
        return true;
    }
}

