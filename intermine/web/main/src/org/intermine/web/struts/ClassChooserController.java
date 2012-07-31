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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import org.intermine.api.InterMineAPI;
import org.intermine.api.profile.TagManager;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.session.SessionMethods;
import org.intermine.model.userprofile.Tag;

/**
 * Gets list of help text blurbs for each class, and passes them on to the display page.
 * @author Julie Sullivan
 */
public class ClassChooserController extends TilesAction
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
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);
        ObjectStoreSummary oss = im.getObjectStoreSummary();
        ServletContext servletContext = session.getServletContext();
        Collection<String> qualifiedTypes = im.getModel().getClassNames();
        Map<String, String> classDescrs =
            (Map<String, String>) servletContext.getAttribute("classDescriptions");
        StringBuffer sb = new StringBuffer();
        TagManager tagManager = im.getTagManager();

        List<Tag> preferredBagTypeTags = tagManager.getTags("im:preferredBagType", null,
                "class", im.getProfileManager().getSuperuser());

        ArrayList<String> typeList = new ArrayList();
        ArrayList<String> preferedTypeList = new ArrayList();

        for (Tag tag : preferredBagTypeTags) {
            preferedTypeList.add(TypeUtil.unqualifiedName(tag.getObjectIdentifier()));
        }
        for (String className : qualifiedTypes) {
            String unqualifiedName = TypeUtil.unqualifiedName(className);
            if (oss.getClassCount(className) > 0
                && !"InterMineObject".equalsIgnoreCase(unqualifiedName)) {
                String helpKey = unqualifiedName;
                String helpText = classDescrs.get(helpKey);
                if (helpText != null) {
                    String escaped = helpText.replaceAll("'", "\\\\'");
                    sb.append("'" + helpKey + "': '" + escaped + "', ");
                }
                typeList.add(unqualifiedName);
            }
        }
        Collections.sort(preferedTypeList);
        Collections.sort(typeList);
        request.setAttribute("typeList", typeList);
        request.setAttribute("preferredTypeList", preferedTypeList);
        if (sb.length() >= 2) {
            sb.deleteCharAt(sb.length() - 2);
        }
        request.setAttribute("helpMap", sb);
        return null;
    }
}
