package org.intermine.web.logic.querybuilder;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.lang.StringUtils;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintIds;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintMultiValue;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathConstraintSubclass;


/**
 * Representation of a PathQuery constraint with additional information for display in the
 * QueryBuilder summary section.  Holds the constraint code in the query and, if the query is a
 * template, whether the constraint is editable.
 *
 * @author Richard Smith
 *
 */
public class SummaryConstraint
{
    private PathConstraint con;
    private boolean editableInTemplate;
    private String code;
    private String description;
    private String switchable;

    /**
     * Construct with the underlying PathConstraint and additional details from the PathQuery.
     * @param con the constraint represented
     * @param code the code of this constraint in the query
     * @param editableInTemplate true if the query is a template and this is an editable constraint
     * @param description the description
     * @param switchable the switchOffAbility
     */
    public SummaryConstraint(PathConstraint con, String code, boolean editableInTemplate,
                             String description, String switchable) {
        this.con = con;
        this.code = code;
        this.editableInTemplate = editableInTemplate;
        this.description = description;
        this.switchable = switchable;
    }

    /**
     * Get a string representation of the constraint operation.
     * @return the constraint op
     */
    public String getOp() {
        return con.getOp().toString();
    }

    /**
     * Get the code associated with this constraint in the query.
     * @return the constraint code
     */
    public String getCode() {
        return code;
    }

    /**
     * Get a string representation of the constraint value to be displayed.  If this is null or
     * not null constraint this returns null as the operation is the same as the value.  For LOOKUP
     * constraints the string will include the extra value if present.
     * @return a representation of the constraint value
     */
    public String getValue() {
        if (con instanceof PathConstraintAttribute) {
            return ((PathConstraintAttribute) con).getValue();
        } else if (con instanceof PathConstraintBag) {
            return ((PathConstraintBag) con).getBag();
        } else if (con instanceof PathConstraintLookup) {
            PathConstraintLookup pcl = (PathConstraintLookup) con;
            if (StringUtils.isBlank(pcl.getExtraValue())) {
                return pcl.getValue();
            } else {
                return pcl.getValue() + " IN " + pcl.getExtraValue();
            }
        } else if (con instanceof PathConstraintSubclass) {
            return ((PathConstraintSubclass) con).getType();
        } else if (con instanceof PathConstraintLoop) {
            String loopPath = ((PathConstraintLoop) con).getLoopPath();
            loopPath = loopPath.replace(".", " > ");
            return loopPath;
        } else if (con instanceof PathConstraintNull) {
            // return nothing as op for is_null constraints is already being displayed
            return null;
        } else if (con instanceof PathConstraintMultiValue) {
            Collection<String> multiValues = ((PathConstraintMultiValue) con).getValues();
            return Arrays.toString(multiValues.toArray());
        } else if (con instanceof PathConstraintIds) {
            return Arrays.toString(PathConstraint.getIds(con).toArray());
        } else {
            throw new Error("PathConstraint type not recognised: " + con.getClass().getName());
        }
    }


    /**
     * Returns true if the constraint is part of a template query and the constraint is editable in
     * that template.  Determines status of the 'lock' icon on template constraints.
     * @return true if this is a template editable constraint
     */
    public boolean isEditableInTemplate() {
        return editableInTemplate;
    }

    /**
     * Return true if this type of constraint can be set as editable in a template query - i.e. if
     * it is an attribute or lookup constraint or null constraint.
     * @return true if can be set as an editable constraint
     */
    public boolean isValidEditableConstraintType() {
        return ((con instanceof PathConstraintAttribute)
                || (con instanceof PathConstraintLookup)
                || (con instanceof PathConstraintNull)
                || (con instanceof PathConstraintMultiValue));
    }

    /**
     * Get the description for a constraint.
     * @return a description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the switchoffAbility for a constraint.
     * @return the switchoffAbility
     */
    public String getSwitchable() {
        return switchable;
    }
}

