package org.intermine.api.query;

import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.objectstore.query.Queryable;
import org.intermine.pathquery.PathConstraintRange;

public interface RangeHelper {

	Constraint createConstraint(Queryable q, QueryNode node, PathConstraintRange con);
}
