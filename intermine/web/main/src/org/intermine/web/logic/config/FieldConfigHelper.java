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
}
