package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.bag.TypeConverter;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.template.TemplateQuery;

/**
 * @author Xavier Watkins
 *
 */
public class ConvertBagController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    public ActionForward execute(@SuppressWarnings("unused") ComponentContext context,
                                 @SuppressWarnings("unused") ActionMapping mapping,
                                 @SuppressWarnings("unused") ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        InterMineBag imBag = (InterMineBag) request.getAttribute("bag");
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Model model = os.getModel();

        Map<Class, TemplateQuery> conversionTypesMap = TypeConverter.getConversionTemplates(
            servletContext, TypeUtil.instantiate(model.getPackageName() + "." + imBag.getType()));
        ArrayList<String> conversionTypes = new ArrayList<String>();
        Map fastaMap = new HashMap();
        for (Class clazz : conversionTypesMap.keySet()) {
            conversionTypes.add(TypeUtil.unqualifiedName(clazz.getName()));
            Type type = (Type) webConfig.getTypes().get(clazz.getName());
            FieldConfig fieldConfig = (FieldConfig) type.getFieldConfigMap().get("length");
            if (fieldConfig != null && fieldConfig.getDisplayer() != null) {
                fastaMap.put(type, true);
            } else {
                fastaMap.put(type, false);
            }
        }

        request.setAttribute("conversionTypes", conversionTypes);
        request.setAttribute("fastaMap", fastaMap);
        return null;
    }

}
