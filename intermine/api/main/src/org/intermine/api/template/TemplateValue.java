package org.intermine.api.template;

/*
 * Copyright (C) 2002-2009 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.ConstraintOp;


/**
 * A TemplateValue represents the value for an editable constraint of a template query.
 *
 * @author Richard Smith
 **/
public class TemplateValue
{
    private ConstraintOp op;
    private Object value;
    private Object extraValue;
    private String path;
    private String code;
    private boolean bagConstraint = false;
    private boolean objectConstraint = false;


    /**
     * @param operation operation
     * @param value value
     * @param extraValue extra value
     */
    public TemplateValue(String path, ConstraintOp op, Object value, String code) {
        super();
        this.path = path;
        this.op = op;
        this.value = value;
        this.code = code;
    }

    /**
     * @param op operation
     * @param value value
     * @param extraValue extra value
     * @param the constraint code - needed as there may be more than one constraint on a path
     */
    public TemplateValue(String path, ConstraintOp op, Object value, String code,
            Object extraValue) {
        super();
        this.path = path;
        this.op = op;
        this.value = value;
        this.code = code;
        this.extraValue = extraValue;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return extra value
     */
    public Object getExtraValue() {
        return extraValue;
    }

    /**
     * @param extraValue extra value
     */
    public void setExtraValue(String extraValue) {
        this.extraValue = extraValue;
    }

    /**
     * @return operation
     */
    public ConstraintOp getOperation() {
        return op;
    }

    /**
     * Sets operation.
     * @param operation operation
     */
    public void setOperation(ConstraintOp op) {
        this.op = op;
    }

    /**
     * Returns value.
     * @return value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets value.
     * @param value value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isBagConstraint() {
        return bagConstraint;
    }

    public void setBagConstraint(boolean bagConstraint) {
        this.bagConstraint = bagConstraint;
    }

    public boolean isObjectConstraint() {
        return objectConstraint;
    }

    public void setObjectConstraint(boolean objectConstraint) {
        this.objectConstraint = objectConstraint;
    }

    public String toString() {
        return code + " - " + path + " " + op + " " + value + " (" + extraValue + ", " 
            + objectConstraint + ", " + bagConstraint + ")";
    }
    
}
