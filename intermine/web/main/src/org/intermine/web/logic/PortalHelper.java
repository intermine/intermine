package org.intermine.web.logic;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.intermine.api.InterMineAPI;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.config.WebConfig;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


/**
 * Util methods for the portal
 * @author Julie Sullivan
 **/
public class PortalHelper 
{

    private static Map<String, BagConverter> bagConverters = new HashMap();
    
    public static String getAdditionalParameter(String param, String[] paramArray) {

        String[] urlFields = paramArray[0].split(",");
        for (String urlField : urlFields) {
            // if one of the request vars matches the variables listed in the bagquery
            // config, add the variable to be passed to the custom converter
            
            if (urlField.equals(param)) {
                // the spaces in organisms, eg. D.%20rerio, need to be handled
                return param;
            }
        }
        return null;
    }
    
    public static String getAdditionalParameter(HttpServletRequest request, String[] paramArray)
    throws UnsupportedEncodingException {

        String[] urlFields = paramArray[0].split(",");
        String addparameter = null;
        for (String urlField : urlFields) {
            // if one of the request vars matches the variables listed in the bagquery
            // config, add the variable to be passed to the custom converter
            String param = request.getParameter(urlField);
            if (StringUtils.isNotEmpty(param)) {
                // the spaces in organisms, eg. D.%20rerio, need to be handled
                addparameter = URLDecoder.decode(param, "UTF-8");
            }
        }
        return addparameter;
    }
    
    public BagConverter getBagConverter(InterMineAPI im, WebConfig webConfig,
            String converterClassName) {

        BagConverter bagConverter = bagConverters.get(converterClassName);

        if (bagConverter == null) {
            try {
                Class clazz = Class.forName(converterClassName);
                Constructor constructor = clazz.getConstructor(InterMineAPI.class, WebConfig.class);
                bagConverter = (BagConverter) constructor.newInstance(im, webConfig);
            } catch (Exception e) {
                throw new RuntimeException("Failed to construct bagconverter for "
                        + converterClassName, e);
            }
            bagConverters.put(converterClassName, bagConverter);
        }
        return bagConverter;
    }
    
}
