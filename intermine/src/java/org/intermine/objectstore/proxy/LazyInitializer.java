/* 
 * Copyright (C) 2002-2003 FlyMine
 * 
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more 
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

package org.flymine.objectstore.proxy;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import net.sf.cglib.*;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.ResultsRow;
import org.flymine.util.TypeUtil;

/**
 * Class which intercepts all method calls to proxy
 * @author Mark Woodbridge
 */
public class LazyInitializer implements MethodInterceptor
{
    protected static final Logger LOG = Logger.getLogger(LazyInitializer.class);

    private Query query;
    private Object realSubject;
    private ObjectStore os;
    private Integer id;

    /**
     * Construct a dynamic proxy for a given class representing a persistent object
     *
     * @param cls the class to proxy
     * @param query the query that retrieves the real object
     * @param id the internal id of the real object
     * @return the proxy object
     */
    public static Object getDynamicProxy(Class cls, Query query, Integer id) {
        return Enhancer.enhance(cls, new Class[] {LazyReference.class},
                                new LazyInitializer(query, id));
    }

    /**
     * Construct the interceptor using an object identifier
     * @param query the query that retrieves the real object
     * @param id the internal id of the real object
     */
    LazyInitializer(Query query, Integer id) {
        this.query = query;
        this.id = id;
    }

    /**
     * Intercept all method calls, materialise real object (if necessary), and forward call
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
            Integer otherId = (Integer) TypeUtil.getFieldValue(args[0], "id");
            return otherId == null ? new Boolean(false) : new Boolean(id.equals(otherId));
        }
        if (method.getName().equals("hashCode")) {
            return new Integer(id.hashCode());
        }
        if (method.getName().equals("finalize")) {
            return null;
        }
        // org.flymine.objectstore.proxy.LazyReference methods
        if (method.getName().equals("setObjectStore")) {
            this.os = (ObjectStore) args[0];
            return null;
        }
        if (method.getName().equals("isMaterialised")) {
            return new Boolean(realSubject != null);
        }
        // org.flymine.model method
        if (method.getName().equals("getId")) {
            return id;
        }
        // any other method...
        if (realSubject == null) {
            if (os == null) {
                throw new Exception(method.getName() + ": ObjectStore is null");
            }
            try {
                realSubject = ((ResultsRow) os.execute(query).get(0)).get(0);
            } catch (Exception e) {
                throw new Exception(method.getName() + ": Materialization problem: " + e);
            }
            if (realSubject == null) {
                throw new Exception(method.getName() + ": realSubject is still null");
            }
        }
        return method.invoke(realSubject, args);
    }
}
