package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.PrimaryKeyUtil;

/**
 * Helper methods for the FieldConfig class.
 *
 * @author Kim Rutherford
 */

public class FieldConfigHelper
{
    /**
     * Find the FieldConfig objects for the the given ClassDescriptor.
     * @param webConfig the WebConfig object for this webapp
     * @param cd a ClassDescriptor
     * @return the FieldConfig objects for the the given ClassDescriptor
     */
    public static List getClassFieldConfigs(WebConfig webConfig, ClassDescriptor cd) {
        Type type = (Type) webConfig.getTypes().get(cd.getName());

        if (type != null) {
            List fieldConfigs = new ArrayList(type.getFieldConfigs());

            if (fieldConfigs.size() > 0) {
                return fieldConfigs;
            }
        }

        // there are no configured fields for this Class so use the fields from the primary keys
        List returnRow = new ArrayList();

        Iterator keyAttributesIter = keyAttributes(cd).iterator();
        while (keyAttributesIter.hasNext()) {
            FieldConfig fc = new FieldConfig();
            fc.setFieldExpr(((FieldDescriptor) keyAttributesIter.next()).getName());
            returnRow.add(fc);
        }

        return returnRow;
    }

    /**
     * Return the list of fields that are both attributes and primary keys
     * @param cld the metadata for the class
     * @return the list of fields
     */
    private static List keyAttributes(ClassDescriptor cld) {
        List keyAttributes = new ArrayList();
        Iterator i =
            PrimaryKeyUtil.getPrimaryKeyFields(cld.getModel(), cld.getType()).iterator();
        while (i.hasNext()) {
            FieldDescriptor fd = (FieldDescriptor) i.next();
            if (fd.isAttribute() && !fd.getName().equals("id")) {
                keyAttributes.add(fd);
            }

        }
        return keyAttributes;
    }
}
