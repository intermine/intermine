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

import org.apache.struts.tiles.actions.TilesAction;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;

import org.flymine.objectstore.query.FromElement;
import org.flymine.objectstore.query.Query;
import org.flymine.objectstore.query.ConstraintHelper;
import org.flymine.objectstore.query.Constraint;
import org.flymine.objectstore.query.ConstraintSet;
import org.flymine.objectstore.query.presentation.PrintableConstraint;
import org.flymine.objectstore.query.presentation.AssociatedConstraint;

/**
 * Splits up the query in the request into little bits for queryView.jsp to display.
 *
 * @author Matthew Wakeling
 */
public class QueryViewController extends TilesAction
{
    /**
     * @see TilesAction#perform
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        Query query = (Query) request.getSession().getAttribute("query");
        List pclist = ConstraintHelper.createList(query);
        Map perFromConstraints = new HashMap();
        Set noFromConstraints = new HashSet();
        Map perFromTitle = new HashMap();
        Map perFromAlias = new HashMap();
        if (query != null) {
            Iterator fromIter = query.getFrom().iterator();
            while (fromIter.hasNext()) {
                FromElement fromElement = (FromElement) fromIter.next();
                if (fromElement instanceof org.flymine.objectstore.query.QueryClass) {
                    String fromElementString = fromElement.toString();
                    perFromTitle.put(fromElement,
                            fromElementString.substring(fromElementString.lastIndexOf(".") + 1));
                } else {
                    perFromTitle.put(fromElement, fromElement.toString());
                }
                perFromAlias.put(fromElement, query.getAliases().get(fromElement));
                Set fromSet = new HashSet();
                perFromConstraints.put(fromElement, fromSet);
                Iterator conIter = pclist.iterator();
                while (conIter.hasNext()) {
                    Constraint c = (Constraint) conIter.next();
                    if (!(c instanceof ConstraintSet)) {
                        AssociatedConstraint ac = new AssociatedConstraint(query, c);
                        if (ac.isAssociatedWith(fromElement)) {
                            fromSet.add(ac);
                        }
                    }
                }
            }
            Iterator conIter = pclist.iterator();
            while (conIter.hasNext()) {
                Constraint c = (Constraint) conIter.next();
                PrintableConstraint pc;
                if (c instanceof ConstraintSet) {
                    pc = new PrintableConstraint(query, (ConstraintSet) c);
                } else {
                    pc = new AssociatedConstraint(query, c);
                }

                if (pc.isAssociatedWithNothing()) {
                    noFromConstraints.add(pc);
                }
            }
        }

        context.putAttribute("perFromConstraints", perFromConstraints);
        context.putAttribute("noFromConstraints", noFromConstraints);
        context.putAttribute("perFromTitle", perFromTitle);
        context.putAttribute("perFromAlias", perFromAlias);

        return null;
    }
}

