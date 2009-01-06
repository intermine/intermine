package org.intermine.web.logic;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathError;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Utility methods for Paths
 * @author Richard Smith
 *
 */
public class PathUtil 
{

    /**
     * Return the object at the end of a given path, starting from the given object.
     * @param path the path to resolve
     * @param o the start object
     * @return the attribute, object or collection at the end of the path
     */
    public static Object resolvePath(Path path, Object o) {
        Model model = path.getModel();
        if (path.getStartClassDescriptor() != null) {
            Set clds = model.getClassDescriptorsForClass(o.getClass());
            if (!clds.contains(path.getStartClassDescriptor())) {
                throw new PathError("ClassDescriptor from the start of path: " + path
                        + " is not a superclass of the class: "
                        + DynamicUtil.getFriendlyName(o.getClass()) + " while resolving object: "
                        + o, path.toString());
            }
        }

        Iterator<String> iter = path.getElements().iterator();

        Object current = o;

        while (iter.hasNext()) {
            String fieldName = iter.next();
            try {
                if (current == null) {
                    return null;
                }
                current = TypeUtil.getFieldValue(current, fieldName);
                if (current instanceof Collection) {
                    throw new RuntimeException("Attempt to to get value of "
                       + "field \"" + fieldName + "\" for collection: " + o
                       + "It must be simple object. This operation is not allowed for collection.");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("IllegalAccessException while trying to get value of "
                                           + "field \"" + fieldName + "\" in object: " + o, e);
            }
        }

        return current;
    }
}
