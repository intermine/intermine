package org.flymine.util;

import java.util.Iterator;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.FieldDescriptor;
import org.flymine.metadata.MetaDataException;

/**
 * Provides utility methods for working with data models
 *
 * @author Mark Woodbridge
 */
public class ModelUtil
{
    private ModelUtil() {
    }

    /**
     * Checks that an object has its primary keys set
     *
     * @param obj the Object to check
     * @param model the metadata against which to check
     * @return true if primary keys set, false otherwise
     * @throws IllegalAccessException if one of the fields is inaccessible
     * @throws MetaDataException if there is a problem retrieving the metadata
     */
    public static boolean hasValidKey(Object obj, Model model)
        throws IllegalAccessException, MetaDataException {
        if (obj == null) {
            throw new NullPointerException("obj must not be null");
        }
        ClassDescriptor cld = model.getClassDescriptorByName(obj.getClass().getName());
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
