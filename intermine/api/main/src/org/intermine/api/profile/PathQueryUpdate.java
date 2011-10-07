package org.intermine.api.profile;

/*
 * Copyright (C) 2002-2011 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.intermine.metadata.Model;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.pathquery.OrderElement;
import org.intermine.pathquery.OuterJoinStatus;
import org.intermine.pathquery.Path;
import org.intermine.pathquery.PathConstraint;
import org.intermine.pathquery.PathConstraintAttribute;
import org.intermine.pathquery.PathConstraintBag;
import org.intermine.pathquery.PathConstraintLookup;
import org.intermine.pathquery.PathConstraintLoop;
import org.intermine.pathquery.PathConstraintNull;
import org.intermine.pathquery.PathConstraintSubclass;
import org.intermine.pathquery.PathException;
import org.intermine.pathquery.PathQuery;

public class PathQueryUpdate {
    private PathQuery pathQuery;
    private PathQuery newPathQuery;
    private Model oldModel;

    public PathQueryUpdate(PathQuery pathQuery, Model newModel, Model oldModel) {
        this.pathQuery = pathQuery;
        this.oldModel = oldModel;
        this.newPathQuery = new PathQuery(newModel);
    }

    public synchronized List<String> updateWithRenamedClass(
            String prevClass, String newClass) throws PathException {
            // Update view paths
            updateView(prevClass, newClass, null, null);
            // Update constraints
            updateConstraints(prevClass, newClass, null, null);
            // Update outer join paths
            updateOuterJoins(prevClass, newClass, null, null);
            // Update description paths
            updateDescriptionsPath(prevClass, newClass, null, null);
            // Update order by paths
            updateOrderByPath(prevClass, newClass, null, null);

            List<String> problems = newPathQuery.verifyQuery();
            return problems;
        }

    public synchronized List<String> updateWithRenamedField(String cls, String prevField,
            String newField) throws PathException {
            // Update view paths
            updateView(cls, null, prevField, newField);
            // Update constraints
            updateConstraints(cls, null, prevField, newField);
            // Update outer join paths
            updateOuterJoins(cls, null, prevField, newField);
            // Update description paths
            updateDescriptionsPath(cls, null, prevField, newField);
            // Update order by paths
            updateOrderByPath(cls, null, prevField, newField);

            List<String> problems = newPathQuery.verifyQuery();
            return problems;
        }

    private void updateView (String cls, String newClass, String prevField, String newField)
        throws PathException {
        String viewPath = "";
        Path p;
        List<String> view = pathQuery.getView();
        List<String> newView = new ArrayList<String>();
        for (int index = 0; index < view.size(); index++) {
            viewPath = view.get(index);
            p = new Path(oldModel, viewPath);
            if ((newField == null && p.startContainsClass(cls))
                || (newField != null && p.elementsContainField(cls, prevField))) {
                if (newField == null) {
                    viewPath = viewPath.replace(cls, newClass);
                } else {
                    viewPath = viewPath.replace(prevField, newField);
                }
                newView.add(index, viewPath);
            }
        }
        newPathQuery.addViews(newView);
    }

    private void updateConstraints(String cls, String newClass, String prevField, String newField)
        throws PathException {
        String path, newPath;
        Path p;
        PathConstraint newPathConstraint;
        Map<PathConstraint, String> constraints = pathQuery.getConstraints();
        for (PathConstraint pathConstraint : constraints.keySet()) {
            path = pathConstraint.getPath();
            newPath = path;
            p = new Path(oldModel, path);
            if ((newField == null && p.startContainsClass(cls))
                || (newField != null && p.elementsContainField(cls, prevField))) {
                if (newField == null) {
                    newPath = path.replace(cls, newClass);
                } else {
                    newPath = path.replace(prevField, newField);
                }
            newPathConstraint = createPathConstraint(pathConstraint, path);
            newPathQuery.addConstraint(newPathConstraint);
        }
    }
}

    private PathConstraint createPathConstraint(PathConstraint pathConstraint, String newPath) {
        PathConstraint newPathConstraint = null;
        ConstraintOp op = pathConstraint.getOp();
        if (pathConstraint instanceof PathConstraintAttribute) {
            newPathConstraint = new PathConstraintAttribute(newPath, op,
                                ((PathConstraintAttribute) pathConstraint).getValue());
        } else if (pathConstraint instanceof PathConstraintBag) {
            newPathConstraint = new PathConstraintBag(newPath, op,
                                ((PathConstraintBag) pathConstraint).getBag());
        } else if (pathConstraint instanceof PathConstraintLookup) {
            newPathConstraint = new PathConstraintLookup(newPath,
                                ((PathConstraintLookup) pathConstraint).getValue(),
                                ((PathConstraintLookup) pathConstraint).getExtraValue());
        } else if (pathConstraint instanceof PathConstraintSubclass) {
            newPathConstraint = new PathConstraintSubclass(newPath,
                    ((PathConstraintSubclass) pathConstraint).getType());
        } else if (pathConstraint instanceof PathConstraintLoop) {
            newPathConstraint = new PathConstraintLoop(newPath, op,
                     ((PathConstraintLoop) pathConstraint).getLoopPath());
        } else if (pathConstraint instanceof PathConstraintNull) {
            newPathConstraint = new PathConstraintNull(newPath, op);
        }
        return newPathConstraint;
    }

    private void updateOuterJoins(String cls, String newClass, String prevField, String newField)
        throws PathException {
        Path p;
        Map<String, OuterJoinStatus> outerJoinStatus = pathQuery.getOuterJoinStatus();
        String newJoinPath;
        for (String joinPath : outerJoinStatus.keySet()) {
            p = new Path(oldModel, joinPath);
            newJoinPath = joinPath;
            if ((newField == null && p.startContainsClass(cls))
                || (newField != null && p.elementsContainField(cls, prevField))) {
                if (newField == null) {
                    newJoinPath = joinPath.replace(cls, newClass);
                } else {
                    newJoinPath = joinPath.replace(prevField, newField);
                }
                newPathQuery.setOuterJoinStatus(newJoinPath, outerJoinStatus.get(joinPath));
            }
        }
    }

    private void updateDescriptionsPath(String cls, String newClass, String prevField,
            String newField) throws PathException {
        Path p;
        Map<String, String> descriptions = pathQuery.getDescriptions();
        String newDescriptionPath;
        for (String descPath : descriptions.keySet()) {
            p = new Path(oldModel, descPath);
            newDescriptionPath = descPath;
            if ((newField == null && p.startContainsClass(cls))
                || (newField != null && p.elementsContainField(cls, prevField))) {
                if (newField == null) {
                    newDescriptionPath = descPath.replace(cls, newClass);
                } else {
                    newDescriptionPath = descPath.replace(prevField, newField);
                }
            }
            newPathQuery.setDescription(newDescriptionPath, descriptions.get(descPath));
         }
    }

    private void updateOrderByPath(String cls, String newClass, String prevField, String newField)
        throws PathException {
        String orderPath, newOrderPath;
        Path p;
        OrderElement orderElement;
        List<OrderElement> orderBy = pathQuery.getOrderBy();
        for (int index = 0; index < orderBy.size(); index++) {
            orderElement = (OrderElement) orderBy.get(index);
            orderPath = orderElement.getOrderPath();
            newOrderPath = orderPath;
            p = new Path(oldModel, orderPath);
            if ((newField == null && p.startContainsClass(cls))
                || (newField != null && p.elementsContainField(cls, prevField))) {
                if (newField == null) {
                    newOrderPath = orderPath.replace(cls, newClass);
                } else {
                    newOrderPath = orderPath.replace(prevField, newField);
                }
            }
            newPathQuery.addOrderBy(new OrderElement(newOrderPath, orderElement.getDirection()));
        }
    }
}
