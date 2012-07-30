package org.intermine.api.query;

import org.intermine.objectstore.query.Constraint;
import org.intermine.objectstore.query.QueryNode;
import org.intermine.pathquery.PathConstraintRange;

public interface RangeHelper {

	Constraint createConstraint(QueryNode node, PathConstraintRange con);
}
