package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
//import java.util.Map;
//import java.util.HashMap;

import org.intermine.util.TypeUtil;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.ClassDescriptor;
//import org.intermine.metadata.ReferenceDescriptor;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.proxy.LazyCollection;

/**
 * An inline table created from a Collection
 * This table has one object per row
 * @author Mark Woodbridge
 */
public class InlineResultsTable
{
    protected LazyCollection results;
    protected int size = 10;
    protected List keyAttributes;
    //protected Map keyReferences = new HashMap();

    /**
     * Constructor
     * @param results the underlying SingletonResults object
     * @param cld the metadata for this collection field
     * @throws ObjectStoreException if an error occurs
     */
    public InlineResultsTable(LazyCollection results, ClassDescriptor cld)
        throws ObjectStoreException {
        this.results = results;
        try {
            results.get(size);
        } catch (IndexOutOfBoundsException e) {
            size = results.size();
        }
     
        keyAttributes = keyAttributes(cld);
        // for (Iterator i = keyReferences(cld).iterator(); i.hasNext();) {
//             ReferenceDescriptor ref = (ReferenceDescriptor) i.next();
//             keyReferences.put(ref, keyAttributes(ref.getReferencedClassDescriptor()));
//         }
    }
    
    /**
     * Return the key attributes
     * @return the key attributes
     */
    public List getKeyAttributes() {
        return keyAttributes;
    }

//     public Map getKeyReferences() {
//         return keyReferences;
//     }

    /**
     * Return the list of fields that are both attributes and primary keys
     * @param cld the metadata for the class
     * @return the list of fields
     */
    public static List keyAttributes(ClassDescriptor cld) {
        List keyAttributes = new ArrayList();
        for (Iterator i = PrimaryKeyUtil.getPrimaryKeyFields(cld.getModel(),
                                                             cld.getType()).iterator();
             i.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) i.next();
            if (fd.isAttribute() && !fd.getName().equals("id")) {
                keyAttributes.add(fd);
            }
        }
        return keyAttributes;
    }

    //     protected static List keyReferences(ClassDescriptor cld) {
//         List keyReferences = new ArrayList();
//         for (Iterator i = PrimaryKeyUtil.getPrimaryKeyFields(cld.getModel(),
//    cld.getType()).iterator();
//              i.hasNext();) {
//             FieldDescriptor fd = (FieldDescriptor) i.next();
//             if (fd.isReference()) {
//                 keyReferences.add(fd);
//             }
//         }
//         return keyReferences;
//     }

    /**
     * Get a set of field values from an object, given the object and a list of fields
     * @param o the Object
     * @param fieldDescriptors the list of fields
     * @return the list of field values
     * @throws Exception if an error occurs
     */
    protected static List getFieldValues(Object o, List fieldDescriptors) throws Exception {
        List values = new ArrayList();
        for (Iterator i = fieldDescriptors.iterator(); i.hasNext();) {
            values.add(TypeUtil.getFieldValue(o, ((FieldDescriptor) i.next()).getName()));
        }
        return values;
    }

    /**
     * Get the rows of the table
     * @return the rows of the table
     * @throws Exception if an error occurs accessing the ObjectStore
     */
    public List getRows() throws Exception {
        List rows = new ArrayList();
        for (Iterator i = results.subList(0, size).iterator(); i.hasNext();) {
            Object o = i.next();
            List row = new ArrayList();
            row.addAll(getFieldValues(o, keyAttributes));
            // for (Iterator j = keyReferences.entrySet().iterator(); j.hasNext();) {
//                 Map.Entry entry = (Map.Entry) j.next();
//                 Object ref = TypeUtil.getFieldValue(o, (String) entry.getKey());
//                 if (ref != null) {
//                     row.addAll(getFieldValues(ref, (List) entry.getValue()));
//                 }
//             }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Get the class descriptors of each object displayed in the table
     * @return the set of class descriptors for each row
     */
    public List getTypes() {
        List types = new ArrayList();
        for (Iterator i = results.subList(0, size).iterator(); i.hasNext();) {
            types.add(ObjectViewController.getLeafClds(i.next().getClass(), results.getObjectStore()
                                                       .getModel()));
        }
        return types;
    }

    /**
     * Get the ids of the objects in the rows
     * @return a List of ids, one per row
     */
    public List getIds() {
        List ids = new ArrayList();
        for (Iterator i = results.subList(0, size).iterator(); i.hasNext();) {
            ids.add(((InterMineObject) i.next()).getId());
        }
        return ids;
    }
}
