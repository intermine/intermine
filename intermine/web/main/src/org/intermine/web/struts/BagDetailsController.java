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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
import org.intermine.web.logic.config.BagTableDisplayer;
import org.intermine.web.logic.config.GraphDisplayer;
import org.intermine.web.logic.config.Type;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.PagedCollection;
import org.intermine.web.logic.widget.BagGraphWidget;
import org.intermine.web.logic.widget.BagTableWidgetLoader;
import org.intermine.web.logic.widget.DataSetLdr;

/**
 * @author Xavier Watkins
 */
public class BagDetailsController extends TilesAction
{

    /**
     * @see TilesAction#execute(ComponentContext, ActionMapping, ActionForm, HttpServletRequest,
     *      HttpServletResponse)
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response)
                    throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);

        String bagName = request.getParameter("bagName");
        InterMineBag imBag = (InterMineBag) profile.getSavedBags().get(bagName);

        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
        Model model = os.getModel();

        Type type = (Type) webConfig.getTypes().get(model.getPackageName() + "." + imBag.getType());
        Set graphDisplayers = type.getGraphDisplayers();
        ArrayList graphDisplayerArray = new ArrayList();
        for (Iterator iter = graphDisplayers.iterator(); iter.hasNext();) {
            GraphDisplayer graphDisplayer = (GraphDisplayer) iter.next();
            String dataSetLoader = graphDisplayer.getDataSetLoader();
            Class clazz = TypeUtil.instantiate(dataSetLoader);
            Constructor constr = clazz.getConstructor(new Class[]
                {
                    InterMineBag.class, ObjectStore.class
                });
            DataSetLdr dataSetLdr = (DataSetLdr) constr.newInstance(new Object[]
                {
                    imBag, os
                });
            //TODO use caching here
            if (dataSetLdr.getResultsSize() > 0) {
                BagGraphWidget bagGraphWidget = new BagGraphWidget(session,
                                                                   dataSetLdr.getDataSet(),
                                                                   dataSetLdr
                                                                       .getGeneCategoryArray(),
                                                                   bagName, graphDisplayer
                                                                       .getTitle(), graphDisplayer
                                                                       .getDomainLabel(),
                                                                   graphDisplayer.getRangeLabel(),
                                                                   graphDisplayer.getToolTipGen(),
                                                                   graphDisplayer.getUrlGen());
                graphDisplayerArray.add(new String[]
                    {
                        bagGraphWidget.getHTML(), graphDisplayer.getDescription()
                    });
            }
        }

        ArrayList tableDisplayerArray = new ArrayList();
        Set bagTabledisplayers = type.getBagTableDisplayers();
        for (Iterator iter = bagTabledisplayers.iterator(); iter.hasNext();) {
            BagTableDisplayer bagTableDisplayer = (BagTableDisplayer) iter.next();
            String ldrType = bagTableDisplayer.getType();
            String collectionName = bagTableDisplayer.getCollectionName();
            String fields = bagTableDisplayer.getFields();
            String title = bagTableDisplayer.getTitle();
            String description = bagTableDisplayer.getDescription();
            BagTableWidgetLoader bagWidgLdr =
                new BagTableWidgetLoader(title, description, ldrType, collectionName,
                                         imBag, os, webConfig, model,
                                         classKeys, fields);
            tableDisplayerArray.add(bagWidgLdr);
        }

        WebCollection webCollection = new WebCollection(os, imBag.getType(), imBag, model,
                                                        webConfig, classKeys);
        PagedCollection pagedColl = new PagedCollection(webCollection);
        request.setAttribute("bag", imBag);
        request.setAttribute("pagedColl", pagedColl);
        request.setAttribute("graphDisplayerArray", graphDisplayerArray);
        request.setAttribute("tableDisplayerArray", tableDisplayerArray);
        return null;
    }
}
