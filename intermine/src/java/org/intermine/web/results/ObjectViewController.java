package org.intermine.web.results;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;

import org.intermine.metadata.Model;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.model.InterMineObject;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;
import org.intermine.objectstore.ObjectStore;

/**
 * Implementation of <strong>TilesAction</strong> that assembles data for displaying an object.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class ObjectViewController extends TilesAction
{
    /**
     * Assembles data for displaying an object.
     *
     * @param context The Tiles ComponentContext
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     *
     * @exception Exception if an error occurs
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
        Model model = (Model) os.getModel();
        Object o = request.getAttribute("object");

        if (o == null) {
            Integer id = new Integer((String) request.getParameter("id"));
            String field = request.getParameter("field");
            o = os.getObjectById(id);

            if (field != null) {
                o = TypeUtil.getFieldValue(o, field);
            }
            context.putAttribute("object", o);
        }

        String viewType = (String) request.getAttribute("viewType");
        
        if (o instanceof InterMineObject) {
            List leafClds = new ArrayList();
            for (Iterator i = DynamicUtil.decomposeClass(o.getClass()).iterator(); i.hasNext();) {
                leafClds.add(model.getClassDescriptorByName(((Class) i.next()).getName()));
            }
            context.putAttribute("leafClds", leafClds);

            if (viewType.equals("summary")) {
                Map primaryKeyFieldsMap = new LinkedHashMap();
                Class c = o.getClass();
                for (Iterator i = PrimaryKeyUtil.getPrimaryKeyFields(model, c).iterator();
                     i.hasNext();) {
                    String fieldName = (String) i.next();
                    primaryKeyFieldsMap.put(fieldName, fieldName);
                }
                context.putAttribute("primaryKeyFields", primaryKeyFieldsMap);
            }
        } else {
            context.putAttribute("leafClds", new ArrayList());
            context.putAttribute("primaryKeyFields", new HashMap());
        }

        return null;
    }
}
