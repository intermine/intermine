package org.flymine.web.results;

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
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;

import org.flymine.metadata.Model;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.util.DynamicUtil;

/**
 * Implementation of <strong>TilesAction</strong>. Assembles data for
 * a results cell.
 *
 * @author Andrew Varley
 */
public class ResultsCellController extends TilesAction
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
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();

        Object obj = request.getAttribute("object");

        // Put a map of all fields on to the request
        Model model = ObjectStoreFactory.getObjectStore().getModel();

        Set clds = model.getClassDescriptorsForClass(obj.getClass());

        if (clds.size() > 0) {
            context.putAttribute("clds", clds);
        }

        Set leafClasses = DynamicUtil.decomposeClass(obj.getClass());
        Set leafClds = new HashSet();
        for (Iterator i = leafClasses.iterator(); i.hasNext(); ) {
            leafClds.add(model.getClassDescriptorByName(((Class) i.next()).getName()));
        }
        if (leafClds.size() > 0) {
            context.putAttribute("leafClds", leafClds);
        }

        return null;
    }
}
