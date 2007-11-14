package org.intermine.bio.web;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 *
 * @author Kim Rutherford
 */
public class AttributeLinkDisplayerController extends TilesAction
{
    /**
     * @see TilesAction#execute(ComponentContext, ActionMapping, ActionForm, HttpServletRequest,
     *                          HttpServletResponse)
     */
    @Override
    public ActionForward execute(@SuppressWarnings("unused")  ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response) {
        ServletContext servletContext = request.getSession().getServletContext();
        InterMineObject imo = (InterMineObject) request.getAttribute("object");
        Map<String, Map<String, Map<String, Object>>> linkConfigs =
            new HashMap<String, Map<String, Map<String, Object>>>();
        Properties webProperties =
            (Properties) servletContext.getAttribute(Constants.WEB_PROPERTIES);
        Pattern p = Pattern.compile("attributelink\\.(.*)\\.([^.]+)\\.(urlPrefix|text|imageName)");
        for (Map.Entry<Object, Object> entry: webProperties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            Matcher matcher = p.matcher(key);
            if (matcher.matches()) {
                String configKey = matcher.group(1);
                String attrName = matcher.group(2);
                String propType = matcher.group(3);

                Map<String, Map<String, Object>> attrMap;
                if (linkConfigs.containsKey(configKey)) {
                    attrMap = linkConfigs.get(configKey);
                } else {
                    attrMap = new HashMap<String, Map<String, Object>>();
                    linkConfigs.put(configKey, attrMap);
                }
                Map<String, Object> config;
                if (attrMap.containsKey(attrName)) {
                    config = attrMap.get(attrName);
                } else {
                    config = new HashMap<String, Object>();
                    attrMap.put(attrName, config);
                }

                Object attrValue = null;
                if (config.containsKey("attributeValue")) {
                    attrValue = config.get("attributeValue");
                } else {
                    try {
                        attrValue = TypeUtil.getFieldValue(imo, attrName);
                        config.put("attributeValue", attrValue);
                        config.put("valid", Boolean.TRUE);
                    } catch (IllegalAccessException e) {
                        config.put("valid", Boolean.FALSE);
                        config.put("attributeValue", e);
                    }
                }

                if (propType.equals("url")) {
                    if (attrValue != null) {
                        config.put("url", value + attrValue);
                    }
                } else {
                    if (propType.equals("imageName")) {
                        config.put("imageName", value);
                    } else {
                        String text = value.replaceAll("[[attributeValue]]",
                                                       String.valueOf(attrValue));
                        config.put("text", text);
                    }
                }
            }
        }
        request.setAttribute("attributeLinkConfiguration", linkConfigs);
        String className = DynamicUtil.decomposeClass(imo.getClass()).iterator().next().getName();
        request.setAttribute("attributeLinkClassName", TypeUtil.unqualifiedName(className));
        return null;
    }
}
