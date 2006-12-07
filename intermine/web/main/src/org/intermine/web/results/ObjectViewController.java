package org.intermine.web.results;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.intermine.metadata.FieldDescriptor;
import org.intermine.metadata.Model;
import org.intermine.metadata.PrimaryKeyUtil;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.TypeUtil;
import org.intermine.web.Constants;

import org.apache.log4j.Logger;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * Implementation of <strong>TilesAction</strong> that assembles data for displaying an object.
 *
 * @author Andrew Varley
 * @author Kim Rutherford
 */
public class ObjectViewController extends TilesAction
{
    private static final Logger LOG = Logger.getLogger(ObjectViewController.class);
    
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
/*        
        if (o == null) {
            String objectId = (String) request.getParameter("id");
            if (objectId != null) {
                o = os.getObjectById(new Integer(objectId));
                String field = request.getParameter("field");
                if (field != null) {
                    o = TypeUtil.getFieldValue(o, field);
                }
            }
        }
        
        if (o == null) {
            //this wouldn't be necessary if objectdetails was tidier
            session.removeAttribute("object");
        } else {
            context.putAttribute("object", o);
        }

        Set leafClds = null;

        if (o instanceof InterMineObject) {
            if ("summary".equals((String) request.getAttribute("viewType"))) {
                Map primaryKeyFields = new LinkedHashMap();
                for (Iterator i = PrimaryKeyUtil.getPrimaryKeyFields(model,
                                                                     o.getClass()).iterator();
                     i.hasNext();) {
                    FieldDescriptor fd = (FieldDescriptor) i.next();
                    if (fd.isAttribute()) {
                        primaryKeyFields.put(fd.getName(), fd.getName());
                    }
                }
                context.putAttribute("primaryKeyFields", primaryKeyFields);
            }
        }
*/
        return null;
    }
}
