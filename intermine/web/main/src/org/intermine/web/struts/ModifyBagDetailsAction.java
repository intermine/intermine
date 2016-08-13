package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.AdditionalConverter;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.util.NameUtil;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.pathquery.PathQuery;
import org.intermine.metadata.TypeUtil;
import org.intermine.model.InterMineObject;
import org.intermine.web.logic.PortalHelper;
import org.intermine.web.logic.bag.BagConversionHelper;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * @author Xavier Watkins
 *
 */
@SuppressWarnings("deprecation")
public class ModifyBagDetailsAction extends InterMineAction
{

    /**
     * Forward to the correct method based on the button pressed
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);

        Model model = im.getModel();
        ModifyBagDetailsForm mbdf = (ModifyBagDetailsForm) form;
        BagManager bagManager = im.getBagManager();

        InterMineBag imBag = bagManager.getBag(profile, mbdf.getBagName());
        String bagIdentifier = "bag." + imBag.getName();

        if (request.getParameter("removeFromBag") != null) {
            PagedTable pc = SessionMethods.getResultsTable(session, bagIdentifier);
            String msg = "";

            if (pc.isAllRowsSelected()) {
                // TODO these messages need to be moved to properties file
                msg = "You can't remove all items from your list.  Try deleting your list instead.";
            } else {
                int removed = pc.removeSelectedFromBag(imBag, session);
                msg = "You have removed " + removed + " items from your list.";
            }
            SessionMethods.recordMessage(msg, session);
            //return new ForwardParameters(mapping.findForward("bagDetails"))
            //.addParameter("bagName", mbdf.getBagName()).forward();

            // pass an extra parameter telling the JSP to open up the results table
            return new ForwardParameters(mapping.findForward("bagDetails"))
                .addParameter("bagName", mbdf.getBagName())
                .addParameter("table", "open").forward();

        } else if (request.getParameter("addToBag") != null) {
            InterMineBag newBag = bagManager.getBag(profile,
                    mbdf.getExistingBagName());
            String msg = "";
            if (newBag.getType().equals(imBag.getType())) {
                PagedTable pc = SessionMethods.getResultsTable(session, bagIdentifier);
                int oldSize = newBag.size();
                pc.addSelectedToBag(newBag);
                int newSize = newBag.size();
                int added = newSize - oldSize;
                msg = "You have added " + added + " items from list <strong>" + imBag.getName()
                    + "</strong> to list <strong>" + newBag.getName() + "</strong>";
            } else {
                msg = "You can only add objects to other lists of the same type";
            }
            SessionMethods.recordMessage(msg, session);
        // orthologues form
        } else if (request.getParameter("convertToThing") != null) {
            BagQueryConfig bagQueryConfig = im.getBagQueryConfig();
            Set<AdditionalConverter> additionalConverters
                = bagQueryConfig.getAdditionalConverters(imBag.getType());
            if (additionalConverters != null && !additionalConverters.isEmpty()) {
                for (AdditionalConverter additionalConverter : additionalConverters) {
                    BagConverter bagConverter = PortalHelper.getBagConverter(im,
                            SessionMethods.getWebConfig(request),
                            additionalConverter.getClassName());
                    List<Integer> converted = bagConverter.getConvertedObjectIds(profile,
                            imBag.getType(), imBag.getContentsAsIds(), mbdf.getExtraFieldValue());

                    if (converted.size() == 1) {
                        return goToReport(mapping, converted.get(0).toString());
                    }

                    String bagName = NameUtil.generateNewName(profile.getSavedBags().keySet(),
                            mbdf.getExtraFieldValue() + " orthologues of " + imBag.getName());

                    InterMineBag newBag = profile.createBag(bagName, imBag.getType(), "",
                            im.getClassKeys());
                    return createBagAndGoToBagDetails(mapping, newBag, converted);
                }
            }

            // "use in bag" link
        } else if (request.getParameter("useBag") != null) {
            PagedTable pc = SessionMethods.getResultsTable(session, bagIdentifier);
            PathQuery pathQuery = pc.getWebTable().getPathQuery().clone();
            SessionMethods.setQuery(session, pathQuery);
            session.setAttribute("path", imBag.getType());
            session.setAttribute("prefix", imBag.getType());
            String msg = "You can now create a query using your list " + imBag.getName();
            SessionMethods.recordMessage(msg, session);
            return mapping.findForward("query");

        // convert links
        } else if (request.getParameter("convert") != null
                        && request.getParameter("bagName") != null) {
            String type2 = request.getParameter("convert");
            TemplateManager templateManager = im.getTemplateManager();
            @SuppressWarnings("unchecked")
            Class<? extends InterMineObject> classA = (Class<? extends InterMineObject>)
                    TypeUtil.instantiate(model.getPackageName() + "." + imBag.getType());
            @SuppressWarnings("unchecked")
            Class<? extends InterMineObject> classB = (Class<? extends InterMineObject>)
                    TypeUtil.instantiate(model.getPackageName() + "." + type2);
            PathQuery q = BagConversionHelper.getConvertedObjects(session,
                    templateManager.getConversionTemplates(), classA, classB, imBag);
            q.setTitle(type2 + "s from list '" + imBag.getName() + "'");
            SessionMethods.loadQuery(q, session, response);
            final String trail = "|bag." + imBag.getName();
            return new ForwardParameters(mapping.findForward("results"))
                               .addParameter("trail", trail)
                               .forward();
        }
        return new ForwardParameters(mapping.findForward("bagDetails"))
                    .addParameter("bagName", mbdf.getBagName()).forward();
    }

    private ActionForward createBagAndGoToBagDetails(ActionMapping mapping, InterMineBag imBag,
            List<Integer> bagList) throws ObjectStoreException {
        imBag.addIdsToBag(bagList, imBag.getType());
        return new ForwardParameters(mapping.findForward("bagDetails"))
            .addParameter("bagName", imBag.getName()).forward();
    }

    private ActionForward goToReport(ActionMapping mapping, String id) {
        return new ForwardParameters(mapping.findForward("report"))
            .addParameter("id", id).forward();
    }
}
