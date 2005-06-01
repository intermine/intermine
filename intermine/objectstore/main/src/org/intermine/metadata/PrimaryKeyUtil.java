package org.intermine.metadata;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import org.intermine.dataloader.PrimaryKey;
import org.intermine.dataloader.DataLoaderHelper;

/**
 * Utility methods for PrimaryKey objects.
 *
 * @author Kim Rutherford
 */

public abstract class PrimaryKeyUtil
{
    /**
     * Retrieve a List of all fields that appear in any primary key for the given class.
     *
     * @param model the Model
     * @param c the Class to fetch primary key fields from
     * @return a List of all fields that appear in any primary key
     */
    public static Set getPrimaryKeyFields(Model model, Class c) {        
        Set pkFields = new HashSet();
        for (Iterator i = model.getClassDescriptorsForClass(c).iterator(); i.hasNext();) {
            ClassDescriptor cld = (ClassDescriptor) i.next();
            for (Iterator j = DataLoaderHelper.getPrimaryKeys(cld).values().iterator();
                 j.hasNext();) {
                PrimaryKey pk = (PrimaryKey) j.next();
                for (Iterator k = pk.getFieldNames().iterator(); k.hasNext();) {
                    String fieldName = (String) k.next();
                    pkFields.add(cld.getFieldDescriptorByName(fieldName));
                }
            }
        }
        return pkFields;
    }
}
