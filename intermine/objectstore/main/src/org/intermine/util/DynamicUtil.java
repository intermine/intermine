package org.intermine.util;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.*;

/**
 * Utilities to create DynamicBeans
 *
 * @author Andrew Varley
 */
public class DynamicUtil
{
    private static Map classMap = new HashMap();
    private static Map decomposeMap = new HashMap();
    private static Map friendlyNameMap = new HashMap();

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
                ((Factory) retval).setCallback(0, new DynamicBean());
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
                    throw new IllegalArgumentException("Cannot create an object without a class "
                                                       + "for: " + classes);
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
     * trivial: after calling Class.newInstance(), cast the Object to net.sf.cglib.proxy.Factory,
     * and call interceptor(new org.intermine.util.DynamicBean()) on it.
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
     * Convert a set of interface names to a set of Class objects
     *
     * @param names the set of interface names
     * @return set of Class objects
     * @throws ClassNotFoundException if class cannot be found
     */
    protected static Set convertToClasses(Set names) throws ClassNotFoundException {
        Set classes = new HashSet();
        Iterator iter = names.iterator();
        while (iter.hasNext()) {
            classes.add(Class.forName((String) iter.next()));
        }

        return classes;
    }

    /**
     * Convert a dynamic Class into a Set of Class objects that comprise it.
     *
     * @param clazz the Class to decompose
     * @return a Set of Class objects
     */
    public static synchronized Set decomposeClass(Class clazz) {
        Set retval = (Set) decomposeMap.get(clazz);
        if (retval == null) {
            if (net.sf.cglib.proxy.Factory.class.isAssignableFrom(clazz)) {
                // Decompose
                retval = new LinkedHashSet();
                retval.add(clazz.getSuperclass());
                Class interfs[] = clazz.getInterfaces();
                for (int i = 0; i < interfs.length; i++) {
                    Class inter = interfs[i];
                    if (net.sf.cglib.proxy.Factory.class != inter) {
                        boolean notIn = true;
                        Iterator inIter = retval.iterator();
                        while (inIter.hasNext() && notIn) {
                            Class in = (Class) inIter.next();
                            if (in.isAssignableFrom(inter)) {
                                // That means that the one already in the return value is more
                                // general than the one we are about to put in, so we can get rid
                                // of the one already in.
                                inIter.remove();
                            }
                            if (inter.isAssignableFrom(in)) {
                                // That means that the one already in the return value is more
                                // specific than the one we would have added, so don't bother.
                                notIn = false;
                            }
                        }
                        if (notIn) {
                            retval.add(inter);
                        }
                    }
                }
            } else {
                // Normal class - return it.
                retval = Collections.singleton(clazz);
            }
            decomposeMap.put(clazz, retval);
        }
        return retval;
    }

    /**
     * Create an outline business object from a class name and a list of interface names
     * @param className the class name
     * @param implementations a space separated list of interface names
     * @return the materialised business object
     */
    public static Object instantiateObject(String className, String implementations) {

        Set classNames = new HashSet();

        if (className != null && !"".equals(className) && !"".equals(className.trim())) {
            classNames.add(className.trim());
        }

        if (implementations != null) {
            classNames.addAll(StringUtil.tokenize(implementations));
        }

        if (classNames.size() == 0) {
            throw new RuntimeException("attempted to create an object without specifying any "
                                       + "classes or interfaces");
        }

        try {
            return DynamicUtil.createObject(convertToClasses(classNames));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot find one of '" + classNames + "' in model", e);
        }
    }

    /**
     * Creates a friendly name for a given class.
     *
     * @param clazz the class
     * @return a String describing the class, without package names
     */
    public static synchronized String getFriendlyName(Class clazz) {
        String retval = (String) friendlyNameMap.get(clazz);
        if (retval == null) {
            retval = "";
            Iterator iter = decomposeClass(clazz).iterator();
            boolean needComma = false;
            while (iter.hasNext()) {
                Class constit = (Class) iter.next();
                retval += needComma ? "," : "";
                needComma = true;
                retval += constit.getName().substring(constit.getName().lastIndexOf('.') + 1);
            }
            friendlyNameMap.put(clazz, retval);
        }
        return retval;
    }
}
