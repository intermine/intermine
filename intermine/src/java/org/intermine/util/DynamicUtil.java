package org.flymine.util;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.*;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;

/**
 * Utilities to create DynamicBeans
 *
 * @author Andrew Varley
 */
public class DynamicUtil
{
    private static Map classMap = new HashMap();

    /**
     * Cannot construct
     */
    private DynamicUtil() {
    }

    /**
     * Create a DynamicBean from a Set of Class objects
     *
     * @param classes the classes and interfaces to extend/implement
     * @return the DynamicBean
     * @throws IllegalArgumentException if there is more than one Class, or if fields are not
     * compatible.
     */
    public static synchronized Object createObject(Set classes) throws IllegalArgumentException {
        Class requiredClass = (Class) classMap.get(classes);
        if (requiredClass != null) {
            Object retval = null;
            try {
                retval = requiredClass.newInstance();
            } catch (Exception e) {
                IllegalArgumentException e2 = new IllegalArgumentException();
                e2.initCause(e);
                throw e2;
            }
            if (retval instanceof Factory) {
                ((Factory) retval).interceptor(new DynamicBean());
            }
            return retval;
        } else {
            Iterator classIter = classes.iterator();
            Class clazz = null;
            Set interfaces = new HashSet();
            while (classIter.hasNext()) {
                Class cls = (Class) classIter.next();
                if (cls.isInterface()) {
                    interfaces.add(cls);
                } else if ((clazz == null) || clazz.isAssignableFrom(cls)) {
                    clazz = cls;
                } else if (!cls.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("Cannot create a class from multiple"
                            + " classes: " + classes);
                }
            }
            if (clazz != null) {
                interfaces.removeAll(Arrays.asList(clazz.getInterfaces()));
            }
            if (interfaces.isEmpty()) {
                if (clazz == null) {
                    throw new IllegalArgumentException("Cannot create a class from nothing");
                } else {
                    try {
                        classMap.put(classes, clazz);
                        return clazz.newInstance();
                    } catch (InstantiationException e) {
                        IllegalArgumentException e2 = new IllegalArgumentException("Problem running"
                                + " constructor");
                        e2.initCause(e);
                        throw e2;
                    } catch (IllegalAccessException e) {
                        IllegalArgumentException e2 = new IllegalArgumentException("Problem running"
                                + " constructor");
                        e2.initCause(e);
                        throw e2;
                    }
                }
            }
            Object retval = DynamicBean.create(clazz, (Class [])
                    interfaces.toArray(new Class[] {}));
            classMap.put(classes, retval.getClass());
            return retval;
        }
    }

    /**
     * Return the Class for a set of Class objects. NOTE: Creating an instance of this class is not
     * trivial: after calling Class.newInstance(), cast the Object to net.sf.cglib.Factory, and call
     * interceptor(new org.flymine.util.DynamicBean()) on it.
     *
     * @param classes the classes and interfaces to extend/implement
     * @return the Class
     * @throws IllegalArgumentException if there is more than one Class, or if the fields are not
     * compatible.
     */
    public static synchronized Class composeClass(Set classes) throws IllegalArgumentException {
        Class retval = (Class) classMap.get(classes);
        if (retval == null) {
            retval = createObject(classes).getClass();
        }
        return retval;
    }
    
    /**
     * Create a DynamicBean from a set of interface names
     *
     * @param model the Model we are using
     * @param interfaces the interfaces to implement
     * @return the DynamicBean
     * @throws ClassNotFoundException if any class cannot be found
     */
    public static Object createObject(Model model, Set interfaces) throws ClassNotFoundException {
        Set expandedInterfaces = new HashSet();
        Iterator intIter = interfaces.iterator();
        while (intIter.hasNext()) {
            String intName = (String) intIter.next();
            ClassDescriptor cld = model.getClassDescriptorByName(intName);
            if (cld == null) {
                throw new ClassNotFoundException("Cannot find " + intName
                                                 + " in " + model.getName());
            }
            if (!cld.isInterface()) {
                throw new IllegalArgumentException(intName + " is not an interface");
            }
        }

        // Now create the object
        return DynamicBean.create(null, convertToClassArray(interfaces));

    }

    /**
     * Convert a set of interface names to an array of Class objects
     *
     * @param names the set of interface names
     * @return array of Class objects
     * @throws ClassNotFoundException if class cannot be found
     */
    protected static Class [] convertToClassArray(Set names) throws ClassNotFoundException {
        Iterator iter = names.iterator();
        List list = new ArrayList();

        while (iter.hasNext()) {
            list.add(Class.forName((String) iter.next()));
        }

        return (Class []) list.toArray(new Class [] {});
    }

    /**
     * Convert a dynamic Class into a Set of Class objects that comprise it.
     *
     * @param clazz the Class to decompose
     * @return a Set of Class objects
     */
    public static Set decomposeClass(Class clazz) {
        if (net.sf.cglib.Factory.class.isAssignableFrom(clazz)) {
            // Decompose
            Set retval = new LinkedHashSet();
            retval.add(clazz.getSuperclass());
            Class interfs[] = clazz.getInterfaces();
            for (int i = 0; i < interfs.length; i++) {
                Class inter = interfs[i];
                if (net.sf.cglib.Factory.class != inter) {
                    boolean notIn = true;
                    Iterator inIter = retval.iterator();
                    while (inIter.hasNext() && notIn) {
                        Class in = (Class) inIter.next();
                        if (in.isAssignableFrom(inter)) {
                            // That means that the one already in the return value is more general
                            // than the one we are about to put in, so we can get rid of the one
                            // already in.
                            inIter.remove();
                        }
                        if (inter.isAssignableFrom(in)) {
                            // That means that the one already in the return value is more specific
                            // than the one we would have added, so don't bother.
                            notIn = false;
                        }
                    }
                    if (notIn) {
                        retval.add(inter);
                    }
                }
            }
            return retval;
        } else {
            // Normal class - return it.
            return Collections.singleton(clazz);
        }
    }
}
