package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.lang.reflect.Constructor;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.pathquery.PathQuery;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.bag.TypeConverter;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.search.SearchRepository;
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
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        ModifyBagDetailsForm mbdf = (ModifyBagDetailsForm) form;
        SearchRepository globalRepository =
            (SearchRepository) servletContext.getAttribute(Constants.GLOBAL_SEARCH_REPOSITORY);
        InterMineBag imBag = BagHelper.getBag(profile, globalRepository, mbdf.getBagName());
        String bagIdentifier = "bag." + imBag.getName();

        if (request.getParameter("removeFromBag") != null) {
            PagedTable pc = SessionMethods.getResultsTable(session, bagIdentifier);
            String msg = "";
            int n = imBag.getSize();
            if (pc.isAllSelected()) {
                // TODO these messages need to be moved to properties file
                msg = "You can't remove all items from your list.  Try deleting your list instead.";
            } else {
                pc.removeFromBag(mbdf.getBagName(), profile, os, session);
                int removed = n - imBag.size();
                msg = "You have removed " + removed + " items from your list.";
            }
            SessionMethods.recordMessage(msg, session);
        } else if (request.getParameter("addToBag") != null) {
                PagedTable pc = SessionMethods.getResultsTable(session, bagIdentifier);
                InterMineBag newBag = BagHelper.getBag(profile, globalRepository,
                                                       mbdf.getExistingBagName());
                int size = pc.getCurrentSelectedIdStringsList().size();
                pc.addSelectedToBag(os, newBag);
                String msg = "You have added " + size + " items from list <strong>"
                + imBag.getName() + "</strong> to list <strong>" + newBag.getName() + "</strong>";
                SessionMethods.recordMessage(msg, session);
        // orthologues form
        } else if (request.getParameter("convertToThing") != null) {
            BagQueryConfig bagQueryConfig =
                (BagQueryConfig) servletContext.getAttribute(Constants.BAG_QUERY_CONFIG);
            Map<String, String []> additionalConverters
            = bagQueryConfig.getAdditionalConverters(imBag.getType());
            if (additionalConverters != null) {
                for (String converterClassName : additionalConverters.keySet()) {
                    Class clazz = Class.forName(converterClassName);
                    Constructor constructor = clazz.getConstructor();
                    BagConverter bagConverter = (BagConverter) constructor.newInstance();
                    WebResults result =
                        bagConverter.getConvertedObjects(session, mbdf.getExtraFieldValue(),
                                                         imBag.getContentsAsIds(),
                                                         imBag.getType());

                    PagedTable pc = new PagedTable(result);
                    String identifier = "col" + index++;
                    SessionMethods.setResultsTable(session, identifier, pc);
                    String trail = "|bag." + imBag.getName();
                    session.removeAttribute(Constants.QUERY);
                    return new ForwardParameters(mapping.findForward("results"))
                    .addParameter("table", identifier)
                    .addParameter("trail", trail).forward();
                }
            }
        // "use in bag" link
        } else if (request.getParameter("useBag") != null) {
            PagedTable pc = SessionMethods.getResultsTable(session, bagIdentifier);
            PathQuery pathQuery = pc.getWebTable().getPathQuery().clone();
            session.setAttribute(Constants.QUERY, pathQuery);
            session.setAttribute("path", imBag.getType());
            session.setAttribute("prefix", imBag.getType());
            String msg = "You can now create a query using your list " + imBag.getName();
            SessionMethods.recordMessage(msg, session);
            return mapping.findForward("query");
        // convert links
        } else if (request.getParameter("convert") != null
                        && request.getParameter("bagName") != null) {
            String type2 = request.getParameter("convert");
            Model model = os.getModel();
            WebResults webResults = TypeConverter.getConvertedObjects(session, servletContext,
                BagQueryRunner.getConversionTemplates(servletContext),
                TypeUtil.instantiate(model.getPackageName() + "." + imBag.getType()),
                TypeUtil.instantiate(model.getPackageName() + "." + type2),
                imBag);
            PagedTable pc = new PagedTable(webResults);
            String identifier = "bagconvert." + imBag.getName() + "." + type2;

            SessionMethods.setResultsTable(session, identifier, pc);
            String trail = "|bag." + imBag.getName();
            session.removeAttribute(Constants.QUERY);
            return new ForwardParameters(mapping.findForward("results"))
                            .addParameter("table", identifier)
                            .addParameter("size", "25")
                            .addParameter("trail", trail).forward();

        }
        return new ForwardParameters(mapping.findForward("bagDetails"))
                    .addParameter("bagName", mbdf.getBagName()).forward();
    }
}
