package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.bag.BagManager;
import org.intermine.api.bag.BagQueryConfig;
import org.intermine.api.profile.InterMineBag;
import org.intermine.api.profile.Profile;
import org.intermine.api.results.WebResults;
import org.intermine.api.template.TemplateManager;
import org.intermine.metadata.Model;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.PortalHelper;
import org.intermine.web.logic.bag.BagConversionHelper;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.session.SessionMethods;

/**
 * @author Xavier Watkins
 *
 */
public class ModifyBagDetailsAction extends InterMineAction
{
    private static int index = 0;

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
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 @SuppressWarnings("unused") HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        Profile profile = SessionMethods.getProfile(session);

        Model model = im.getModel();
        ModifyBagDetailsForm mbdf = (ModifyBagDetailsForm) form;
        BagManager bagManager = im.getBagManager();

        InterMineBag imBag = bagManager.getUserOrGlobalBag(profile, mbdf.getBagName());
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
            InterMineBag newBag = bagManager.getUserOrGlobalBag(profile,
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
            Map<String, String []> additionalConverters
                = bagQueryConfig.getAdditionalConverters(imBag.getType());
            if (additionalConverters != null) {
                for (String converterClassName : additionalConverters.keySet()) {
                    BagConverter bagConverter = PortalHelper.getBagConverter(im,
                            SessionMethods.getWebConfig(request), converterClassName);
                    WebResults result = bagConverter.getConvertedObjects(profile,
                            imBag.getContentsAsIds(), imBag.getType(), mbdf.getExtraFieldValue());
                    PagedTable pc = new PagedTable(result);
                    String identifier = "col" + index++;
                    SessionMethods.setResultsTable(session, identifier, pc);
                    String trail = "|bag." + imBag.getName();
                    SessionMethods.removeQuery(session);
                    return new ForwardParameters(mapping.findForward("results"))
                        .addParameter("table", identifier).addParameter("trail", trail).forward();
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
            WebResults webResults = BagConversionHelper.getConvertedObjects(session,
                    templateManager.getConversionTemplates(),
                    TypeUtil.instantiate(model.getPackageName() + "." + imBag.getType()),
                    TypeUtil.instantiate(model.getPackageName() + "." + type2), imBag);
            PagedTable pc = new PagedTable(webResults);
            String identifier = "bagconvert." + imBag.getName() + "." + type2;

            SessionMethods.setResultsTable(session, identifier, pc);
            String trail = "|bag." + imBag.getName();
            SessionMethods.removeQuery(session);
            return new ForwardParameters(mapping.findForward("results"))
                            .addParameter("table", identifier)
                            .addParameter("size", "25")
                            .addParameter("trail", trail).forward();

        }
        return new ForwardParameters(mapping.findForward("bagDetails"))
                    .addParameter("bagName", mbdf.getBagName()).forward();
    }
}
