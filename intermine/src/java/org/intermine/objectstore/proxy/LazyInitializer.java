package org.flymine.objectstore.proxy;

import net.sf.cglib.*;
import java.lang.reflect.Method;

import org.flymine.objectstore.ObjectStore;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.ResultsRow;

/**
 * Class which intercepts all method calls to proxy
 * @author Mark Woodbridge
 */
public class LazyInitializer implements MethodInterceptor
{
    private Query query;
    private Object realSubject;
    private ObjectStore os;
    private Integer id;

    /**
     * Construct a dynamic proxy for a given class representing a persistent object
     *
     * @param cls the class to proxy
     * @param query the query that retrieves the real object
     * @param id the internal id of the persistent object
     * @return the proxy object
     */
    public static Object getDynamicProxy(Class cls, Query query, Integer id) {
        return Enhancer.enhance(cls, new Class[] {LazyReference.class},
                                new LazyInitializer(query, id));
    }

    /**
     * Construct the interceptor using an object identifier
     * @param query the query that retrieves the real object
     * @param id the internal id of the persistent object
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
        if (method.getName().equals("finalize")) {
            return null;
        }
        if (method.getName().equals("setObjectStore")) {
            this.os = (ObjectStore) args[0];
            return null;
        }
        if (method.getName().equals("equals")) {
            Object o = args[0];
            if (o == null) {
                return null;
            }
            java.lang.reflect.Field f = o.getClass().getDeclaredField("id");
            f.setAccessible(true);
            int otherId = f.getInt(o);
            if (otherId != 0) {
                return new Boolean(id.intValue() == otherId);
            }
        }
        if (method.getName().equals("hashCode")) {
            return id;
        }
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
                throw new Exception(method.getName() + "realSubject is still null");
            }
        }
        return method.invoke(realSubject, args);
    }
}
