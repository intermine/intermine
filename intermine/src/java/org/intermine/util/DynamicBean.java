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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


import org.apache.log4j.Logger;

import net.sf.cglib.*;

/**
 * Class which represents a generic bean
 * @author Andrew Varley
 */
public class DynamicBean implements MethodInterceptor
{
    protected static final Logger LOG = Logger.getLogger(DynamicBean.class);

    private Map map = new HashMap();

    /**
     * Construct the interceptor
     */
    public DynamicBean() {
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
        return Enhancer.enhance(clazz, inter,
                                new DynamicBean());
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
            // Equals defined in the class
            return proxy.invokeSuper(obj, args);
        }
        if (method.getName().equals("hashCode")) {
            return proxy.invokeSuper(obj, args);
        }
        if (method.getName().equals("finalize")) {
            return null;
        }
        // Bean methods
        if (method.getName().startsWith("get")
            && (args.length == 0)) {
            Object retval = map.get(method.getName().substring(3));
            if ((retval == null) && Collection.class.isAssignableFrom(method.getReturnType())) {
                retval = new ArrayList();
                map.put(method.getName().substring(3), retval);
            }
            return retval;
        }
        if (method.getName().startsWith("is")
            && (args.length == 0)) {
            return map.get(method.getName().substring(2));
        }
        if (method.getName().startsWith("set")
            && (args.length == 1)
            && (method.getReturnType() == Void.TYPE)) {
            map.put(method.getName().substring(3), args[0]);
            return null;
        }
        return null;
    }
}
