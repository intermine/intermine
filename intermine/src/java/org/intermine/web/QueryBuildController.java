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
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.flymine.metadata.ClassDescriptor;
import org.flymine.metadata.AttributeDescriptor;
import org.flymine.util.TypeUtil;
import org.flymine.objectstore.query.SimpleConstraint;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.QueryClass;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.QueryValue;
import org.flymine.objectstore.query.QueryField;

import org.flymine.objectstore.query.presentation.ConstraintListCreator;
import org.flymine.objectstore.query.presentation.AssociatedConstraint;

/**
 * Perform initialisation steps for query editing tile prior to calling
 * query.jsp.
 *
 * @author Mark Woodbridge
 * @author Richard Smith
 */
public class QueryBuildController extends TilesAction
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

        ClassDescriptor cld = (ClassDescriptor) session.getAttribute("cld");
        if (cld != null) {
            request.getSession().setAttribute("ops", getOps(cld));
        }

        form.reset(mapping, request);
        if (session.getAttribute("alias") != null) {
            populateQueryBuildForm((QueryBuildForm) form, session);
            request.setAttribute("aliasStr", session.getAttribute("alias"));  // used for display in view
            session.removeAttribute("alias");
        }
        return null;
    }

    /**
     * Editing an QueryClass already on query so need to fill in form from
     * constraints already on query.
     *
     * @param form the QueryBuildForm to populate
     * @param session HttpSession to get alias and query from
     * @throws Exception if anything goes wrong
     */
    protected void populateQueryBuildForm(QueryBuildForm form, HttpSession session)
        throws Exception {

        String alias = (String) session.getAttribute("alias");
        Query q = (Query) session.getAttribute("query");
        QueryClass qc = (QueryClass) q.getReverseAliases().get(alias);
        List constraints = ConstraintListCreator.createList(q, qc);

        Iterator iter = constraints.iterator();
        while (iter.hasNext()) {
            AssociatedConstraint ac = (AssociatedConstraint) iter.next();
            Constraint c = ac.getConstraint();
            if (c instanceof SimpleConstraint) {
                SimpleConstraint sc = (SimpleConstraint) c;
                if ((sc.getArg1() instanceof QueryField)  && (sc.getArg2() instanceof QueryValue)) {
                    form.setFieldValue(((QueryField) sc.getArg1()).getFieldName(),
                                       ((QueryValue) sc.getArg2()).getValue());

                    form.setFieldOp(((QueryField)sc.getArg1()).getFieldName(),
                                    new Integer(sc.getType()));
                }
            }
        }
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
