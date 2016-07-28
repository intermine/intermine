package org.intermine.dataloader;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.intermine.metadata.ClassDescriptor;
import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.model.FastPathObject;
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
     * Verifies the priorities config given the list of data sources.
     *
     * @param model the Model of the production database
     * @param sources the space-separated list of data sources
     * @throws IllegalArgumentException if a source is mentioned in the priorities file but not the
     * project.xml file
     */
    public static void verify(Model model, String sources) {
        Set<String> allSources = new HashSet<String>(Arrays.asList(sources.split(" ")));
        Set<String> seenSources = new HashSet<String>();
        Map<String, List<String>> descriptors = DataLoaderHelper.getDescriptors(model);
        for (List<String> descSources : descriptors.values()) {
            for (String source : descSources) {
                seenSources.add(source);
            }
        }
        seenSources.remove("*");
        Set<String> extraSources = new HashSet<String>();
        for (String seenSource : seenSources) {
            if (!allSources.contains(seenSource)) {
                extraSources.add(seenSource);
            }
        }
        Set<String> unseenSources = new HashSet<String>();
        for (String allSource : allSources) {
            if (!seenSources.contains(allSource)) {
                unseenSources.add(allSource);
            }
        }

        //if (!unseenSources.isEmpty()) {
        //    System.err .println("Sources only present in project.xml: " + unseenSources);
        //}
        if (!extraSources.isEmpty()) {
            //System.err .println("Sources only present in priorites: " + extraSources);
            throw new IllegalArgumentException("Some sources mentioned in the priorities config "
                    + "file do not exist in project.xml - maybe they are typos: " + extraSources);
        }
    }

    /**
     * Constructor.
     *
     * @param model the Model of the production database
     * @throws IllegalArgumentException if the priorities are misconfigured
     */
    public PriorityConfig(Model model) {
        descriptors = DataLoaderHelper.getDescriptors(model);
        for (String key : descriptors.keySet()) {
            if (key.indexOf(".") == -1) {
                ClassDescriptor cld = model.getClassDescriptorByName(key);
                if (cld == null) {
                    throw new IllegalArgumentException("Class '" + key + "' not found in model, "
                            + "check priorities configuration file.");
                }
                Class<? extends FastPathObject> clazz = cld.getType();
                for (FieldDescriptor field : cld.getFieldDescriptors()) {
                    if (!field.isCollection()) {
                        getPriorities(clazz, field.getName());
                    }
                }
            } else {
                String className = key.substring(0, key.indexOf("."));
                String fieldName = key.substring(key.indexOf(".") + 1);
                ClassDescriptor cld = model.getClassDescriptorByName(className);
                if (cld == null) {
                    throw new IllegalArgumentException("Class '" + className + "' not found in "
                            + "model, check priorities configuration file - bad entry is "
                            + key + ".");
                }
                Class<? extends FastPathObject> clazz = cld.getType();
                FieldDescriptor field = cld.getFieldDescriptorByName(fieldName);
                if ((field != null) && (!field.isCollection())) {
                    getPriorities(clazz, fieldName);
                } else {
                    throw new IllegalArgumentException("Bad entry in priorities file: " + key
                            + " - " + className + " does not have a field " + fieldName);
                }
            }
        }
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
    protected synchronized List<String> getPriorities(Class<? extends FastPathObject> clazz,
            String fieldName) {
        ClassAndFieldName cafn = new ClassAndFieldName(clazz, fieldName);
        List<String> retval = cache.get(cafn);
        if (retval == null) {
            String firstHit = null;
            Set<Class<?>> done = new HashSet<Class<?>>();
            Stack<Class<?>> todo = new Stack<Class<?>>();
            todo.push(clazz);
            while (!todo.empty()) {
                Class<?> next = todo.pop();
                if (!done.contains(next)) {
                    String className = TypeUtil.unqualifiedName(next.getName());
                    if (next.getSuperclass() != null) {
                        todo.push(next.getSuperclass());
                    }
                    for (Class<?> inter : next.getInterfaces()) {
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
        private Class<?> clazz;
        private String fieldName;

        public ClassAndFieldName(Class<?> clazz, String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
        }

        @Override
        public int hashCode() {
            return 3 * clazz.hashCode() + 5 * fieldName.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ClassAndFieldName) {
                ClassAndFieldName c = (ClassAndFieldName) o;
                return clazz.equals(c.clazz) && fieldName.equals(c.fieldName);
            }
            return false;
        }

        @Override
        public String toString() {
            return clazz.getName() + "." + fieldName;
        }
    }
}
