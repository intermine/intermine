 package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.ConstraintOp;
import java.util.List;

/**
 * Simple object that carries constraint values for other processing.
 *
 * @author Jakub Kulaviak
 **/
public class ConstraintInput
{
    private ConstraintOp op;

    private String value;

    private List<String> multivalues;

    private String extraValue;

    private String pathId;

    private String code;

    private String parameterName;

    /**
     *
     * @return parameter name
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     *
     * @param parameterName saves to the object name of the path parameter of this constraint
     * For example: cons1, cons2 ...
     */
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    /**
     *
     * @return code
     */
    public String getCode() {
        return code;
    }

    /**
     *
     * @param code code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     *
     * @return path that is used as constraint id
     */
    public String getPathId() {
        return pathId;
    }

    /**
     *
     * @param pathId path that is used as constraint id
     */
    public void setPathId(String pathId) {
        this.pathId = pathId;
    }

    /**
     * ConstraintLoad constructor.
     * @param parameterName parameter name
     * @param pathId path that serves as id
     * @param code constraint code
     * @param op constraint operation
     * @param value value restricting result
     * @param extraValue optional extra value used for lookup, automatically restricts
     * results according other criterion, for example for Gene there can specified organism name,
     * restricts resulted genes to specified organism
     */
    public ConstraintInput(String parameterName, 
            String pathId, String code, ConstraintOp op,
            String value, List<String> multivalues, String extraValue) {
        this.code = code;
        this.parameterName = parameterName;
        this.pathId = pathId;
        this.op = op;
        this.value = value;
        this.multivalues = multivalues;
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
     * Return multivalues
     * @return multivalues
     */
    public List<String> getMultivalues() {
        return multivalues;
    }

    /**
     * Set multivalues
     * @param multivalues A list of values
     */
    public void setMultivalues(List<String> multivalues) {
        this.multivalues = multivalues;
    }

    /**
     * Returns extra value.
     * @return value
     * @see ConstraintInput
     */
    public String getExtraValue() {
        return extraValue;
    }

    /**
     * Sets extra value
     * @param extraValue extra value
     * @see ConstraintInput
     */
    public void setExtraValue(String extraValue) {
        this.extraValue = extraValue;
    }
}
