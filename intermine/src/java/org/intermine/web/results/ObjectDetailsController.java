package org.intermine.web.results;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;

import org.intermine.metadata.Model;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.ObjectStoreFactory;
import org.intermine.objectstore.ObjectStore;
import org.intermine.util.DynamicUtil;
import org.intermine.util.TypeUtil;

/**
 * Implementation of <strong>TilesAction</strong>. Assembles data for
 * an object details view.
 *
 * @author Andrew Varley
 */
public class ObjectDetailsController extends TilesAction
{
    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it).
     * Return an <code>ActionForward</code> instance describing where and how
     * control should be forwarded, or <code>null</code> if the response has
     * already been completed.
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
        Integer id = new Integer((String) request.getParameter("id"));

        ObjectStore os = ObjectStoreFactory.getObjectStore();

        InterMineObject o = os.getObjectById(id);
        
        String field = request.getParameter("field");
        if (field != null) {
            o = (InterMineObject) TypeUtil.getFieldValue(o, field);
        }
        if (o == null) {
            return null;
        }
        
        context.putAttribute("object", o);

        Model model = os.getModel();
        Set leafClds = new HashSet();
        for (Iterator i = DynamicUtil.decomposeClass(o.getClass()).iterator(); i.hasNext();) {
            leafClds.add(model.getClassDescriptorByName(((Class) i.next()).getName()));
        }
        context.putAttribute("leafClds", leafClds);
        
        return null;
    }
}
