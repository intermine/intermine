package org.intermine.web.history;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.util.TypeUtil;
import org.intermine.web.ClassKeyHelper;
import org.intermine.web.Constants;
import org.intermine.web.Profile;
import org.intermine.web.ProfileManager;
import org.intermine.web.SessionMethods;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Tiles controller for history tile (page).
 * 
 * @author Thomas Riley
 */
public class MyMineController extends TilesAction
{
    /**
     * 
     * @see TilesAction#execute
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = SessionMethods.getProfileManager(servletContext);
        String page = request.getParameter("page");

        
        if (!StringUtils.isEmpty(page)) {
            session.setAttribute(Constants.MYMINE_PAGE, page);
        }

        if (page != null) {
            if (page.equals("templates")) {
                // prime the tags cache so that the templates tags will be quick to access
                String userName = ((Profile) session.getAttribute(Constants.PROFILE)).getUsername();
                if (userName != null) {
                    // discard result
                    pm.getTags(null, null, "template", userName);
                }
            }
        }
        
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        ObjectStoreSummary oss = (ObjectStoreSummary) servletContext.
        getAttribute(Constants.OBJECT_STORE_SUMMARY);
        Collection qualifiedTypes = os.getModel().getClassNames();
        ArrayList typeList = new ArrayList();
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        for (Iterator iter = qualifiedTypes.iterator(); iter.hasNext();) {
            String className = (String) iter.next();
            String unqualifiedName = TypeUtil.unqualifiedName(className);
            if (ClassKeyHelper.hasKeyFields(classKeys, unqualifiedName)
                && oss.getClassCount(className) > 0) {
                typeList.add(unqualifiedName);
            }
        }
        Collections.sort(typeList);
        request.setAttribute("typeList", typeList);

        return null;
    }
}
