package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.AdditionalConverter;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.bag.TypeConverter;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.template.ApiTemplate;
import org.intermine.api.template.TemplateManager;
import org.intermine.metadata.Model;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.config.FieldConfig;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.session.SessionMethods;

/**
 * @author Xavier Watkins
 *
 */
public class ConvertBagController extends TilesAction
{

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        InterMineBag imBag = (InterMineBag) request.getAttribute("bag");
        WebConfig webConfig = SessionMethods.getWebConfig(request);
        Model model = im.getModel();
        TemplateManager templateManager = im.getTemplateManager();

        Map<Class, ApiTemplate> conversionTypesMap = TypeConverter.getConversionTemplates(
            templateManager.getConversionTemplates(),
            TypeUtil.instantiate(model.getPackageName() + "." + imBag.getType()));
        ArrayList<String> conversionTypes = new ArrayList<String>();
        Map<Type, Boolean> fastaMap = new HashMap<Type, Boolean>();
        for (Class<?> clazz : conversionTypesMap.keySet()) {
            conversionTypes.add(TypeUtil.unqualifiedName(clazz.getName()));
            Type type = webConfig.getTypes().get(clazz.getName());
            FieldConfig fieldConfig = type.getFieldConfigMap().get("length");
            if (fieldConfig != null && fieldConfig.getDisplayer() != null) {
                fastaMap.put(type, Boolean.TRUE);
            } else {
                fastaMap.put(type, Boolean.FALSE);
            }
        }
        // Use custom converters
        BagQueryConfig bagQueryConfig = im.getBagQueryConfig();
        String bagType = imBag.getType();
        Set<AdditionalConverter> additionalConverters
            = bagQueryConfig.getAdditionalConverters(bagType);
        request.setAttribute("customConverters", additionalConverters);
        request.setAttribute("conversionTypes", conversionTypes);
        request.setAttribute("fastaMap", fastaMap);
        return null;
    }

}
