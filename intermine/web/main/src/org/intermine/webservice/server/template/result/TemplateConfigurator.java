package org.intermine.webservice.server.template.result;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;

import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.ConstraintValueParser;
import org.intermine.pathquery.ParseValueException;
import org.intermine.pathquery.PathNode;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.webservice.server.exceptions.BadRequestException;


/**
 * Configures original template. Old constraints are replaced with the similar
 * new constraints, that have different values.
 * @author Jakub Kulaviak
 **/
public class TemplateConfigurator
{

    private Iterator<ConstraintLoad> newConstraintIt;

    /**
     * Makes copy of original template and configures it with new constraints that have
     * new values. New constraints must correspond to old constraints. Actually this creates
     * very similar TemplateQuery with only different values.
     * @param origTemplate original template
     * @param newConstraints new constraints
     * @return new template
     */
    public TemplateQuery getConfiguredTemplate(TemplateQuery origTemplate,
                                               List<ConstraintLoad> newConstraints) {
        /* Made according to org.intermine.web.logic.template.
         * TemplateHelper.templateFormToTemplateQuery().
         * Be carefull when changing this code. If you replace constraint
         * in list that was returned for example from getAllEditableConstraints method,
         * this change won't have effect to template*/
        TemplateQuery template = (TemplateQuery) origTemplate.clone();
        newConstraintIt = newConstraints.iterator();
        for (PathNode node : template.getEditableNodes()) {
            for (Constraint c : template.getEditableConstraints(node)) {
                setConstraint(node, c);
            }
        }
        return template;
    }

    private void setConstraint(PathNode node, Constraint c) {
        ConstraintLoad load = nextNewConstraint();
        int constraintIndex = node.getConstraints().indexOf(c);
        Object extraValue = getExtraValue(c, load, node);
        Object value = getValue(c, load, node);
        Constraint newConstraint = new Constraint(load.getConstraintOp(), value,
                true, c.getDescription(), c.getCode(), c.getIdentifier(), extraValue);
        node.getConstraints().set(constraintIndex, newConstraint);
    }

    private Object getValue(Constraint c, ConstraintLoad load, PathNode node) {
        Object ret;
        try {
            ret = ConstraintValueParser.parse(load.getValue(), getType(node),
                                              load.getConstraintOp());
        } catch (ParseValueException ex) {
            throw new BadRequestException("invalid value: " + load.getValue() + ". "
                    + ex.getMessage());
        }
        return ret;
    }

    private Object getExtraValue(Constraint c, ConstraintLoad load, PathNode node) {
        Object ret;
        if (load.getExtraValue() == null || load.getExtraValue().trim().length() == 0) {
            return c.getExtraValue();
        }
        try {
            ret = ConstraintValueParser.parse(load.getExtraValue(), getType(node),
                                              load.getConstraintOp());
        } catch (ParseValueException ex) {
            throw new BadRequestException("invalid value: " + load.getExtraValue() + ". "
                    + ex.getMessage());
        }
        return ret;
    }

    private Class getType(PathNode node) {
        Class fieldClass;
        if (node.isAttribute()) {
            fieldClass = TypeUtil.getClass(node.getType());
        } else {
            fieldClass = String.class;
        }
        return fieldClass;
    }


    private ConstraintLoad nextNewConstraint() {
        if (newConstraintIt.hasNext()) {
            return newConstraintIt.next();
        } else {
            throw new BadRequestException("There is insufficient number of constraints in your "
                    + "request. Template has more constrains than you have specified.");
        }
    }
}
