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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.sf.cglib.proxy.*;

import org.intermine.model.InterMineObject;
import org.intermine.objectstore.proxy.ProxyReference;

/**
 * Class which represents a generic bean
 * @author Andrew Varley
 */
public class DynamicBean implements MethodInterceptor
{
    private Map map = new HashMap();

    /**
     * Construct the interceptor
     */
    public DynamicBean() {
        // empty
    }

    /**
     * Create a DynamicBean
     *
     * @param clazz the class to extend
     * @param inter the interfaces to implement
     * @return the DynamicBean
     */
    public static Object create(Class clazz, Class [] inter) {
        if ((clazz != null) && clazz.isInterface()) {
            throw new IllegalArgumentException("clazz must not be an interface");
        }
        // If Enhancer.create() called with a null class it will alter java.lang.Object
        // this causes a security exception if run with Kaffe JRE
        //if ( clazz == null) {
        //    clazz = DynamicBean.class;
        //}
        return Enhancer.create(clazz, inter, new DynamicBean());
    }

    /**
     * Intercept all method calls, and operate on Map.
     * Note that final methods (eg. getClass) cannot be intercepted
     *
     * @param obj the proxy
     * @param method the method called
     * @param args the parameters
     * @param proxy the method proxy
     * @return the return value of the real method call
     * @throws Throwable if an error occurs in executing the real method
     */
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy)
        throws Throwable {
        // java.lang.Object methods
        if (method.getName().equals("equals")) {
            if (args[0] instanceof InterMineObject) {
                Integer otherId = ((InterMineObject) args[0]).getId();
                Integer thisId = (Integer) map.get("Id");
                return Boolean.valueOf((otherId != null) && (thisId != null)
                        && thisId.equals(otherId));
            }
            return Boolean.FALSE;
        }
        if (method.getName().equals("hashCode")) {
            return map.get("Id");
        }
        if (method.getName().equals("finalize")) {
            return null;
        }
        if (method.getName().equals("toString")) {
            StringBuffer className = new StringBuffer();
            boolean needComma = false;
            Set classes = DynamicUtil.decomposeClass(obj.getClass());
            Iterator classIter = classes.iterator();
            while (classIter.hasNext()) {
                if (needComma) {
                    className.append(",");
                }
                Class clazz = (Class) classIter.next();
                className.append(TypeUtil.unqualifiedName(clazz.getName()));
            }
            StringBuffer retval = new StringBuffer(className.toString() + " [");
            Map sortedMap = new TreeMap(map);
            Iterator mapIter = sortedMap.entrySet().iterator();
            needComma = false;
            while (mapIter.hasNext()) {
                Map.Entry mapEntry = (Map.Entry) mapIter.next();
                String fieldName = (String) mapEntry.getKey();
                Object fieldValue = mapEntry.getValue();
                if (needComma) {
                    retval.append(", ");
                }
                needComma = true;
                if (fieldValue instanceof ProxyReference) {
                    retval.append(fieldName + "=" + ((ProxyReference) fieldValue).getId());
                } else if (fieldValue instanceof InterMineObject) {
                    retval.append(fieldName + "=" + ((InterMineObject) fieldValue).getId());
                } else if (fieldValue instanceof Collection) {
                    retval.append(fieldName + ":Collection");
                } else {
                    retval.append(fieldName + "=\"" + fieldValue + "\"");
                }
            }
            return retval.toString() + "]";
        }
        // Bean methods
        if (method.getName().startsWith("get") && (args.length == 0)) {
            Object retval = map.get(method.getName().substring(3));
            if (retval instanceof ProxyReference) {
                retval = ((ProxyReference) retval).getObject();
            }
            if ((retval == null) && Collection.class.isAssignableFrom(method.getReturnType())) {
                retval = new HashSet();
                map.put(method.getName().substring(3), retval);
            }
            return retval;
        }
        if (method.getName().startsWith("is")
            && (args.length == 0)) {
            return map.get(method.getName().substring(2));
        }
        if (method.getName().startsWith("set") && (args.length == 1)
                && (method.getReturnType() == Void.TYPE)) {
            map.put(method.getName().substring(3), args[0]);
            return null;
        }
        if (method.getName().startsWith("proxy") && (args.length == 1)
                && (method.getReturnType() == Void.TYPE)) {
            map.put(method.getName().substring(5), args[0]);
            return null;
        }
        if (method.getName().startsWith("proxGet") && (args.length == 0)) {
            return map.get(method.getName().substring(7));
        }
        if (method.getName().startsWith("add") && (args.length == 1)
                && (method.getReturnType() == Void.TYPE)) {
            Collection col = (Collection) map.get(method.getName().substring(3));
            if (col == null) {
                col = new HashSet();
                map.put(method.getName().substring(3), col);
            }
            col.add(args[0]);
            return null;
        }
        throw new IllegalArgumentException("No definition for method " + method);
    }

    /**
     * Getter for the map, for testing purposes
     *
     * @return a map of data for this object
     */
    public Map getMap() {
        return map;
    }
}
