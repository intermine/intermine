package org.intermine.webservice.server.template.result;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.ConstraintOp;

/* 
 * In future Constraint class can be used instead of this class. This was used
 * because it was possible, that will carries other things than just constraint values.
 */
/**
 * Simple object that carries constraint values for other processing.  
 * @author Jakub Kulaviak
 **/
public class ConstraintLoad
{
    private ConstraintOp op;
    
    private String value;
    
    private String extraValue;

    /**
     * ConstraintLoad constructor.
     * @param op constraint operation
     * @param value value restricting result
     * @param extraValue optional extra value used for lookup, automatically restricts 
     * results according other criterion, for example for Gene there can specified organism name, 
     * restricts resulted genes to specified organism 
     */
    public ConstraintLoad(ConstraintOp op, String value, String extraValue) {
        this.op = op;
        this.value = value;
        this.extraValue = extraValue;
    }

    /**
     * Returns constraint operation
     * @return constraint operation
     */
    public ConstraintOp getConstraintOp() {
        return op;
    }

    /**
     * Sets constraint operation.
     * @param constraintOp constraint operation
     */
    public void setConstraintOp(ConstraintOp constraintOp) {
        this.op = constraintOp;
    }

    /**
     * Returns constraint value.
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets constraint value.
     * @param value value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns extra value. 
     * @return value
     * @see ConstraintLoad
     */
    public String getExtraValue() {
        return extraValue;
    }

    /**
     * Sets extra value
     * @param extraValue extra value
     * @see ConstraintLoad
     */
    public void setExtraValue(String extraValue) {
        this.extraValue = extraValue;
    }    
}
