package org.intermine.web.logic.widget.config;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.metadata.Model;
import org.intermine.pathquery.PathConstraint;

/**
 * Utilities for handling the widget configuration
 * @author Various Artists
 *
 */
public final class WidgetConfigUtil
{
    private WidgetConfigUtil() {
        // Hidden constructor.
    }

    /**
     * @param pc A constraint to test.
     * @return whether the constraint is a list constraint **/
    public static boolean isListConstraint(PathConstraint pc) {
        String value = PathConstraint.getValue(pc);
        value = value.replace(" ", "");
        if ("[list]".equalsIgnoreCase(value)) {
            return true;
        }
        return false;
    }

    /**
     * Check to see if the path contains a subclass.
     * @param model The data model.
     * @param path The path.
     * @return whether the path contains a sub-class.
     */
    public static boolean isPathContainingSubClass(Model model, String path) {
        if (path.contains("[") && path.contains("]")) {
            String name = path.substring(path.indexOf("[") + 1, path.indexOf("]"));
            if (model.getClassDescriptorByName(name) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param model The data model.
     * @param path A path.
     * @return A string without the subclass constraints.
     */
    public static String getPathWithoutSubClass(Model model, String path) {
        if (isPathContainingSubClass(model, path)) {
            path = path.substring(0, path.indexOf("[")) + path.substring(path.indexOf("]") + 1);
        }
        return path;
    }

    /**
     * @param config The widget config.
     * @param pc A constraint to test.
     * @return whether this constraint is a filter constraint.
     */
    public static boolean isFilterConstraint(final WidgetConfig config, PathConstraint pc) {
        String value = PathConstraint.getValue(pc);
        value = value.replace(" ", "");
        if (value.equalsIgnoreCase("[" + config.getFilterLabel() + "]")) {
            return true;
        }
        return false;
    }
}
