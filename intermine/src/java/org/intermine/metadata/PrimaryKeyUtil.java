package org.intermine.metadata;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    public static List getPrimaryKeyFields(Model model, Class c) {        
        ArrayList primaryKeyFields = new ArrayList();

        for (Iterator classIter = model.getClassDescriptorsForClass(c).iterator();
             classIter.hasNext();) {
            ClassDescriptor classDescriptor = (ClassDescriptor) classIter.next();

            Map primaryKeys = DataLoaderHelper.getPrimaryKeys(classDescriptor);

            for (Iterator keyIter = primaryKeys.keySet().iterator(); keyIter.hasNext();) {
                String keyKey = (String) keyIter.next();
                PrimaryKey primaryKey;
                primaryKey = (PrimaryKey) primaryKeys.get(keyKey);
                for (Iterator nameIter = primaryKey.getFieldNames().iterator();
                     nameIter.hasNext(); ) {
                    String fieldName = (String) nameIter.next();
                    if (!primaryKeyFields.contains(fieldName)) {
                        primaryKeyFields.add(fieldName);
                    }
                }
            }
        }

        return primaryKeyFields;
    }
}
