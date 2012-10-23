package org.intermine.web.logic.template;

/*
 * Copyright (C) 2002-2012 FlyMine
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
     * @param multivalues The list of values in the case of a multivalue operator.
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

    @Override
    public String toString() {
        return new StringBuffer("<" + getClass().getName())
                     .append(" parameterName=" + parameterName)
                     .append(" pathId=" + pathId)
                     .append(" code=" + code)
                     .append(" value=" + value)
                     .append(" multivalues=" + multivalues)
                     .append(" extraValue=" + extraValue)
                     .append(">")
                     .toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof ConstraintInput) {
            ConstraintInput o = (ConstraintInput) other;
            boolean eq = parameterName.equals(o.getParameterName());
            eq = eq && (value == null) ? o.getValue() == null : value.equals(o.getValue());
            eq = eq && (code == null) ? o.getCode() == null : code.equals(o.getCode());
            eq = eq && (pathId == null) ? o.getPathId() == null : pathId.equals(o.getPathId());
            eq = eq && ((extraValue == null)
                    ? o.getExtraValue() == null
                    : extraValue.equals(o.getExtraValue()));
            if (multivalues == null) {
                eq = eq && o.getMultivalues() == null;
            } else {
                boolean multisAreEqual = o.getMultivalues() != null;
                if (multisAreEqual) {
                    for (String mv: multivalues) {
                        multisAreEqual = multisAreEqual && o.getMultivalues().contains(mv);
                    }
                    multisAreEqual = multisAreEqual
                            && multivalues.size() == o.getMultivalues().size();
                }
                eq = eq && multisAreEqual;
            }
            return eq;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 17;
        if (value != null) {
            hash *= 19 * value.hashCode();
        }
        if (code != null) {
            hash *= 27 * code.hashCode();
        }
        if (pathId != null) {
            hash *= 33 * pathId.hashCode();
        }
        if (multivalues != null) {
            hash *= 37 * multivalues.hashCode();
        }
        if (extraValue != null) {
            hash *= 43 * extraValue.hashCode();
        }
        return hash;
    }
}
