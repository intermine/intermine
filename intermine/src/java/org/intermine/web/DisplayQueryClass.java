package org.flymine.web;

/*
 * Copyright (C) 2002-2003 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.ArrayList;
import java.util.List;

/**
 * Bean to represent a QueryClass during Query construction in the webapp
 * @author Mark Woodbridge
 */
public class DisplayQueryClass
{
    String type;
    List constraintNames = new ArrayList();
    List fieldNames = new ArrayList();
    List fieldOps = new ArrayList();
    List fieldValues = new ArrayList();

    /**
     * Gets the value of type
     *
     * @return the value of type
     */
    public String getType()  {
        return this.type;
    }

    /**
     * Sets the value of type
     *
     * @param argType Value to assign to this.type
     */
    public void setType(String argType) {
        this.type = argType;
    }

    /**
     * Gets the value of constraintNames
     *
     * @return the value of constraintNames
     */
    public List getConstraintNames()  {
        return this.constraintNames;
    }

    /**
     * Sets the value of constraintNames
     *
     * @param argConstraintNames Value to assign to this.constraintNames
     */
    public void setConstraintNames(List argConstraintNames) {
        this.constraintNames = argConstraintNames;
    }

    /**
     * Gets the value of fieldNames
     *
     * @return the value of fieldNames
     */
    public List getFieldNames()  {
        return this.fieldNames;
    }

    /**
     * Sets the value of fieldNames
     *
     * @param argFieldNames Value to assign to this.fieldNames
     */
    public void setFieldNames(List argFieldNames) {
        this.fieldNames = argFieldNames;
    }

    /**
     * Gets the value of fieldOps
     *
     * @return the value of fieldOps
     */
    public List getFieldOps()  {
        return this.fieldOps;
    }

    /**
     * Sets the value of fieldOps
     *
     * @param argFieldOps Value to assign to this.fieldOps
     */
    public void setFieldOps(List argFieldOps) {
        this.fieldOps = argFieldOps;
    }

    /**
     * Gets the value of fieldValues
     *
     * @return the value of fieldValues
     */
    public List getFieldValues()  {
        return this.fieldValues;
    }

    /**
     * Sets the value of fieldValues
     *
     * @param argFieldValues Value to assign to this.fieldValues
     */
    public void setFieldValues(List argFieldValues) {
        this.fieldValues = argFieldValues;
    }

    /**
     * @see Object#toString
     */
    public String toString() {
        return type + " " + " " + constraintNames + " " + fieldNames + " " + fieldOps + " "
            + fieldValues;
    }
}
