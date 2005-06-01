package org.intermine.web.config;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * The webapp configuration for one field of a class.
 *
 * @author Kim Rutherford
 */

public class FieldConfig
{
    private String fieldExpr;

    /**
     * Set the JSTL expression for this field which will be evalutated in the request context when
     * the JSP is viewed.
     * @param fieldExpr the expression
     */
    public void setFieldExpr(String fieldExpr) {
        this.fieldExpr = fieldExpr;
    }

    /**
     * Return the JSTL expression for the object.
     * @return the expression
     */
    public String getFieldExpr() {
        return fieldExpr;
    }

    /**
     * @see Object#equals
     */
    public boolean equals(Object otherObject) {
        if (otherObject instanceof FieldConfig) {
            return ((FieldConfig) otherObject).fieldExpr.equals(fieldExpr);
        } else {
            return false;
        }
    }

    /**
     * @see Object#hashCode
     */
    public int hashCode() {
        return toString().hashCode();
    }
    
    /**
     * @see java.lang.String#toString
     */
    public String toString() {
        return "<fieldconfig fieldExpr=\"" + fieldExpr + "\"/>";
    }
}
