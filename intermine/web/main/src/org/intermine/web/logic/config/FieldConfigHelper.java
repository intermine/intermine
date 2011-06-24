package org.intermine.web.logic.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;

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
    public static List<FieldConfig> getClassFieldConfigs(WebConfig webConfig, ClassDescriptor cd) {
        Type type = webConfig.getTypes().get(cd.getName());

        if (type != null) {
            List<FieldConfig> fieldConfigs = new ArrayList<FieldConfig>(type.getFieldConfigs());

            if (fieldConfigs.size() > 0) {
                return fieldConfigs;
            }
        }

        return Collections.EMPTY_LIST;
    }

    public static FieldConfig getFieldConfig(WebConfig webConfig, FieldDescriptor fd) {
        ClassDescriptor cld = fd.getClassDescriptor();
        return getFieldConfig(webConfig, cld, fd);
    }

    public static FieldConfig getFieldConfig(WebConfig webConfig, ClassDescriptor cld, FieldDescriptor fd) {
        List<FieldConfig> fcs = getClassFieldConfigs(webConfig, cld);
        for (FieldConfig fc: fcs) {
            if (fc == null) {
                continue;
            }
            String fieldExpr = fc.getFieldExpr();
            if (fieldExpr != null && fieldExpr.equals(fd.getName())) {
                return fc;
            }
        }
        return null;
    }
}
