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

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.intermine.objectstore.ObjectStore;
import org.intermine.web.logic.Constants;

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
     * @see TilesAction#execute(ComponentContext, ActionMapping, ActionForm, HttpServletRequest,
     *                          HttpServletResponse)
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)  throws Exception {

        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        String model = os.getModel().getPackageName();

        Map classCounts = (Map) servletContext.getAttribute("classCounts");
        Map classDescrs = (Map) servletContext.getAttribute("classDescriptions");
        Map sortedClassDescrs = new TreeMap (classDescrs);
        StringBuffer sb = new StringBuffer();

        for (Iterator it = sortedClassDescrs.keySet().iterator(); it.hasNext();) {
            String helpKey = (String) it.next();
            String helpText = (String) sortedClassDescrs.get(helpKey);
            Integer n = (Integer) classCounts.get(model + "." + helpKey);

            // if this class has objects, add help text to array
            // for javascript to use on display page
            if (helpText != null && n != null && n.intValue() > 0) {
                String escaped = new String();
                escaped = helpText.replaceAll("'", "\\\\'");
                sb.append(new String("'" + escaped + "',"));
            }
        }
        // remove last comma
        sb.deleteCharAt(sb.length() - 1);
        request.setAttribute("helpText", sb);
        return null;
    }
}
