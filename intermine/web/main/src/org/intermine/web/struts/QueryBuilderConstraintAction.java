package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2015 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.metadata.ConstraintOp;
import org.intermine.pathquery.Constraints;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintRange;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathQuery;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplateQuery;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Action to handle button presses on the main tile
 *
 * @author Mark Woodbridge
 * @author Matthew Wakeling
 * @author Richard Smith
 */
public class QueryBuilderConstraintAction extends InterMineAction
{

    /**
     * Method called when user has finished updating a constraint.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws an exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public ActionForward execute(
            ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        //to prevent submit twice
        if (!isTokenValid(request)) {
            return mapping.findForward("query");
        }
        resetToken(request);
        PathQuery query = SessionMethods.getQuery(session);
        QueryBuilderConstraintForm constraintForm = (QueryBuilderConstraintForm) form;

        Path path = query.makePath(constraintForm.getPath());
        String rootPath = null;
        if (path.endIsAttribute()) {
            rootPath = path.getPrefix().toStringNoConstraints();
        } else {
            rootPath = path.toStringNoConstraints();
        }

        String joinType = constraintForm.getJoinType();

        String editingConstraintCode = constraintForm.getEditingConstraintCode();

        boolean constrainToAnAttribute = request.getParameter("attribute") != null;
        boolean constrainToABag = request.getParameter("bag") != null;
        boolean constrainToALoop = request.getParameter("loop") != null;
        boolean constrainToASubclass = request.getParameter("subclass") != null;
        boolean constrainToNull = request.getParameter("nullnotnull") != null;
        boolean editingTemplateConstraintParams = request.getParameter("template") != null;
        boolean constrainToARange = request.getParameter("range") != null;

        // Select the join style for the path in the query
        // If we set an outer join, then we need to take care of the consequences, like order by
        // and loop constraints
        if ((!StringUtils.isEmpty(joinType)) && (rootPath.contains("."))) {
            if ("outer".equals(joinType)) {
                query.setOuterJoinStatus(rootPath, OuterJoinStatus.OUTER);
                List<String> messages = query.fixUpForJoinStyle();
                for (String message : messages) {
                    SessionMethods.recordMessage(message, session);
                }
            } else {
                query.setOuterJoinStatus(rootPath, null);
            }
        }

        PathConstraint oldConstraint = null;
        if (!StringUtils.isBlank(editingConstraintCode)) {
            oldConstraint = query.getConstraintForCode(editingConstraintCode);

            if (query instanceof TemplateQuery) {
                TemplateQuery template = (TemplateQuery) query;
                if (editingTemplateConstraintParams) {
                    // We're just updating template settings
                    template.setEditable(oldConstraint, constraintForm.isEditable());
                    template.setConstraintDescription(oldConstraint,
                            constraintForm.getTemplateLabel());

                    String switchable = constraintForm.getSwitchable();
                    if ("on".equals(switchable)) {
                        template.setSwitchOffAbility(oldConstraint, SwitchOffAbility.ON);
                    } else if ("off".equals(switchable)) {
                        template.setSwitchOffAbility(oldConstraint, SwitchOffAbility.OFF);
                    } else {
                        template.setSwitchOffAbility(oldConstraint, SwitchOffAbility.LOCKED);
                    }
                    constraintForm.reset(mapping, request);
                    return mapping.findForward("query");
                }
            }
        }

        PathConstraint newConstraint = null;
        if (constrainToAnAttribute) {
            ConstraintOp constraintOp = ConstraintOp.getOpForIndex(Integer.valueOf(constraintForm
                    .getAttributeOp()));
            String constraintValue = constraintForm.getAttributeValue();
            if (ConstraintOp.LOOKUP.equals(constraintOp)) {
                newConstraint = new PathConstraintLookup(constraintForm.getPath(), constraintValue,
                        constraintForm.getExtraValue());
            } else if (PathConstraintMultiValue.VALID_OPS.contains(constraintOp)) {
                String multiValues = constraintForm.getMultiValueAttribute();
                newConstraint = new PathConstraintMultiValue(constraintForm.getPath(), constraintOp,
                                                             Arrays.asList(multiValues.split(",")));
            } else {
                newConstraint = new PathConstraintAttribute(constraintForm.getPath(), constraintOp,
                        constraintValue);
            }
        } else if (constrainToABag) {
            ConstraintOp constraintOp =
                    ConstraintOp.getOpForIndex(Integer.valueOf(constraintForm.getBagOp()));
            String constraintValue = constraintForm.getBagValue();
            // Note, we constrain the parent if the path is an attribute
            Path path1 = query.makePath(constraintForm.getPath());
            if (path1.endIsAttribute()) {
                path1 = path1.getPrefix();
            }
            newConstraint = new PathConstraintBag(path1.getNoConstraintsString(), constraintOp,
                    constraintValue);
        } else if (constrainToALoop) {
            ConstraintOp constraintOp = ConstraintOp.getOpForIndex(Integer.valueOf(constraintForm
                    .getLoopQueryOp()));
            String constraintValue = constraintForm.getLoopQueryValue();
            // Here, we trust the the jsp has already prevented us from creating a loop over an
            // outer join boundary.
            newConstraint = new PathConstraintLoop(constraintForm.getPath(),
                constraintOp, constraintValue);
        } else if (constrainToASubclass) {
            newConstraint = new PathConstraintSubclass(constraintForm.getPath(),
                  constraintForm.getSubclassValue());
        } else if (constrainToARange) {
            Set<String> ranges = new HashSet<String>();
            String rangeString = constraintForm.getRangeConstraint();
            String[] bits = rangeString.split("[, ]+");
            ranges.addAll(Arrays.asList(bits));
            ConstraintOp constraintOp
                = ConstraintOp.getOpForIndex(Integer.valueOf(constraintForm.getRangeOp()));
            newConstraint = new PathConstraintRange(constraintForm.getPath(), constraintOp, ranges);
        } else if (constrainToNull) {
            if ("NotNULL".equals(constraintForm.getNullConstraint())) {
                newConstraint = Constraints.isNotNull(constraintForm.getPath());
            } else {
                newConstraint = Constraints.isNull(constraintForm.getPath());
            }
            // TODO: It doesn't make much sense to have a null constraint as the root of an outer
            // join group, but because the default join style is inner, we'll let the user shoot
            // themselves in the foot if they really know how to.
        } else if (constraintForm.getUseJoin() != null) {
            // already dealt with that
        } else {
            StringBuilder sb = new StringBuilder("attributes = {");
            boolean needComma = false;
            Enumeration<String> attNames = request.getAttributeNames();
            while (attNames.hasMoreElements()) {
                if (needComma) {
                    sb.append(", ");
                }
                needComma = true;
                String attName = attNames.nextElement();
                sb.append(attName)
                    .append(" = ")
                    .append(request.getAttribute(attName));
            }
            sb.append("}, parameters = {");
            needComma = false;
            for (Map.Entry<String, String[]> param
                    : ((Map<String, String[]>) request.getParameterMap()).entrySet()) {
                if (needComma) {
                    sb.append(", ");
                }
                needComma = true;
                sb.append(param.getKey())
                    .append(" = [");
                boolean needComma2 = false;
                for (String val : param.getValue()) {
                    if (needComma2) {
                        sb.append(", ");
                    }
                    sb.append(val);
                }
                sb.append("]");
            }
            sb.append("}");
            throw new IllegalArgumentException("Unrecognised action: " + sb);
        }
        if (newConstraint != null) {
            if (oldConstraint != null) {
                // subclass constraints don't have a code but all other constraint types do
                if (oldConstraint instanceof PathConstraintSubclass
                        || newConstraint instanceof PathConstraintSubclass) {
                    query.removeConstraint(oldConstraint);
                    query.addConstraint(newConstraint);
                } else {
                    query.replaceConstraint(oldConstraint, newConstraint);
                }
            } else {
                query.addConstraint(newConstraint);
            }
            if (query instanceof TemplateQuery
                 && !(newConstraint instanceof PathConstraintSubclass)
                 && !(newConstraint instanceof PathConstraintLoop)) {
                ((TemplateQuery) query).setEditable(newConstraint, true);
            }
        }

        constraintForm.reset(mapping, request);

        return mapping.findForward("query");
    }
}
