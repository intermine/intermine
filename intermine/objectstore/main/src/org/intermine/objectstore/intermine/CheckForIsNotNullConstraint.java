package org.intermine.objectstore.intermine;

/*
 * Copyright (C) 2002-2007 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.SimpleConstraint;
import org.intermine.objectstore.query.ConstraintTraverseAction;


/**
 * For use with ConstraintHelper.traverseConstraints().  For each constrain
 * check whether ir sets the given query node to be 'NOT NULL'
 *
 * @author Richard Smith
 */
public class CheckForIsNotNullConstraint implements ConstraintTraverseAction
{
    private boolean exists;
    private QueryNode node;

    /**
     * Construct with the QueryNode to test for NOT NULL constraint.
     * @param node the node to test
     */
    public CheckForIsNotNullConstraint(QueryNode node) {
        this.exists = false;
        this.node = node;
    }

    /**
     * Check whether a NOT NULL constraint was found.
     * @return true if NOT CULL constraint found
     */
    public boolean exists() {
        return exists;
    }

    /**
     * Perform test for NOT NULL constraint on node
     * @param c the constraint to test
     */
    public void apply(Constraint c) {

        if (c instanceof SimpleConstraint) {
            SimpleConstraint sc = (SimpleConstraint) c;
            if (sc.getOp().equals(ConstraintOp.IS_NOT_NULL)) {
                if (node.equals(sc.getArg1())) {
                    exists = true;
                }
            }
        }
    }
}
