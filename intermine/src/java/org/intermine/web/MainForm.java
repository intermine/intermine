package org.intermine.web;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.apache.struts.action.ActionForm;

/**
 * The main form, using for editing constraints
 * @author Mark Woodbridge
 */
public class MainForm extends ActionForm
{
    protected String constraintOp, constraintValue, path, subclass;

    /**
     * Gets the value of subclass
     *
     * @return the value of subclass
     */
    public String getSubclass()  {
        return subclass;
    }

    /**
     * Sets the value of subclass
     *
     * @param subclass Value to assign to subclass
     */
    public void setSubclass(String subclass) {
        this.subclass = subclass;
    }
    /**
     * Gets the value of constraintOp
     *
     * @return the value of constraintOp
     */
    public String getConstraintOp()  {
        return constraintOp;
    }

    /**
     * Sets the value of constraintOp
     *
     * @param constraintOp Value to assign to constraintOp
     */
    public void setConstraintOp(String constraintOp) {
        this.constraintOp = constraintOp;
    }

    /**
     * Gets the value of constraintValue
     *
     * @return the value of constraintValue
     */
    public String getConstraintValue()  {
        return constraintValue;
    }

    /**
     * Sets the value of constraintValue
     *
     * @param constraintValue Value to assign to constraintValue
     */
    public void setConstraintValue(String constraintValue) {
        this.constraintValue = constraintValue;
    }

    /**
     * Gets the value of path
     *
     * @return the value of path
     */
    public String getPath()  {
        return path;
    }

    /**
     * Sets the value of path
     *
     * @param path Value to assign to path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @see ActionForm#reset
     */
    public void reset() {
        constraintOp = null;
        constraintValue = null;
        path = null;
        subclass = null;
    }
}
