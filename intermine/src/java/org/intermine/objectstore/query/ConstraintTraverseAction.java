package org.intermine.objectstore.query;

/*
 * Copyright (C) 2002-2004 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

/**
 * ConstraintTraverseActions are passed to the traverseConstraints() method.  The apply() method is
 * called for each Constraint and sub-Constraint.
 *
 * @author Kim Rutherford
 */

public interface ConstraintTraverseAction
{
    /**
     * The apply() method is called by ConstraintHelper.traverseConstraints() for each Constraint
     * and sub-Constraint.
     */
    public void apply(Constraint c);
}
