package org.intermine.webservice.server.core;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.WebService;

/** @author Alex Kalderimis **/
public class RestfulServlet extends WebServiceServlet
{

    private static final long serialVersionUID = -6545928555512220185L;

    private Map<Method, Constructor<? extends WebService>> mapping
        = new HashMap<Method, Constructor<? extends WebService>>();

    @Override
    public void init() throws ServletException {
        super.init();
        for (Method method: Method.values()) {
            String className = getInitParameter(method.name());
            if (StringUtils.isNotBlank(className)) {
                mapping.put(method, getConstructor(className));
            }
        }

        if (mapping.isEmpty()) {
            throw new ServletException("No supported methods");
        }
    }

    @SuppressWarnings("unchecked")
    private Constructor<? extends WebService> getConstructor(String className)
        throws ServletException {
        Class<? extends WebService> serviceClass;
        try {
            serviceClass = (Class<? extends WebService>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ServletException("Could not find " + className, e);
        }
        try {
            return serviceClass.getConstructor(InterMineAPI.class);
        } catch (SecurityException e) {
            throw new ServletException(e);
        } catch (NoSuchMethodException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        if (mapping.containsKey(method)) {
            try {
                return mapping.get(method).newInstance(api);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new NoServiceException();
    }
}
