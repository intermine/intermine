package org.intermine.api.template;

/*
 * Copyright (C) 2002-2012 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.intermine.api.profile.InterMineBag;
import org.intermine.api.util.PathUtil;
import org.intermine.model.InterMineObject;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathException;
import org.intermine.template.SwitchOffAbility;
import org.intermine.template.TemplatePopulatorException;
import org.intermine.template.TemplateQuery;
import org.intermine.template.TemplateValue;
import org.intermine.template.TemplateValue.ValueType;
import org.intermine.util.DynamicUtil;


/**
 * Configures original template. Old constraints are replaced with the similar
 * new constraints, that have different values.
 * @author Richard Smith
 **/
public final class TemplatePopulator
{
    private TemplatePopulator() {
        // don't
    }

    /**
     * Given a template and a map of values for editable constraints on each editable node create
     * a copy of the template query with the values filled in.  This may alter the query when e.g.
     * bag constraints are applied to a class rather than an attribute.
     *
     * @param origTemplate the template to populate with values
     * @param newConstraints a map from editable node to a list of values for each editable
     *  constraint
     * @return a copy of the template with values filled in
     * @throws TemplatePopulatorException if something goes wrong
     */
    public static TemplateQuery getPopulatedTemplate(TemplateQuery origTemplate,
            Map<String, List<TemplateValue>> newConstraints) {
        TemplateQuery template = origTemplate.clone();
        template.setEdited(true);
        Set<List<TemplateValue>> providedValues = new HashSet<List<TemplateValue>>(
                newConstraints.values());

        for (String editablePath : origTemplate.getEditablePaths()) {
            List<PathConstraint> constraints = origTemplate.getEditableConstraints(editablePath);
            List<TemplateValue> values = newConstraints.get(editablePath);

            // TODO this is a temporary fix, this section of code should be re-written without
            // editablePaths.  Each TemplateValue has a reference to a PathConstraint.
            if (values == null) {
                values = new ArrayList<TemplateValue>();
            }

            if (constraints.size() < values.size()) {
                throw new TemplatePopulatorException("There were more values provided than "
                        + "  there are editable constraints on the path " + editablePath);
            }

            for (PathConstraint con : constraints) {
                boolean found = false;
                for (TemplateValue templateValue : values) {
                    if (con.equals(templateValue.getConstraint())) {
                        try {
                            setConstraint(template, templateValue);
                        } catch (PathException e) {
                            throw new TemplatePopulatorException("Invalid path found when "
                                    + "populating template.", e);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (!template.isOptional(con)) {
                        throw new TemplatePopulatorException(
                                "No value provided for required constraint " + con);
                    }
                    template.removeConstraint(con);
                }
            }
            providedValues.remove(values);
        }
        if (!providedValues.isEmpty()) {
            throw new TemplatePopulatorException("Values provided for non-existent constraints: "
                + providedValues);
        }
        return template;
    }


    /**
     * Constrain a template query with a single editable constraint to be the given object.  This
     * returns a copy of the template with the value filled in, if the existing constraint will be
     * replaced by a constraint on the id field of the editable node.
     *
     * @param template the template to constrain
     * @param obj the object to constrain to
     * @return a copy of the template with values filled in
     * @throws PathException if the template is invalid
     */
    public static  TemplateQuery populateTemplateWithObject(TemplateQuery template,
            InterMineObject obj) throws PathException {
        Map<String, List<TemplateValue>> templateValues =
            new HashMap<String, List<TemplateValue>>();

        if (template.getEditableConstraints().size() != 1) {
            throw new TemplatePopulatorException("Template must have exactly one editable "
                    + "constraint to be configured with an object.");
        }

        PathConstraint constraint = template.getEditableConstraints().get(0);

        Path path = getPathOfClass(template, constraint.getPath());
        if (!PathUtil.canAssignObjectToType(path.getEndType(), obj)) {
            throw new TemplatePopulatorException("The constraint of type " + path.getEndType()
                    + " can't be set to object of type "
                    + DynamicUtil.getFriendlyName(obj.getClass())
                    + " in template query " + template.getName() + ".");
        }

        TemplateValue templateValue = new TemplateValue(constraint,
                ConstraintOp.EQUALS, obj.getId().toString(), TemplateValue.ValueType.OBJECT_VALUE,
                SwitchOffAbility.ON);
        templateValues.put(constraint.getPath(),
                new ArrayList<TemplateValue>(Collections.singleton(templateValue)));

        return TemplatePopulator.getPopulatedTemplate(template, templateValues);
    }

    /**
     * Constrain a template query with a single editable constraint to be in the given bag.  This
     * returns a copy of the template with the value filled in, if the constraint is on an
     * attribute it will be replaced by a constrain on the parent class.
     *
     * @param template the template to constrain
     * @param bag the bag to constrain to
     * @return a copy of the template with values filled in
     * @throws PathException if the template is invalid
     */
    public static  TemplateQuery populateTemplateWithBag(TemplateQuery template,
            InterMineBag bag) throws PathException {
        Map<String, List<TemplateValue>> templateValues =
            new HashMap<String, List<TemplateValue>>();

        if (template.getEditableConstraints().size() != 1) {
            throw new TemplatePopulatorException("Template must have exactly one editable "
                    + "constraint to be configured with a bag.");
        }

        PathConstraint constraint = template.getEditableConstraints().get(0);
        Path path = getPathOfClass(template, constraint.getPath());
        if (!bag.isOfType(path.getLastClassDescriptor().getName())) {
            throw new TemplatePopulatorException("The constraint of type "
                    + path.getNoConstraintsString()
                    + " can't be set to a bag (list) of type " + bag.getType()
                    + " in template query " + template.getName() + ".");
        }

        TemplateValue templateValue = new TemplateValue(constraint, ConstraintOp.IN,
                bag.getName(), TemplateValue.ValueType.BAG_VALUE, SwitchOffAbility.ON);
        templateValues.put(constraint.getPath(),
                new ArrayList<TemplateValue>(Collections.singleton(templateValue)));

        return TemplatePopulator.getPopulatedTemplate(template, templateValues);
    }


    private static Path getPathOfClass(TemplateQuery template, String pathStr)
        throws PathException {
        Path path = template.makePath(pathStr);
        if (path.endIsAttribute()) {
            path = path.getPrefix();
        }
        return path;
    }

    /**
     * Populate a TemplateQuery that has a single editable constraint with the given value.  This
     * returns a copy of the template with the value filled in.
     * @param template the template query to populate
     * @param op operation of the constraint
     * @param value value to be constrained to
     * @return a copy of the template with the value filled in
     * @throws PathException if the template is invalid
     */
    public static TemplateQuery populateTemplateOneConstraint(
            TemplateQuery template, ConstraintOp op, String value) throws PathException {
        Map<String, List<TemplateValue>> templateValues =
            new HashMap<String, List<TemplateValue>>();

        if (template.getEditableConstraints().size() != 1) {
            throw new RuntimeException("Template must have exactly one editable constraint to be "
                    + " configured with a single value.");
        }

        String editablePath = template.getEditablePaths().get(0);
        Path path = template.makePath(editablePath);
        if (path.endIsAttribute()) {
            path = path.getPrefix();
        }
        PathConstraint constraint = template.getEditableConstraints(editablePath).get(0);

        TemplateValue templateValue = new TemplateValue(constraint, op, value,
                TemplateValue.ValueType.SIMPLE_VALUE, SwitchOffAbility.ON);
        templateValues.put(constraint.getPath(),
                new ArrayList<TemplateValue>(Collections.singleton(templateValue)));

        return TemplatePopulator.getPopulatedTemplate(template, templateValues);
    }


    /**
     * Set the value for a constraint in the template query with the given TemplateValue.  This may
     * move a constraint from an attribute to the parent class if the constraint is object or bag.
     * @param template the template to constrain
     * @param templateValue container for the value to set constraint to
     * @throws PathException if a TemplateValue contains an invalid path
     */
    protected static void setConstraint(TemplateQuery template, TemplateValue templateValue)
        throws PathException {

        PathConstraint originalConstraint = templateValue.getConstraint();
        Path constraintPath = template.makePath(templateValue.getConstraint().getPath());
        String pathString = constraintPath.getNoConstraintsString();

        if (templateValue.isBagConstraint()) {
            if (constraintPath.endIsAttribute()) {
                constraintPath = constraintPath.getPrefix();
            }
            PathConstraint newConstraint =
                new PathConstraintBag(constraintPath.getNoConstraintsString(),
                    templateValue.getOperation(), templateValue.getValue());
            template.replaceConstraint(originalConstraint, newConstraint);
            template.setSwitchOffAbility(newConstraint, templateValue.getSwitchOffAbility());
        } else if (templateValue.isObjectConstraint()) {
            if (constraintPath.endIsAttribute()) {
                constraintPath = constraintPath.getPrefix();
            }
            String idPath = constraintPath.getNoConstraintsString() + ".id";
            PathConstraint newConstraint =
                new PathConstraintAttribute(idPath, templateValue.getOperation(),
                        templateValue.getValue());
            template.replaceConstraint(originalConstraint, newConstraint);
            template.setSwitchOffAbility(newConstraint, templateValue.getSwitchOffAbility());
        } else {
            // this is one of the valid constraint types for templates
            PathConstraint newConstraint = null;
            if (originalConstraint instanceof PathConstraintAttribute) {
                // if the op has been changed to IN or NOT_IN this becomes a multi value constraint
                if (PathConstraintMultiValue.VALID_OPS.contains(templateValue.getOperation())) {
                    newConstraint = new PathConstraintMultiValue(pathString,
                            templateValue.getOperation(), templateValue.getValues());
                } else {
                    newConstraint = new PathConstraintAttribute(pathString,
                            templateValue.getOperation(), templateValue.getValue());
                }
            } else if (originalConstraint instanceof PathConstraintLookup) {
                newConstraint = new PathConstraintLookup(pathString,
                            templateValue.getValue(), templateValue.getExtraValue());
            } else if (originalConstraint instanceof PathConstraintNull) {
                newConstraint = new PathConstraintNull(pathString, templateValue.getOperation());
            } else if (originalConstraint instanceof PathConstraintMultiValue) {
                // if op has been changed to something other than IN or NOT_IN make this becomes
                // a regular attribute constraint
                if (!PathConstraintMultiValue.VALID_OPS.contains(templateValue.getOperation())) {
                    newConstraint = new PathConstraintAttribute(pathString,
                        templateValue.getOperation(), templateValue.getValue());
                } else {
                    newConstraint = new PathConstraintMultiValue(pathString,
                        templateValue.getOperation(), templateValue.getValues());
                }
            }
            template.replaceConstraint(originalConstraint, newConstraint);
            template.setSwitchOffAbility(newConstraint, templateValue.getSwitchOffAbility());
        }
    }
}
