package org.intermine.bio.web.logic;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.InvocationTargetException;

import javax.servlet.http.HttpServletRequest;

import org.apache.tools.ant.BuildException;
import org.intermine.web.logic.session.SessionMethods;

/**
 * This utility class instance a GenomicRegionSearchService object based on mine's setting.
 *
 * @author Fengyuan Hu
 */
public final class GenomicRegionSearchUtil
{
    private GenomicRegionSearchUtil() {

    }

    /**
     * Generate GenomicRegionSearchService object by using Java reflection
     *
     * @param request HttpServletRequest
     * @return the current mine's GenomicRegionSearchService object
     */
    public static GenomicRegionSearchService getGenomicRegionSearchService(
            HttpServletRequest request) {

        // Get service class name from web.properties
        String serviceClassName = (String) SessionMethods.getWebProperties(
                request.getSession().getServletContext()).get(
                "genomicRegionSearch.service");

        GenomicRegionSearchService grsService = null;
        if (serviceClassName == null || "".equals(serviceClassName)) {
            grsService = new GenomicRegionSearchService();
            grsService.init(request);
        } else { // reflection
            Class<?> serviceClass;
            try {
                serviceClass = Class.forName(serviceClassName);
            } catch (ClassNotFoundException e) {
                throw new BuildException("Class not found for " + serviceClassName, e);
            }
            Class<?> [] types = new Class[] {HttpServletRequest.class};
            Object [] args = new Object[] {request};
            try {
                grsService = (GenomicRegionSearchService) serviceClass
                        .getConstructor(types).newInstance(args);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return grsService;
    }
}
