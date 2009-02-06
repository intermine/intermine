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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.Model;
import org.intermine.pathquery.Constraint;
import org.intermine.pathquery.ConstraintValueParser;
import org.intermine.pathquery.ParseValueException;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathError;
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

    /**
     * Makes copy of original template and configures it with new constraints that have
     * new values. New constraints must correspond to old constraints. Actually this creates
     * very similar TemplateQuery with only different values.
     * @param origTemplate original template
     * @param newConstraints new constraints
     * @return new template
     */
    public TemplateQuery getConfiguredTemplate(TemplateQuery origTemplate,
                                               Map<String, List<ConstraintLoad>> newConstraints) {
        /* Be carefull when changing this code. If you replace constraint
         * in list that was returned for example from getEditableConstraints method,
         * this change won't have effect to template */
        checkPaths(origTemplate.getModel(), newConstraints.values());
        TemplateQuery template = (TemplateQuery) origTemplate.clone();
        for (PathNode node : template.getEditableNodes()) {
            List<Constraint> cons = template.getEditableConstraints(node);
            List<ConstraintLoad> loads = newConstraints.get(node.getPathString());
            if (loads == null) {
                throw new BadRequestException("There isn't specified constraint value "
                        + "and operation for path " + node.getPathString() 
                        + " in the request.");                
            }
            if (cons.size() > loads.size()) {
                throw new BadRequestException("Template has more editable constraints for path " 
                        + node.getPathString() + " than was specified. All must be specified in "
                        + "the request.");
            }
            if (cons.size() < loads.size()) {
                throw new BadRequestException("There were more constraints specified "
                        + " in the request than there are editable constraints for path " 
                        + node.getPathString() + ".");            
            }
            if (loads.size() == 1) {
                setConstraint(node, cons.get(0), loads.get(0));                                    
            } else {
                for (Constraint con : cons) {
                    boolean found = false;
                    for (int i = 0; i < loads.size(); i++) {
                        ConstraintLoad load = loads.get(i);
                        if (load != null && con.getCode().equals(load.getCode())) {
                            setConstraint(node, con, load);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new BadRequestException("There are multiple editable constraints for "
                                + "path " + node.getPathString() + " but value and operation for "
                                + "constraint with code " + con.getCode() + " wasn't specified in " 
                                + "the request or there is an error. Check the codes.");
                    }
                }
            }
        }
        return template;
    }

    private void checkPaths(Model model, Collection<List<ConstraintLoad>> collection) {
        for (List<ConstraintLoad> col : collection) {
            for (ConstraintLoad load : col) {
                try {
                    new Path(model, load.getPathId());    
                } catch (PathError ex) {
                    throw new BadRequestException("Invalid path specified in " 
                            + load.getParameterName() + " parameter.");
                }                
            }
        }
    }

    private void setConstraint(PathNode node, Constraint c, ConstraintLoad load) {
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
}
