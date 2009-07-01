package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.intermine.metadata.Model;
import org.intermine.util.TypeUtil;

/**
 * Class holding data source priority configuration for a Model.
 *
 * @author Matthew Wakeling
 */
public class PriorityConfig
{
    private Map<String, List<String>> descriptors;
    private Map<ClassAndFieldName, List<String>> cache
        = new HashMap<ClassAndFieldName, List<String>>();

    /**
     * Constructor.
     *
     * @param model the Model of the production database
     */
    public PriorityConfig(Model model) {
        descriptors = DataLoaderHelper.getDescriptors(model);
    }

    /**
     * Returns a List of data source names representing the priority for a given class and
     * fieldname pair. This method searches all the parent classes for a priority config, and
     * throws an exception if more than one is found. Results are cached for performance.
     *
     * @param clazz the class of the object being created
     * @param fieldName the name of the field that the priority is for
     * @return a List of data source names
     * @throws IllegalArgumentException if more than one priority config matches
     */
    protected synchronized List<String> getPriorities(Class clazz, String fieldName)
    throws IllegalArgumentException {
        ClassAndFieldName cafn = new ClassAndFieldName(clazz, fieldName);
        List<String> retval = cache.get(cafn);
        if (retval == null) {
            String firstHit = null;
            Set<Class> done = new HashSet<Class>();
            Stack<Class> todo = new Stack<Class>();
            todo.push(clazz);
            while (!todo.empty()) {
                Class next = todo.pop();
                if (!done.contains(next)) {
                    String className = TypeUtil.unqualifiedName(next.getName());
                    if (next.getSuperclass() != null) {
                        todo.push(next.getSuperclass());
                    }
                    for (Class inter : next.getInterfaces()) {
                        todo.push(inter);
                    }
                    String thisHit = className + "." + fieldName;
                    List<String> possibleResult = descriptors.get(thisHit);
                    if (possibleResult == null) {
                        thisHit = className;
                        possibleResult = descriptors.get(thisHit);
                    }
                    if (possibleResult != null) {
                        if (retval == null) {
                            retval = possibleResult;
                            firstHit = thisHit;
                        } else {
                            throw new IllegalArgumentException("There are multiple priorities"
                                    + " configured for " + clazz.getName() + "." + fieldName
                                    + ". Found a match on " + firstHit + " and " + thisHit);
                        }
                    }
                    done.add(next);
                }
            }
            cache.put(cafn, retval);
        }
        return retval;
    }

    private static class ClassAndFieldName
    {
        private Class clazz;
        private String fieldName;

        public ClassAndFieldName(Class clazz, String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
        }

        public int hashCode() {
            return 3 * clazz.hashCode() + 5 * fieldName.hashCode();
        }

        public boolean equals(Object o) {
            if (o instanceof ClassAndFieldName) {
                ClassAndFieldName c = (ClassAndFieldName) o;
                return clazz.equals(c.clazz) && fieldName.equals(c.fieldName);
            }
            return false;
        }

        public String toString() {
            return clazz.getName() + "." + fieldName;
        }
    }
}
