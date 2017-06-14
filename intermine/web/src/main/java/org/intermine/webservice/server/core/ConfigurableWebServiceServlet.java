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
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.webservice.server.WebService;

/**
 * A servlet that can be configured entirely in XML.
 * @author Alex Kalderimis
 *
 */
public class ConfigurableWebServiceServlet extends WebServiceServlet
{

    private static final long serialVersionUID = 1943972842080907136L;

    private Class<? extends WebService> serviceClass = null;
    private Constructor<? extends WebService> constructor = null;
    private final Set<Method> supportedMethods = new HashSet<Method>();

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {
        super.init();
        String className = getInitParameter("service");
        if (StringUtils.isBlank(className)) {
            throw new ServletException("No service name provided");
        }
        try {
            serviceClass = (Class<? extends WebService>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ServletException("Could not find " + className, e);
        }
        try {
            constructor = serviceClass.getConstructor(InterMineAPI.class);
        } catch (SecurityException e) {
            throw new ServletException(e);
        } catch (NoSuchMethodException e) {
            throw new ServletException(e);
        }
        String methods = getInitParameter("methods");
        if (StringUtils.isBlank(methods)) {
            throw new ServletException("No methods");
        }
        for (String method: methods.trim().split(",")) {
            Method m = Method.valueOf(method.trim());
            supportedMethods.add(m);
        }
        if (supportedMethods.isEmpty()) {
            throw new ServletException("No supported methods");
        }
    }

    @Override
    protected WebService getService(Method method) throws NoServiceException {
        if (supportedMethods.contains(method)) {
            try {
                return constructor.newInstance(api);
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
