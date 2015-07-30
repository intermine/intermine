package org.intermine.util;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.Factory;

import org.intermine.metadata.TypeUtil;
import org.intermine.metadata.Util;
import org.intermine.model.FastPathObject;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyReference;

/**
 * Utilities to create DynamicBeans
 *
 * @author Andrew Varley
 */
public final class DynamicUtil
{
    private static Map<Class<?>, String> simpleNameMap = new HashMap<Class<?>, String>();

    /**
     * Cannot construct
     */
    private DynamicUtil() {
        // don't instantiate
    }

    /**
     * Create a DynamicBean from a class or interface.
     *
     * @param cls the classes and interfaces to extend/implement
     * @return the DynamicBean
     * @throws IllegalArgumentException if there is more than one Class, or if fields are not
     * compatible.
     */
    @SuppressWarnings("unchecked")
    public static synchronized <C extends FastPathObject> C createObject(Class<C> cls) {
        if (cls == null) {
            throw new IllegalArgumentException("Class can't be null when creating a DynamicObject");
        }
        if (cls.isInterface()) {
            try {
                Class<C> retval = (Class<C>) Class.forName(
                        cls.getName() + "Shadow");
                return createObject(retval);
            } catch (ClassNotFoundException e) {
                // No problem - falling back on dynamic
            }
            return DynamicBean.create(cls);
        } else {
            C retval = null;
            try {
                retval = cls.newInstance();
            } catch (Exception e) {
                IllegalArgumentException e2 = new IllegalArgumentException();
                e2.initCause(e);
                throw e2;
            }
            if (retval instanceof Factory) {
                ((Factory) retval).setCallback(0, new DynamicBean());
            }
            return retval;
        }
    }

    /**
     * Get the actual class from a class that may be a dynamic class. For dynamic objects that
     * represent the instantiation of an interface the interface class is returned. For interfaces
     * where a Shadow class is used the interface class is returned. This util method is needed
     * because DynamicBean can't override getClass().
     * @param cls a class, usually from the data model
     * @return the class (usually from the data model), this may be an interface
     */
    // NOTE - due to dependencies the code for this method is in a Util class in the model project
    //        but it is clearer if code in the objectstore project calls DynamicUtil.getClass().
    //        Also creation of dynamic objects is required to test this method which requires that
    //        the test is in the objectstore project.
    public static Class<?> getClass(Class<?> cls) {
        return Util.dynamicGetClass(cls);
    }

    /**
     * Get the actual class from an object that may be a dynamic class. For dynamic objects that
     * represent the instantiation of an interface the interface class is returned. For interfaces
     * where a Shadow class is used the interface class is returned. This util method is needed
     * because DynamicBean can't override getClass().
     * @param o an object from the data model
     * @return the class (usually from the data model) that describes the object, this may be an
     * interface
     */
    public static Class<?> getClass(Object o) {
        return getClass(o.getClass());
    }

    /**
     * Create a new object given a class, which may be an interface. This method is equivalent to
     * calling createObject(Collections.singleton(clazz)), except that it is genericised.
     *
     * @param clazz the class of the object to instantiate
     * @param <C> The type of the object that is expected
     * @return the object
     * @throws IllegalArgumentException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static <C extends FastPathObject> C simpleCreateObject(Class<C> clazz) {
        // TODO this method is now no different to standard createObject()
        return createObject(clazz);
    }

    /**
     * Convert a set of interface names to a set of Class objects
     *
     * @param names the set of interface names
     * @return set of Class objects
     * @throws ClassNotFoundException if class cannot be found
     */
    protected static Set<Class<?>> convertToClasses(Set<String> names)
        throws ClassNotFoundException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        Iterator<String> iter = names.iterator();
        while (iter.hasNext()) {
            classes.add(Class.forName(iter.next()));
        }

