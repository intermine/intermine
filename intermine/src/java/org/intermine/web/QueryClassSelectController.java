package org.flymine.web;

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
import java.util.LinkedHashSet;
import java.util.TreeSet;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.tiles.ComponentContext;

import org.flymine.metadata.Model;
import org.flymine.util.TypeUtil;

/**
 * Implementation of <strong>TilesAction</strong>. Assembles data for
 * the QueryClass selection view.
 *
 * @author Mark Woodbridge
 */
public class QueryClassSelectController extends TilesAction
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
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        Model model = (Model) servletContext.getAttribute(Constants.MODEL);

        Set classNames = new TreeSet(model.getClassNames());
        Set unqualifiedClassNames = new LinkedHashSet();
        for (Iterator i = classNames.iterator(); i.hasNext();) {
            unqualifiedClassNames.add(TypeUtil.unqualifiedName((String) i.next()));
        }
        
        request.setAttribute("classNames", classNames);
        request.setAttribute("unqualifiedClassNames", unqualifiedClassNames);
        
        return null;
    }
}
