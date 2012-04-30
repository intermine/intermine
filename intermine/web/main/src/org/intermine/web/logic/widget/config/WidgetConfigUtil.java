package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.pathquery.PathConstraint;

public class WidgetConfigUtil {

    public static boolean isListConstraint(PathConstraint pc) {
        String value = PathConstraint.getValue(pc);
        value = value.replace(" ", "");
        if ("[list]".equalsIgnoreCase(value)) {
            return true;
        }
        return false;
    }

    public static boolean isPathContainingSubClass(Model model, String path) {
        if (path.contains("[") && path.contains("]")) {
            String name = path.substring(path.indexOf("[") + 1, path.indexOf("]"));
            if (model.getClassDescriptorByName(name) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFilterConstraint(final WidgetConfig config, PathConstraint pc) {
        String value = PathConstraint.getValue(pc);
        value = value.replace(" ", "");
        if (value.equalsIgnoreCase("[" + config.getFilterLabel() + "]")) {
            return true;
        }
        return false;
    }
}