        return classes;
    }

    /**
     * Creates a friendly description of an object - that is, the class and the ID (if it has one).
     *
     * @param o the object to be described
     * @return a String description
     */
    public static String getFriendlyDesc(Object o) {
        if (o instanceof InterMineObject) {
            return Util.getFriendlyName(o.getClass()) + ":" + ((InterMineObject) o).getId();
        } else {
            return o.toString();
        }
    }

    /**
     * Returns the simple class name for the given class or throws an exception if
     * there are more than one.
     * @param clazz the class
     * @return the simple class name
     */
    public static synchronized String getSimpleClassName(Class<?> clazz) {
        String retval = simpleNameMap.get(clazz);
        if (retval == null) {
            Set<Class<?>> decomposedClass = Util.decomposeClass(clazz);
            if (decomposedClass.size() > 1) {
                throw new IllegalArgumentException("No simple name for class: "
                                                   + Util.getFriendlyName(clazz));
            } else {
                retval = decomposedClass.iterator().next().getName();
                simpleNameMap.put(clazz, retval);
            }

        }
        return retval;
    }

    /**
     * Returns the simple class name for the given object or throws an exception if
     * there are more than one.
     * @param obj an object from the model
     * @return the simple class name
     */
    public static synchronized String getSimpleClassName(FastPathObject obj) {
        return getSimpleClassName(obj.getClass());
    }

    /**
     * Returns true if sup is a superclass of sub (or the same), taking into account dynamic
     * classes.
     *
     * @param sup the supposed superclass
     * @param sub the supposed subclass
     * @return a boolean
     */
    public static boolean isAssignableFrom(Class<?> sup, Class<?> sub) {
        Set<Class<?>> classes = Util.decomposeClass(sup);
        for (Class<?> clazz : classes) {
            if (!clazz.isAssignableFrom(sub)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if obj is an instance of clazz, taking into account dynamic classes.
     *
     * @param obj the Object
     * @param clazz the Class
     * @return a boolean
     */
    public static boolean isInstance(Object obj, Class<?> clazz) {
        return isAssignableFrom(clazz, obj.getClass());
    }


    /**
     * Returns the result of decomposeClass if that is a single class, or throws an exception if
     * there are more than one.
     *
     * @param clazz the class
     * @return the corresponding non-dynamic class
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends FastPathObject> getSimpleClass(
            Class<? extends FastPathObject> clazz) {
        Set<Class<?>> decomposed = Util.decomposeClass(clazz);
        if (decomposed.size() > 1) {
            throw new IllegalArgumentException("No simple class for "
                    + Util.getFriendlyName(clazz));
        }
        return (Class) decomposed.iterator().next();
    }

    /**
     * For the given object returns the result of decomposeClass if that is a single class, or
     * throws an exception if there are more than one class.
     *
     * @param obj an object from the model
     * @return the corresponding non-dynamic class
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends FastPathObject> getSimpleClass(FastPathObject obj) {
        return getSimpleClass(obj.getClass());
    }



    /**
     * Sets the value of a public or protected Field of an Object given the field name.
     * This used to be in TypeUtil.
     *
     * @param o the Object
     * @param fieldName the name of the relevant Field
     * @param fieldValue the value of the Field
     */
    public static void setFieldValue(Object o, String fieldName, Object fieldValue) {
        try {
            if (fieldValue instanceof ProxyReference) {
                TypeUtil.getProxySetter(o.getClass(), fieldName).invoke(o,
                        new Object[] {fieldValue});
            } else {
                TypeUtil.getSetter(o.getClass(), fieldName).invoke(o, new Object[] {fieldValue});
            }
        } catch (Exception e) {
            String type = null;
            try {
                type = TypeUtil.getFieldInfo(o.getClass(),
                        fieldName).getGetter().getReturnType().getName();
            } catch (Exception e3) {
                // ignore
            }
            IllegalArgumentException e2 = new IllegalArgumentException("Couldn't set field \""
                    + Util.getFriendlyName(o.getClass()) + "." + fieldName + "\""
                    + (type == null ? "" : " (a " + type + ")")
                    + " to \"" + fieldValue + "\" (a " + fieldValue.getClass().getName() + ")");
            e2.initCause(e);
            throw e2;
        }
    }
}
