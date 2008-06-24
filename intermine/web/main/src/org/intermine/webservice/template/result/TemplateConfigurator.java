package org.intermine.webservice.template.result;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.PathNode;
import org.intermine.util.TypeUtil;
import org.intermine.web.logic.template.ConstraintValueParser;
import org.intermine.web.logic.template.ParseValueException;
import org.intermine.web.logic.template.TemplateQuery;
import org.intermine.webservice.WebServiceException;


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
     * @param locale locale
     * @return new template
     */
    public TemplateQuery getConfiguredTemplate(TemplateQuery origTemplate,
            List<ConstraintLoad> newConstraints, Locale locale) {
        /* Made according to org.intermine.web.logic.template.
         * TemplateHelper.templateFormToTemplateQuery().
         * Be carefull when changing this code. If you replace constraint 
         * in list that was returned for example from getAllEditableConstraints method, 
         * this change won't have effect to template*/
        TemplateQuery template = (TemplateQuery) origTemplate.clone();
        newConstraintIt = newConstraints.iterator();
        for (PathNode node : template.getEditableNodes()) {
            for (Constraint c : template.getEditableConstraints(node)) {
                setConstraint(node, c, locale);
            }
        }
        return template;
    }

    private void setConstraint(PathNode node, Constraint c, Locale locale) {
        ConstraintLoad load = nextNewConstraint();
        int constraintIndex = node.getConstraints().indexOf(c);
        Object extraValue = getExtraValue(c, load, node, locale);
        Object value = getValue(c, load, node, locale);
        Constraint newConstraint = new Constraint(load.getConstraintOp(), value, 
                true, c.getDescription(), c.getCode(), c.getIdentifier(), extraValue); 
        node.getConstraints().set(constraintIndex, newConstraint);
    }

    private Object getValue(Constraint c, ConstraintLoad load, PathNode node, Locale locale) {
        Object ret;
        try {
            ret = new ConstraintValueParser().parse(load.getValue(), getType(node), 
                    load.getConstraintOp(), locale);    
        } catch (ParseValueException ex) {
            throw new WebServiceException("invalid value: " + load.getValue() + ". "
                    + ex.getMessage());                
        }
        return ret;
    }

    private Object getExtraValue(Constraint c, ConstraintLoad load, PathNode node, Locale locale) {
        Object ret;
        if (load.getExtraValue() == null || load.getExtraValue().trim().length() == 0) {
            return c.getExtraValue();
        }
        try {
            ret = new ConstraintValueParser().parse(load.getExtraValue(), getType(node), 
                    load.getConstraintOp(), locale);    
        } catch (ParseValueException ex) {
            throw new WebServiceException("invalid value: " + load.getExtraValue() + ". "
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
            throw new WebServiceException("There is insufficient number of constraints in your " 
                    + "request. Template has more constrains than you have specified.");
        }
    }    
}
