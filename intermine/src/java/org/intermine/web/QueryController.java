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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.objectstore.ObjectStoreFactory;
import org.flymine.util.TypeUtil;

/**
 * Perform initialisation steps for query editing tile prior to calling
 * query.jsp.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryController extends TilesAction
{
    /**
     * @see TilesAction#execute
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();

        Model model = ObjectStoreFactory.getObjectStore().getModel();
        session.setAttribute("model", new DisplayModel(model));

        Map classNames = new HashMap();
        Iterator iter = model.getClassDescriptors().iterator();
        while (iter.hasNext()) {
            ClassDescriptor cld = (ClassDescriptor) iter.next();
            classNames.put(cld.getName(), TypeUtil.unqualifiedName(cld.getName()));
        }
        session.setAttribute("classNames", classNames);

        return null;
    }
}
