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

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.metadata.Model;
import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.metadata.presentation.DisplayModel;
import org.flymine.util.TypeUtil;
import org.flymine.objectstore.query.SimpleConstraint;

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
        request.getSession().setAttribute("model",
                                          new DisplayModel(Model.getInstanceByName("testmodel")));
        ClassDescriptor cld = (ClassDescriptor) request.getSession().getAttribute("cld");
        if (cld != null) {
            request.getSession().setAttribute("ops", getOps(cld));
        }
        return null;
    }
    
    /**
     * This method returns a map from field names to a map of operation codes to operation strings
     * For example 'name' -> 0 -> 'EQUALS'
     * @param cld the ClassDescriptor to be inspected
     * @return the map
     */
    protected Map getOps(ClassDescriptor cld) {
        Map fieldOps = new HashMap();
        Iterator iter = cld.getAllAttributeDescriptors().iterator();
        while (iter.hasNext()) {
            AttributeDescriptor attr = (AttributeDescriptor) iter.next();
            Map opString = new LinkedHashMap();
            int[] ops = SimpleConstraint.validOperators(TypeUtil.instantiate(attr.getType()));
            for (int i = 0; i < ops.length; i++) {
                opString.put(new Integer(ops[i]), SimpleConstraint.getOpString(ops[i]));
            }
            fieldOps.put(attr.getName(), opString);
        }
        return fieldOps;
    }
}
