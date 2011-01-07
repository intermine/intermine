package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.Profile;
import org.intermine.api.profile.TagManager;
import org.intermine.api.search.Scope;
import org.intermine.api.tag.TagNames;
import org.intermine.api.template.TemplateManager;
import org.intermine.api.template.TemplateQuery;
import org.intermine.api.tracker.TemplateTracker;
import org.intermine.api.tracker.TrackerDelegate;
import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.util.PropertiesUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.aspects.Aspect;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Display the query builder (if there is a curernt query) or redirect to project.sitePrefix.
 *
 * @author Tom Riley
 */
public class BeginAction extends InterMineAction
{
    private static final Logger LOG = Logger.getLogger(TemplateTracker.class);
    private static final Integer MAX_TEMPLATES = new Integer(8);

    /**
     * @LinkedHashMap stores tabs of popular templates on the homepage
     */
    private static LinkedHashMap<String, HashMap<String, Object>> bagOfTabs;

     /**
     * Either display the query builder or redirect to project.sitePrefix.
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ServletContext servletContext = session.getServletContext();

        Properties properties = SessionMethods.getWebProperties(servletContext);

        // If GALAXY_URL is sent from a Galaxy server, then save it in the session; if not, read
        // the default value from web.properties and save it in the session
        if (request.getParameter("GALAXY_URL") != null) {
            request.getSession().setAttribute("GALAXY_URL",
                    request.getParameter("GALAXY_URL"));
            String msg = properties.getProperty("galaxy.welcomeMessage");
            SessionMethods.recordMessage(msg, session);
        } else {
            request.getSession().setAttribute(
                    "GALAXY_URL",
                    properties.getProperty("galaxy.baseurl.default")
                            + properties.getProperty("galaxy.url.value"));
        }

        /* count number of templates and bags */
        request.setAttribute("bagCount", new Integer(im.getBagManager()
                .getGlobalBags().size()));
        request.setAttribute("templateCount", new Integer(im
                .getTemplateManager().getGlobalTemplates().size()));

        /*most popular template*/
        TrackerDelegate trackerDelegate = im.getTrackerDelegate();
        if (trackerDelegate != null) {
            trackerDelegate.setTemplateManager(im.getTemplateManager());
            String templateName = trackerDelegate.getMostPopularTemplate();
            if (templateName != null) {
                Profile profile = SessionMethods.getProfile(session);
                TemplateQuery template = im.getTemplateManager()
                                         .getTemplate(profile, templateName, Scope.ALL);
                if (template != null) {
                    request.setAttribute("mostPopularTemplate", template.getTitle());
                } else {
                    LOG.error("The most popular template " + templateName + "is not public");
                }
            }
        }

        List<TemplateQuery> templates = null;
        TemplateManager templateManager = im.getTemplateManager();
        List<String> mostPopularTemplateNames;

        Object beginQueryClassConfig = properties.get("begin.query.classes");
        if (beginQueryClassConfig != null) {
            String[] beginQueryClasses = beginQueryClassConfig.toString().split("[ ,]+");
            request.setAttribute("beginQueryClasses", beginQueryClasses);
        }

        // do we have popular templates cached?
        if (bagOfTabs == null) {
            // ...get begin/homepage popular templates in tabs
            Properties props = PropertiesUtil.getPropertiesStartingWith("begin.tabs", properties);
            if (props.size() != 0) {
                props = PropertiesUtil.stripStart("begin.tabs", props);
                int i = 1;
                // init
                bagOfTabs = new LinkedHashMap<String, HashMap<String, Object>>();
                while (true) {
                    if (props.containsKey(i + ".id")) {
                        LinkedHashMap<String, Object> tab = new LinkedHashMap<String, Object>();
                        String identifier;

                        // identifier, has to be present
                        identifier = (String) props.get(i + ".id"); tab.put("identifier", identifier);
                        // (optional) description
                        tab.put("description", props.containsKey(i + ".description") ? (String) props.get(i + ".description") : "");
                        // (optional) custom name, otherwise use identifier
                        tab.put("name", props.containsKey(i + ".name") ? (String) props.get(i + ".name") : identifier);

                        // fetch the actual template queries
                        templates = templateManager.getAspectTemplates(TagNames.IM_ASPECT_PREFIX + identifier, null);
                        if (SessionMethods.getProfile(session).isLoggedIn()) {
                            mostPopularTemplateNames = trackerDelegate.getMostPopularTemplateOrder(SessionMethods.getProfile(session), session.getId());
                        } else {
                            mostPopularTemplateNames = trackerDelegate.getMostPopularTemplateOrder();
                        }

                        if (mostPopularTemplateNames != null) {
                            Collections.sort(templates, new MostPopularTemplateComparator(mostPopularTemplateNames));
                        }

                        if (templates.size() > MAX_TEMPLATES) {
                            templates = templates.subList(0, MAX_TEMPLATES);
                        }

                        tab.put("templates", templates);

                        bagOfTabs.put(Integer.toString(i), tab);
                        i++;
                    } else {
                        break;
                    }
                }
            }
        }
        request.setAttribute("tabs", bagOfTabs);

        // preferred bags (Gucci)
        ArrayList<String> preferredBags = new ArrayList<String>();
        TagManager tagManager = im.getTagManager();
        List<Tag> preferredBagTypeTags = tagManager.getTags("im:preferredBagType", null, "class", im.getProfileManager().getSuperuser());
        for (Tag tag : preferredBagTypeTags) {
            preferredBags.add(TypeUtil.unqualifiedName(tag.getObjectIdentifier()));
        }
        request.setAttribute("preferredBags", preferredBags);

        return mapping.findForward("begin");
    }

    private class MostPopularTemplateComparator implements Comparator<TemplateQuery>
    {
        private List<String> mostPopularTemplateNames;

        public MostPopularTemplateComparator(List<String> mostPopularTemplateNames) {
            this.mostPopularTemplateNames = mostPopularTemplateNames;
        }
        public int compare(TemplateQuery template1, TemplateQuery template2) {
            String templateName1 = template1.getName();
            String templateName2 = template2.getName();
            if (!mostPopularTemplateNames.contains(templateName1)
                && !mostPopularTemplateNames.contains(templateName2)) {
                if (template1.getTitle().equals(template2.getTitle())) {
                    return template1.getName().compareTo(template2.getName());
                } else {
                    return template1.getTitle().compareTo(template2.getTitle());
                }
            }
            if (!mostPopularTemplateNames.contains(templateName1)) {
                return +1;
            }
            if (!mostPopularTemplateNames.contains(templateName2)) {
                return -1;
            }
            return (mostPopularTemplateNames.indexOf(templateName1)
                   < mostPopularTemplateNames.indexOf(templateName2)) ? -1 : 1;
        }
    }
}
