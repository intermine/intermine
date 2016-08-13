package org.intermine.api.query;

/*
 * Copyright (C) 2002-2016 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */


import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Queryable;
import org.intermine.pathquery.PathConstraintRange;

/**
 *
 * @author Alex
 *
 */
public interface RangeHelper
{

    /**
     *
     * @param q field to constrain
     * @param node class to constrain
     * @param con constraint
     * @return range constraint
     */
    Constraint createConstraint(Queryable q, QueryNode node, PathConstraintRange con);
}
