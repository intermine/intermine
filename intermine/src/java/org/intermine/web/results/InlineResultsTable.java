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
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import org.intermine.util.TypeUtil;
import org.intermine.metadata.FieldDescriptor;
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

    /**
     * Constructor
     * @param results the underlying SingletonResults object
     * @throws ObjectStoreException if an error occurs
     */
    public InlineResultsTable(LazyCollection results) throws ObjectStoreException {
        this.results = results;
        try {
            results.get(size);
        } catch (IndexOutOfBoundsException e) {
            size = results.size();
        }
    }
    
    /**
     * Get the column headings
     * @return the column headings
     */
    public List getColumnNames() {
        Set pkFields = new HashSet();
        for (Iterator i = results.subList(0, size).iterator(); i.hasNext();) {
            pkFields.addAll(PrimaryKeyUtil.getPrimaryKeyFields(results.getObjectStore().getModel(),
                                                                  i.next().getClass()));
        }
        List columnNames = new ArrayList();
        for (Iterator i = pkFields.iterator(); i.hasNext();) {
            FieldDescriptor fd = (FieldDescriptor) i.next();
            if (fd.isAttribute()) {
                columnNames.add(fd.getName());
            }
        }
        return columnNames;
    }

    /**
     * Get the rows of the table
     * @return the rows of the table
     */
    public List getRows() {
        List rows = new ArrayList();
        for (Iterator i = results.subList(0, size).iterator(); i.hasNext();) {
            Object o = i.next();
            List row = new ArrayList();
            for (Iterator j = getColumnNames().iterator(); j.hasNext();) {
                String fieldName = (String) j.next();
                try {
                    row.add(TypeUtil.getFieldValue(o, fieldName));
                } catch (IllegalAccessException e) {
                    row.add(null);
                }
            }
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
