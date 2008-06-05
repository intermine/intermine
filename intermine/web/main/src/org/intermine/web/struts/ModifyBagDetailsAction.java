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

import java.util.Map;

import org.intermine.metadata.Model;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreWriter;
import org.intermine.objectstore.intermine.ObjectStoreWriterInterMineImpl;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.bag.BagConverter;
import org.intermine.web.logic.bag.BagHelper;
import org.intermine.web.logic.bag.BagQueryConfig;
import org.intermine.web.logic.bag.BagQueryRunner;
import org.intermine.web.logic.bag.InterMineBag;
import org.intermine.web.logic.bag.TypeConverter;
import org.intermine.web.logic.config.WebConfig;
import org.intermine.web.logic.profile.Profile;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.query.PathQuery;
import org.intermine.web.logic.results.PagedTable;
import org.intermine.web.logic.results.WebResults;
import org.intermine.web.logic.search.SearchRepository;
import org.intermine.web.logic.session.SessionMethods;

import java.lang.reflect.Constructor;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

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
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        ModifyBagDetailsForm mbdf = (ModifyBagDetailsForm) form;
        SearchRepository globalRepository =
            (SearchRepository) servletContext.getAttribute(Constants.
                                                           GLOBAL_SEARCH_REPOSITORY);

        if (request.getParameter("remove") != null) {
            removeFromBag(mbdf.getBagName(), profile, mbdf, pm.getUserProfileObjectStore(), os,
                    session);
        } else if (request.getParameter("showInResultsTable") != null) {
            return showBagInResultsTable(mbdf.getBagName(), mapping, session);
        } else if (request.getParameter("convertToThing") != null) {
            InterMineBag imBag = BagHelper.getBag(profile, globalRepository,
                                                  mbdf.getBagName());
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
        } else if (request.getParameter("useBagInQuery") != null) {
                InterMineBag imBag = BagHelper.getBag(profile, globalRepository,
                                                      mbdf.getBagName());
                if (imBag == null) {
                    return mapping.findForward("errors");
                }

                String identifier = "bag." + imBag.getName();
                PagedTable pc = SessionMethods.getResultsTable(session, identifier);
                PathQuery pathQuery = pc.getWebTable().getPathQuery().clone();

                session.setAttribute(Constants.QUERY, pathQuery);
                session.setAttribute("path", imBag.getType());
                session.setAttribute("prefix", imBag.getType());

                return mapping.findForward("query");
        } else if (request.getParameter("convert") != null
                        && request.getParameter("bagName") != null) {
            String type2 = request.getParameter("convert");
            Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
            WebConfig webConfig = (WebConfig) servletContext.getAttribute(Constants.WEBCONFIG);
            InterMineBag imBag = BagHelper.getBag(profile, globalRepository,
                request.getParameter("bagName"));
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

    /**
     * remove the selected elements from the bag
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @exception Exception if the application business logic throws
     *  an exception
     */
    private void removeFromBag(String bagName, Profile profile, ModifyBagDetailsForm mbdf,
                               @SuppressWarnings("unused") ObjectStoreWriter uosw,
                               ObjectStore os, HttpSession session) throws Exception {
        Map savedBags = profile.getSavedBags();
        InterMineBag interMineBag = (InterMineBag) savedBags.get(bagName);
        ObjectStoreWriter osw = null;
        try {
            osw = new ObjectStoreWriterInterMineImpl(os);
            for (int i = 0; i < mbdf.getSelectedElements().length; i++) {
                osw
                    .removeFromBag(interMineBag.getOsb(),
                                   new Integer(mbdf.getSelectedElements()[i]));
            }
        } finally {
            if (osw != null) {
                osw.close();
            }
        }
        SessionMethods.invalidateBagTable(session, bagName);
    }

    private ActionForward showBagInResultsTable(String bagName, ActionMapping mapping,
                                                @SuppressWarnings("unused") HttpSession session) {
        return new ForwardParameters(mapping.findForward("bagResultsTable"))
            .addParameter("bagName", bagName)
            .addParameter("trail", "|bag." + bagName).forward();
    }
}
