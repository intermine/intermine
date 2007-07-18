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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.intermine.model.userprofile.Tag;
import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreSummary;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.ClassKeyHelper;
import org.intermine.web.logic.Constants;
import org.intermine.web.logic.profile.ProfileManager;
import org.intermine.web.logic.session.SessionMethods;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;


/**
 * Gets list of help text blurbs for each class, and passes them on to the display page.
 * @author Julie Sullivan
 */
public class ClassChooserController extends TilesAction
{
    /**
     * {@inheritDoc}
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) 
        throws Exception {

        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        ObjectStoreSummary oss =
            (ObjectStoreSummary) servletContext.getAttribute(Constants.OBJECT_STORE_SUMMARY);
        String model = os.getModel().getPackageName();
        ProfileManager pm = SessionMethods.getProfileManager(servletContext);
        
        Collection qualifiedTypes = os.getModel().getClassNames();
        Map classDescrs = (Map) servletContext.getAttribute("classDescriptions");
        StringBuffer sb = new StringBuffer();

        String superUserName = (String) servletContext.getAttribute(Constants.SUPERUSER_ACCOUNT);

        List preferredBagTypeTags = pm.getTags("im:preferredBagType", null, "class", superUserName);
        
        ArrayList typeList = new ArrayList();
        ArrayList preferedTypeList = new ArrayList();
        
        // loop through preferred list, add classes to map
        for (Iterator iter = preferredBagTypeTags.iterator(); iter.hasNext();) {
            Tag tag = (Tag) iter.next();
            preferedTypeList.add(TypeUtil.unqualifiedName(tag.getObjectIdentifier()));
        }
        
        // loop through all classes, add to map if not one of preferred classes
        Map classKeys = (Map) servletContext.getAttribute(Constants.CLASS_KEYS);
        for (Iterator iter = qualifiedTypes.iterator(); iter.hasNext();) {
           
            String className = (String) iter.next();
            String unqualifiedName = TypeUtil.unqualifiedName(className);

            if (ClassKeyHelper.hasKeyFields(classKeys, unqualifiedName)
                            && oss.getClassCount(className) > 0) {
             
                String helpKey = (String) unqualifiedName;
                String helpText = (String) classDescrs.get(helpKey);
                
                // add to help map.  always 
                if (helpText != null) {                                 
                    String escaped = helpText.replaceAll("'", "\\\\'");
                    sb.append("'" + helpKey + "': '" + escaped + "', ");
                }
                // add to map if not on preferred map
                if (ClassKeyHelper.hasKeyFields(classKeys, unqualifiedName)
                                && oss.getClassCount(className) > 0
                                && !preferedTypeList.contains(unqualifiedName)) {

                    typeList.add(unqualifiedName);

                }
            }
        }


        Collections.sort(preferedTypeList);
        Collections.sort(typeList);
        request.setAttribute("typeList", typeList);
        request.setAttribute("preferredTypeList", preferedTypeList);

        sb.deleteCharAt(sb.length() - 2);    
        request.setAttribute("helpMap", sb);
        return null;
    }
}
