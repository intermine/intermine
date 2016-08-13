package org.intermine.api.util;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.Util;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathException;
import org.intermine.util.DynamicUtil;

/**
 * Utility methods for Paths
 * @author Richard Smith
 *
 */
public final class PathUtil
{
    private PathUtil() {
        // don't instantitate
    }

    /**
     * Return the object at the end of a given path, starting from the given object.
     *
     * @param path the path to resolve
     * @param o the start object
     * @return the attribute, object or collection at the end of the path
     * @throws PathException if the path does not match the object type
     */
    public static Object resolvePath(Path path, Object o) throws PathException {
        Model model = path.getModel();
        if (path.getStartClassDescriptor() != null) {
            Set<ClassDescriptor> clds = model.getClassDescriptorsForClass(o.getClass());
            if (!clds.contains(path.getStartClassDescriptor())) {
                throw new PathException("ClassDescriptor from the start of path: " + path
                        + " is not a superclass of the class: "
                        + Util.getFriendlyName(o.getClass()) + " while resolving object: "
                        + o, path.toString());
            }
        }
        Object current = o;
        for (String fieldName : path.getElements()) {
            try {
                if (current == null) {
                    return null;
                }
                current = TypeUtil.getFieldValue(current, fieldName);
                if (current instanceof Collection<?>) {
                    throw new RuntimeException("Attempt to to get value of "
                            + "field \"" + fieldName + "\" for collection: " + o
                            + "It must be simple object. This operation is not allowed for "
                            + "collection.");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("IllegalAccessException while trying to get value of "
                                           + "field \"" + fieldName + "\" in object: " + o, e);
            }
        }
        return current;
    }

    /**
     * Return the object at the end of a given path, starting from the given object. Works with
     * Collections of objects and reverse references.
     *
     * @param path the path to resolve
     * @param o the start object
     * @return the attribute, object or collection at the end of the path
     * @throws PathException if the path does not match the object type
     */
    @SuppressWarnings("unchecked")
    public static Set<Object> resolveCollectionPath(Path path, Object o) throws PathException {
        // early bath for him
        if (!path.containsCollections()) {
            // return result as a Set
            return new HashSet<Object>(Collections.singleton(resolvePath(path, o)));
        }
        Model model = path.getModel();
        if (path.getStartClassDescriptor() != null) {
            Set<ClassDescriptor> clds = model.getClassDescriptorsForClass(o.getClass());
            if (!clds.contains(path.getStartClassDescriptor())) {
                throw new PathException("ClassDescriptor from the start of path: " + path
                        + " is not a superclass of the class: "
                        + Util.getFriendlyName(o.getClass()) + " while resolving object: "
                        + o, path.toString());
            }
        }

        Object current = o;

        // elements in a path we will abuse on collection retrieval
        List<String> pathStrings = path.getElements();
        // traverse all elements in a Path
        for (int i = 0; i < path.getElements().size(); i++) {

            // fetch the field
            String fieldName = pathStrings.get(i);

            try {
                if (current == null) {
                    return null;
                }
                current = TypeUtil.getFieldValue(current, fieldName);

                // do we have a Collection?
                if (current instanceof Collection<?>) {

                    // traverse all of the objects and resolve path in them
                    HashSet<Object> resultList = new HashSet<Object>();
                    for (Object element : (Collection<?>) current) {

                        // what is the class type?
                        String objectClass = DynamicUtil.getSimpleClass(
                                (Class<? extends FastPathObject>) element.getClass())
                                .getSimpleName();

                        // form a new path string starting with the next element
                        //  in the path, separated by a '.'
                        String pathString = "";
                        for (int k = i + 1; k < pathStrings.size(); k++) {
                            pathString += '.' + pathStrings.get(k);
                        }

                        // form a new path
                        path = new Path(model, objectClass + pathString);

                        // add resolved result
                        Object resultObject = resolveCollectionPath(path, element);
                        if (resultObject instanceof HashSet<?>) { // sets of sets
                            for (Object innerElement : (HashSet<?>) resultObject) {
                                resultList.add(innerElement);
                            }
                        } else {
                            // add the object
                            resultList.add(resultObject);
                        }
                    }
                    return resultList;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("IllegalAccessException while trying to get value of "
                                           + "field \"" + fieldName + "\" in object: " + o, e);
            }
        }

        // we will never reach this point
        return new HashSet<Object>(Collections.singleton(null));
    }

    /**
     * Return true if given type (of a constraint) can be assigned to the InterMineObject - i.e.
     * if the class or any superclass of the InterMineObject are the type.  Type can be a qualified
     * or unqualified class name.
     *
     * @param cls the class in the model that will be assigned to
     * @param obj the InterMineObject to check
     * @return a boolean
     */
    public static boolean canAssignObjectToType(Class<?> cls, InterMineObject obj) {
        for (Class<?> c : Util.decomposeClass(obj.getClass())) {
            if (cls.isAssignableFrom(c)) {
                return true;
            }
        }
        return false;
    }
}
